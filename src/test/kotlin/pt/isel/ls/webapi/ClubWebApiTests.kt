package pt.isel.ls.webapi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import pt.isel.ls.repository.mem.TransactionManagerInMem
import pt.isel.ls.services.ClubService
import pt.isel.ls.services.RentalService
import pt.isel.ls.services.UserService
import pt.isel.ls.webapi.dto.AvailableHoursInput
import pt.isel.ls.webapi.dto.ClubCreationInput
import pt.isel.ls.webapi.dto.ClubDetailsOutput
import pt.isel.ls.webapi.dto.ClubsOutput
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val transactionManager = TransactionManagerInMem()

val clubApi =
    ClubWebApi(
        ClubService(transactionManager),
        UserService(transactionManager),
        RentalService(transactionManager),
    )

val clubsRoutes =
    routes(
        "clubs" bind POST to clubApi::createClub,
        "clubs" bind GET to clubApi::getAllClubs,
        "clubs/{cid}" bind GET to clubApi::getClubInfo,
        "courts/{crid}/available" bind POST to clubApi::getAvailableHours,
    )

fun createClub(token: String): ClubDetailsOutput {
    val name = "Club-${randomString(10)}"
    val clubResponse =
        clubsRoutes(
            Request(POST, "clubs")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body(Json.encodeToString<ClubCreationInput>(ClubCreationInput(name))),
        )
    return Json.decodeFromString<ClubDetailsOutput>(clubResponse.bodyString())
}

class ClubWebApiTests {
    @BeforeTest
    fun setup() {
        transactionManager.run {
            it.clubRepo.clear()
            it.userRepo.clear()
        }
    }

    @Test
    fun `club creation with valid Name and User`() {
        val token = createUser()
        val clubResponse =
            clubsRoutes(
                Request(POST, "clubs")
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .body(Json.encodeToString<ClubCreationInput>(ClubCreationInput("Benfica"))),
            )
        assertEquals(Status.CREATED, clubResponse.status)
        val club = Json.decodeFromString<ClubDetailsOutput>(clubResponse.bodyString())
        assertEquals("Benfica", club.name)
    }

    @Test
    fun `get club info with valid club id`() {
        val token = createUser()
        val club = createClub(token)

        val clubResponse =
            clubsRoutes(
                Request(GET, "clubs/${club.cid}"),
            )
        val clubRequest = Json.decodeFromString<ClubDetailsOutput>(clubResponse.bodyString())
        assertEquals(club.name, clubRequest.name)
        assertEquals(club.cid, clubRequest.cid)
        assertEquals(Status.OK, clubResponse.status)
    }

    @Test
    fun `get club info with invalid club id`() {
        val invalidClubId = 999
        createUser()
        val getClubInfoRequest =
            clubsRoutes(Request(GET, "clubs/$invalidClubId"))
        assertEquals(Status.NOT_FOUND, getClubInfoRequest.status)
    }

    @Test
    fun `get all clubs with valid token`() {
        val token = createUser()
        createClub(token)

        val getAllClubsRequest =
            clubsRoutes(
                Request(GET, "clubs"),
            )
        val body = Json.decodeFromString<ClubsOutput>(getAllClubsRequest.bodyString())
        assertTrue(body.clubs.isNotEmpty())
    }

    @Test
    fun `get available hours with invalid court`() {
        val courtId = 999
        val date = LocalDate.parse("2025-06-01")
        val token = createUser()
        val getAvailableHoursRequest =
            clubsRoutes(
                Request(POST, "courts/$courtId/available")
                    .header("Authorization", token)
                    .body(Json.encodeToString<AvailableHoursInput>(AvailableHoursInput(date))),
            )
        assertEquals(Status.NOT_FOUND, getAvailableHoursRequest.status)
    }

    @Test
    fun `get available hours`() {
        val token = createUser()
        val clubId = createClub(token).cid
        val courtId = createCourt(token, clubId.toInt())
        val date = LocalDate.parse("2025-06-01")

        val getAvailableHoursRequest =
            clubsRoutes(
                Request(POST, "courts/${courtId.crid}/available")
                    .header("Authorization", token)
                    .body(Json.encodeToString<AvailableHoursInput>(AvailableHoursInput(date))),
            )
        assertEquals(Status.OK, getAvailableHoursRequest.status)
        val availableHours =
            Json.decodeFromString<List<UInt>>(getAvailableHoursRequest.bodyString())
        assert(availableHours.isNotEmpty())
        assert(availableHours.size == 24)
    }

    @Test
    fun `trying to see available hours for a court in the past should fail`() {
        val token = createUser()
        val clubID = createClub(token).cid.toInt()
        val courtId = createCourt(token, clubID)
        val date = LocalDate.parse("2024-06-01")

        val getAvailableHoursRequest =
            clubsRoutes(
                Request(POST, "courts/${courtId.crid}/available")
                    .header("Authorization", token)
                    .body(Json.encodeToString(date)),
            )
        assertEquals(Status.BAD_REQUEST, getAvailableHoursRequest.status)
    }
    
}
