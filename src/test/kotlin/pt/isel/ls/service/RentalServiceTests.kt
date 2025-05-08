@file:Suppress("ktlint:standard:no-wildcard-imports")

package pt.isel.ls.service

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.isel.ls.domain.*
import pt.isel.ls.repository.mem.TransactionManagerInMem
import pt.isel.ls.services.*
import pt.isel.ls.webapi.createRental
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class RentalServiceTests {
    private val transactionManager = TransactionManagerInMem()

    private val rentalService = RentalService(transactionManager)
    private val clubService = ClubService(transactionManager)
    private val userService = UserService(transactionManager)
    private val courtService = CourtService(transactionManager)

    @BeforeTest
    fun setUp() {
        transactionManager.run {
            it.rentalRepo.clear()
            it.courtRepo.clear()
            it.clubRepo.clear()
            it.userRepo.clear()
        }
    }

    @Test
    fun `create rental with valid renter and court`() {
        val renterResult = userService.createUser(Name("John Doe"), Email("john@example.com"))
        assertTrue(renterResult.isSuccess)
        val renter = renterResult.getOrNull()!!
        val club = clubService.createClub("Sports Club".toName(), renter)
        assertTrue(club.isSuccess)
        val courtResult = courtService.createCourt(Name("Court A"), club.getOrNull()!!.cid)
        assertTrue(courtResult.isSuccess)
        val court = courtResult.getOrNull()!!
        val rentalDate =
            Clock.System
                .now()
                .plus(1.days)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        val rentTime = TimeSlot(10u, 13u)

        val rentalResult = rentalService.createRental(rentalDate, rentTime, renter.uid, court.crid)
        assertTrue(rentalResult.isSuccess)
        val rental = rentalResult.getOrNull()!!

        assertEquals(rentalDate, rental.date)
        assertEquals(rentTime, rental.rentTime)
        assertEquals(renter, rental.renter)
        assertEquals(court, rental.court)
    }

    @Test
    fun `count rentals Of user in certain court`() {
        val user = userService.createUser("Name".toName(), "email@email.com".toEmail()).getOrNull()
        val rental = createRental(user!!.token.toString())
        val a = rentalService.getUsersOfRentalsOnCourt(rental.court.crid)
        assert(a.isSuccess)
        assertEquals(a.getOrThrow().size, 1)
    }
}
