@file:Suppress("kt lint:standard:no-wildcard-imports")

package pt.isel.ls.repository.mem

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.isel.ls.domain.Rental
import pt.isel.ls.domain.TimeSlot
import pt.isel.ls.domain.User
import pt.isel.ls.repository.RentalRepository
import pt.isel.ls.repository.mem.CourtRepositoryInMem.courts
import pt.isel.ls.repository.mem.UserRepositoryInMem.users
import pt.isel.ls.services.RentalError
import pt.isel.ls.services.ensureOrThrow
import pt.isel.ls.services.getOrThrow

object RentalRepositoryInMem : RentalRepository {
    private val rentals = mutableListOf<Rental>()

    private var currId = 0u

    /**
     * Creates a rental.
     * @param date The date of the rental.
     * @param rentTime The duration of the rental.
     * @param renterId The id of the renter.
     * @param courtId The id of the court.
     * @return The created rental.
     * @throws IllegalArgumentException If the rental date is in the past.
     * @throws IllegalArgumentException If the renter or court does not exist.
     */
    override fun createRental(
        date: LocalDate,
        rentTime: TimeSlot,
        renterId: UInt,
        courtId: UInt,
    ): Rental {
        ensureOrThrow(
            condition =
                date >=
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date,
            exception = RentalError.RentalDateInThePast(date),
        )

        ensureOrThrow(
            condition =
                rentals.none { rental ->
                    rental.date == date &&
                        !(rentTime.end <= rental.rentTime.start || rentTime.start >= rental.rentTime.end)
                },
            exception = RentalError.OverlapInTimeSlot(date, rentTime),
        )

        currId += 1u

        val renter =
            getOrThrow(RentalError.RenterNotFound(renterId)) {
                users.firstOrNull { it.uid == renterId }
            }

        val court =
            getOrThrow(RentalError.MissingCourt(courtId)) {
                courts.firstOrNull { it.crid == courtId }
            }

        val rental =
            Rental(
                rid = currId,
                date = date,
                rentTime = rentTime,
                renter = renter,
                court = court,
            )

        rentals.add(rental)
        return rental
    }

    /**
     * Finds the available hours for a court on a given date.
     * @param crid The id of the court.
     * @param date The date to check.
     * @return The list available hours for the court on the given date.
     */
    override fun findAvailableHoursForACourt(
        crid: UInt,
        date: LocalDate,
    ): List<UInt> {
        val court =
            getOrThrow(RentalError.MissingCourt(crid)) {
                courts.firstOrNull { it.crid == crid }
            }

        val rentalsOnDate = rentals.filter { it.court == court && it.date == date }

        val hoursRange = UIntRange(0u, 23u)

        return hoursRange
            .filter { hour ->
                rentalsOnDate.none { rental ->
                    hour in rental.rentTime.start..<rental.rentTime.end
                }
            }
    }

    /**
     * Finds all rentals for a court on a given date.
     * @param crid The id of the court.
     * @param date The date to check.
     * @return The list of rentals for the court on the given date.
     */
    override fun findByCridAndDate(
        crid: UInt,
        date: LocalDate?,
        limit: Int,
        offset: Int,
    ): List<Rental> {
        getOrThrow(RentalError.MissingCourt(crid)) {
            courts.firstOrNull { it.crid == crid }
        }

        return rentals
            .filter {
                it.court.crid == crid && (date == null || it.date == date)
            }.drop(offset)
            .take(limit)
    }

    override fun numRentalsOfCourt(
        crid: UInt,
        date: LocalDate?,
    ): Int = rentals.count { it.court.crid == crid && (date == null || it.date == date) }

    /**
     * Finds all rentals by a renter id.
     * @param renter The id of the renter.
     * @return The list of rentals by the renter.
     */
    override fun findAllRentalsByRenterId(
        renter: UInt,
        limit: Int,
        offset: Int,
    ): List<Rental> {
        getOrThrow(RentalError.RenterNotFound(renter)) {
            users.firstOrNull { it.uid == renter }
        }

        return rentals
            .filter {
                it.renter.uid == renter
            }.drop(offset)
            .take(limit)
    }

    override fun numRentalsOfUser(renter: UInt): Int = rentals.count { it.renter.uid == renter }

    override fun updateDateAndRentTime(
        rid: UInt,
        date: LocalDate,
        rentTime: TimeSlot,
    ): Rental {
        val rental = rentals.firstOrNull { it.rid == rid }
        requireNotNull(rental)
        rentals.removeIf { it.rid == rid }
        val newRental =
            Rental(
                rid = rid,
                date = date,
                rentTime = rentTime,
                renter = rental.renter,
                court = rental.court,
            )
        rentals.add(newRental)
        return newRental
    }

    override fun getNumOfRentalsOfUsersOnCourt(crid: UInt): Map<User, UInt> {
        TODO("Not yet implemented")
    }

    /**
     * Updates an existing rental or creates a new one if it's new.
     * @param element The rental to save.
     */
    override fun save(element: Rental) {
        rentals.removeIf { it.rid == element.rid }
        currId += 1u
        rentals.add(
            Rental(
                rid = currId,
                date = element.date,
                rentTime = element.rentTime,
                renter = element.renter,
                court = element.court,
            ),
        )
    }

    /**
     * Finds a rental by its identifier.
     * @param id The identifier of the rental.
     * @return The rental with the given identifier or null if it does not exist.
     */
    override fun findByIdentifier(id: UInt): Rental? =
        rentals.firstOrNull {
            it.rid == id
        }

    /**
     * Finds all rentals.
     * @return The list of all rentals.
     */
    override fun findAll(
        limit: Int,
        offset: Int,
    ): List<Rental> = rentals.drop(offset).take(limit)

    /**
     * Deletes a rental by its identifier.
     * @param id The identifier of the rental.
     */
    override fun deleteByIdentifier(id: UInt) {
        rentals.removeIf { it.rid == id }
    }

    /**
     * Clears all rentals.
     */
    override fun clear() {
        rentals.clear()
        currId = 0u
    }
}
