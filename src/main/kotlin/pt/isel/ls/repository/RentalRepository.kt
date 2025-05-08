package pt.isel.ls.repository

import kotlinx.datetime.LocalDate
import pt.isel.ls.domain.Rental
import pt.isel.ls.domain.TimeSlot
import pt.isel.ls.domain.User

/**
 * Generic Interface for a Rental repository that supports CRUD operations.
 */
interface RentalRepository : Repository<Rental> {
    /**
     * Function that creates a new Rental.
     * rid is automatically incremented so it's not received as a parameter to the function.
     */
    fun createRental(
        date: LocalDate,
        rentTime: TimeSlot,
        renterId: UInt,
        courtId: UInt,
    ): Rental

    /**
     * Finds all the available hours for a court in a given date.
     * @param crid The court id.
     * @param date The date to search for available hours.
     */
    fun findAvailableHoursForACourt(
        crid: UInt,
        date: LocalDate,
    ): List<UInt>

    /**
     * Finds all the rentals for a court in a given date.
     * @param crid The court id.
     * @param date The date to search for rentals if no date is received search for all the rentals of the court.
     */
    fun findByCridAndDate(
        crid: UInt,
        date: LocalDate?,
        limit: Int = 30,
        offset: Int = 0,
    ): List<Rental>

    /**
     * Function that returns the number of rentals in the system.
     * @param crid the court identifier
     * @return the number of rentals in the system
     */
    fun numRentalsOfCourt(
        crid: UInt,
        date: LocalDate?,
    ): Int

    /**
     * Finds all the rentals of a renter.
     * @param renter The renter id.
     */
    fun findAllRentalsByRenterId(
        renter: UInt,
        limit: Int = 30,
        offset: Int = 0,
    ): List<Rental>

    /**
     * Function that returns the number of rentals in the system.
     * @param renter the renter identifier
     * @return the number of rentals in the system
     */
    fun numRentalsOfUser(renter: UInt): Int

    fun updateDateAndRentTime(
        rid: UInt,
        date: LocalDate,
        rentTime: TimeSlot,
    ): Rental

    fun numberOfUsersWithRentalsOnDate(date: LocalDate): Int

    fun getUsersThatRentedOnDate(
        date: LocalDate,
        limit: Int,
        offset: Int,
    ): Map<User, Int>
}
