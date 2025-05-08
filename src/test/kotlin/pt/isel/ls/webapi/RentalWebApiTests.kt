@file:Suppress("ktlint:standard:no-wildcard-imports")

package pt.isel.ls.webapi

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import pt.isel.ls.domain.TimeSlot
import pt.isel.ls.repository.mem.TransactionManagerInMem
import pt.isel.ls.services.RentalService
import pt.isel.ls.services.UserService
import pt.isel.ls.webapi.dto.*
import kotlin.test.*

private val transactionManager = TransactionManagerInMem()

val rentalApi =
    RentalWebApi(
        RentalService(transactionManager),
        UserService(transactionManager),
    )
val rentalsRoutes =
    routes(
        "rentals" bind POST to rentalApi::createRental,
        "rentals/clubs/courts/{crid}" bind GET to rentalApi::getRentalsOnCourt,
        "rentals/{rid}" bind GET to rentalApi::getRentalInfo,
        "users/{uid}/rentals" bind GET to rentalApi::getUserRentals,
        "rentals/{rid}" bind DELETE to rentalApi::deleteRental,
        "rentals/{rid}" bind PUT to rentalApi::updateRental,
        "rentals/{date}" bind GET to rentalApi::getUsersThatRentedOnDate,
    )

fun createRental(token: String): RentalDetailsOutput {
    val number = (1..30).random()
    val day = if (number < 10) "0$number" else number.toString()
    val clubName = "Club-${randomString(10)}"
    val clubResponse =
        clubsRoutes(
            Request(POST, "clubs")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body(Json.encodeToString<ClubCreationInput>(ClubCreationInput(clubName))),
        )
    assertEquals(Status.CREATED, clubResponse.status)
    val club = Json.decodeFromString<ClubDetailsOutput>(clubResponse.bodyString())
    val name = "Court-${randomString(10)}"
    val courtResponse =
        courtsRoutes(
            Request(POST, "courts")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body("""{"cid":${club.cid.toInt()},"name":"$name"}"""),
        )
    assertEquals(Status.CREATED, courtResponse.status)
    val court = Json.decodeFromString<CourtDetailsOutput>(courtResponse.bodyString())

    val rental =
        rentalsRoutes(
            Request(POST, "rentals")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body(
                    Json
                        .encodeToString(
                            RentalCreationInput(
                                club.cid.toInt(),
                                court.crid.toInt(),
                                LocalDate.parse("2025-06-$day"),
                                10,
                                12,
                            ),
                        ),
                ),
        )
    assertEquals(Status.CREATED, rental.status)
    return Json.decodeFromString<RentalDetailsOutput>(rental.bodyString())
}

fun createRentalOnDate(token: String, date: LocalDate, time: TimeSlot): RentalDetailsOutput {
    val clubName = "Club-${randomString(10)}"
    val clubResponse =
        clubsRoutes(
            Request(POST, "clubs")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body(Json.encodeToString<ClubCreationInput>(ClubCreationInput(clubName))),
        )
    assertEquals(Status.CREATED, clubResponse.status)
    val club = Json.decodeFromString<ClubDetailsOutput>(clubResponse.bodyString())
    val name = "Court-${randomString(10)}"
    val courtResponse =
        courtsRoutes(
            Request(POST, "courts")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body("""{"cid":${club.cid.toInt()},"name":"$name"}"""),
        )
    assertEquals(Status.CREATED, courtResponse.status)
    val court = Json.decodeFromString<CourtDetailsOutput>(courtResponse.bodyString())

    val rental =
        rentalsRoutes(
            Request(POST, "rentals")
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .body(
                    Json
                        .encodeToString(
                            RentalCreationInput(
                                club.cid.toInt(),
                                court.crid.toInt(),
                                date,
                                time.start.toInt(),
                                time.end.toInt(),
                            ),
                        ),
                ),
        )
    assertEquals(Status.CREATED, rental.status)
    return Json.decodeFromString<RentalDetailsOutput>(rental.bodyString())
}

class RentalWebApiTests {
    @BeforeTest
    fun setup() {
        transactionManager.run {
            it.rentalRepo.clear()
            it.courtRepo.clear()
            it.clubRepo.clear()
            it.userRepo.clear()
        }
    }

    @Test
    fun `create rental with valid data`() {
        val token = createUser()
        val club = createClub(token)
        val court = createCourt(token, club.cid.toInt())
        val rental =
            rentalsRoutes(
                Request(POST, "rentals")
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .body(
                        Json.encodeToString<RentalCreationInput>(
                            RentalCreationInput(
                                date = LocalDate.parse("2025-06-15"),
                                initialHour = 10,
                                finalHour = 12,
                                cid = club.cid.toInt(),
                                crid = court.crid.toInt(),
                            ),
                        ),
                    ),
            )
        assertEquals(Status.CREATED, rental.status)
        val rentalDetails = Json.decodeFromString<RentalDetailsOutput>(rental.bodyString())
        assertEquals(rentalDetails.date, LocalDate.parse("2025-06-15"))
        assertEquals(rentalDetails.initialHour, 10)
        assertEquals(rentalDetails.finalHour, 12)
    }

    @Test
    fun `get rental info with valid rental id`() {
        val token = createUser()
        createRental(token)
        val getRentalInfoRequest =
            rentalsRoutes(
                Request(GET, "rentals/1"),
            )
        assertEquals(Status.OK, getRentalInfoRequest.status)
    }

    @Test
    fun `get rental info with invalid rental id`() {
        createUser()
        val invalidRentalId = 999
        val getRentalInfoRequest =
            rentalsRoutes(
                Request(GET, "rentals/$invalidRentalId"),
            )
        assertEquals(Status.NOT_FOUND, getRentalInfoRequest.status)
    }

    @Test
    fun `get all rentals for a court`() {
        val token = createUser()
        val rental = createRental(token)
        val getAllRentalsRequest =
            rentalsRoutes(
                Request(GET, "rentals/clubs/courts/1"),
            )
        val rentals = Json.decodeFromString<RentalsOutput>(getAllRentalsRequest.bodyString())
        assertTrue(rentals.rentals.any { it.date == rental.date })
        assertEquals(Status.OK, getAllRentalsRequest.status)
    }

    @Test
    fun `get user rentals with valid token`() {
        val name = randomString(10)
        val userResponse =
            userRoutes(
                Request(POST, "users")
                    .header("Content-Type", "application/json")
                    .body("""{"name":"Ric", "email":"$name@gmail.com"}"""),
            )
        assertEquals(Status.CREATED, userResponse.status)

        val getUserRentalsRequest =
            rentalsRoutes(
                Request(GET, "users/1/rentals"),
            )
        val userRentals = Json.decodeFromString<RentalsOutput>(getUserRentalsRequest.bodyString())
        assertTrue(userRentals.rentals.isEmpty())
        assertEquals(Status.OK, getUserRentalsRequest.status)
    }

    @Test
    fun `update the date and rentTime of a rental`() {
        val token = createUser()
        val rental = createRental(token)

        val updateRequest =
            rentalsRoutes(
                Request(PUT, "rentals/1")
                    .header("Authorization", token)
                    .body(
                        Json.encodeToString<RentalUpdateInput>(
                            RentalUpdateInput(
                                date = LocalDate.parse("2026-06-15"),
                                initialHour = 16,
                                finalHour = 17,
                            ),
                        ),
                    ),
            )
        assertEquals(Status.OK, updateRequest.status)

        val updatedRental = Json.decodeFromString<RentalDetailsOutput>(updateRequest.bodyString())

        assertNotEquals(rental.date, updatedRental.date)
        assertNotEquals(rental.initialHour, updatedRental.initialHour)
        assertNotEquals(rental.finalHour, updatedRental.finalHour)
    }

    @Test
    fun `delete a rental`() {
        val token = createUser()
        val rental = createRental(token)
        assertNotNull(rental)

        val deleteRequest =
            rentalsRoutes(
                Request(DELETE, "rentals/1")
                    .header("Authorization", token),
            )
        assertEquals(Status.OK, deleteRequest.status)

        val getRentalInfoRequest =
            rentalsRoutes(
                Request(GET, "rentals/1")
                    .header("Authorization", token),
            )
        assertEquals(Status.NOT_FOUND, getRentalInfoRequest.status)
    }

    @Test
    fun `get users that rented on date`() {
        // create user randomizes email so diff users will be created
        val token1 = createUser()
        val token2 = createUser()

        val dateToCheck = LocalDate.parse("2026-06-15")

        val rentalByToken1OnDate1 = createRentalOnDate(token1, dateToCheck, TimeSlot(8u, 9u))
        val rentalByToken1OnDate2 = createRentalOnDate(token1, dateToCheck, TimeSlot(10u, 12u))
        val rentalByToken1OnDiffDate = createRentalOnDate(token1, LocalDate.parse("2027-09-14"), TimeSlot(10u, 12u))

        val rentalByToken2OnDate = createRentalOnDate(token2, dateToCheck, TimeSlot(15u, 17u))

        val usersThatRentedOnDate =
            rentalsRoutes(
                Request(GET, "rentals/2026-06-15")
            )
        assertEquals(Status.OK, usersThatRentedOnDate.status)

        val usersThatRented = Json.decodeFromString<UsersRentalTimesOnDateOutput>(usersThatRentedOnDate.bodyString())

        assertEquals(usersThatRented.usersRentalTimes.size, 2)
        assertTrue(usersThatRented.usersRentalTimes.first().times in 1..2 )
    }
}
