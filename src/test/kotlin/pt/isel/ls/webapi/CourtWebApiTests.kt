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
import pt.isel.ls.services.CourtService
import pt.isel.ls.services.UserService
import pt.isel.ls.webapi.dto.CourtDetailsOutput
import pt.isel.ls.webapi.dto.CourtOutput
import pt.isel.ls.webapi.dto.CourtsOutput
import pt.isel.ls.webapi.dto.RentalCreationInput
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val transactionManager = TransactionManagerInMem()

val courtApi =
    CourtWebApi(
        CourtService(transactionManager),
        UserService(transactionManager),
    )

val courtsRoutes =
    routes(
        "courts" bind POST to courtApi::createCourt,
        "courts/clubs/{cid}" bind GET to courtApi::getCourtsByClub,
        "courts/{crid}" bind GET to courtApi::getCourtInfo,
        "courts/users/{uid}" bind GET to courtApi::getCourtsUserRentals
    )

fun createCourt(
    token: String,
    clubId: Int,
): CourtDetailsOutput {
    val name = "Court-${randomString(10)}"
    val courtResponse =
        courtsRoutes(
            Request(POST, "courts")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body("""{"cid":$clubId,"name":"$name"}"""),
        )
    assertEquals(Status.CREATED, courtResponse.status)
    return Json.decodeFromString<CourtDetailsOutput>(courtResponse.bodyString())
}

class CourtWebApiTests {
    @BeforeTest
    fun setup() {
        transactionManager.run {
            it.courtRepo.clear()
            it.userRepo.clear()
        }
    }

    @Test
    fun `create 2 courts each one with a rental made by the same user`() {
        val token = createUser() // user criado supondo que é o primeiro
        val clubID = createClub(token).cid.toInt()

        val court1Response = createCourt(token, clubID)
        val court2response = createCourt(token, clubID)

        val rental1 =
            rentalsRoutes(
                Request(POST, "rentals")
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .body(
                        Json
                            .encodeToString(
                                RentalCreationInput(
                                    clubID,
                                    court1Response.crid.toInt(),
                                    LocalDate.parse("2025-05-08"),
                                    17,
                                    18,
                                ),
                            ),
                    ),
            )
        assertEquals(Status.CREATED, rental1.status)

        val rental2 =
            rentalsRoutes(
                Request(POST, "rentals")
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .body(
                        Json
                            .encodeToString(
                                RentalCreationInput(
                                    clubID,
                                    court2response.crid.toInt(),
                                    LocalDate.parse("2025-05-08"),
                                    20,
                                    21,
                                ),
                            ),
                    ),
            )

        assertEquals(Status.CREATED, rental2.status)

        val userId = 1
        val listOfCourtsThatHaveRentalByUser =
            courtsRoutes(
                Request(GET, "courts/users/$userId")
            )
        assertEquals(Status.OK, listOfCourtsThatHaveRentalByUser.status)
        val courts = Json.decodeFromString<CourtsOutput>(listOfCourtsThatHaveRentalByUser.bodyString())
        assertEquals(courts.courts.size, 2)
    }

    @Test
    fun `create court with valid data`() {
        val token = createUser()
        val clubID = createClub(token).cid.toInt()
        val court = createCourt(token, clubID)
        assertEquals(court.club.cid.toInt(), clubID)
    }

    @Test
    fun `get court info with valid court id`() {
        val token = createUser()
        val clubID = createClub(token).cid.toInt()
        val courtCreated = createCourt(token, clubID)
        val getCourtInfoRequest =
            courtsRoutes(
                Request(GET, "courts/${courtCreated.crid.toInt()}"),
            )
        assertEquals(Status.OK, getCourtInfoRequest.status)
        val court = Json.decodeFromString<CourtOutput>(getCourtInfoRequest.bodyString())
        assertEquals(court.crid, courtCreated.crid)
    }

    @Test
    fun `get court info with invalid court id`() {
        val token = createUser()
        val invalidCourtId = 999
        val getCourtInfoRequest =
            courtsRoutes(
                Request(GET, "courts/$invalidCourtId"),
            )
        assertEquals(Status.NOT_FOUND, getCourtInfoRequest.status)
    }

    @Test
    fun `get all courts for a club`() {
        val token = createUser()
        val clubID = createClub(token).cid.toInt()
        val getAllCourtsRequest =
            courtsRoutes(
                Request(GET, "courts/clubs/$clubID"),
            )
        val getAllCourtsResponse = Json.decodeFromString<CourtsOutput>(getAllCourtsRequest.bodyString())
        assertEquals(Status.OK, getAllCourtsRequest.status)
    }
}
