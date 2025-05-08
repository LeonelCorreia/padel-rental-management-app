package pt.isel.ls.services

import kotlinx.datetime.LocalDate
import pt.isel.ls.domain.Rental
import pt.isel.ls.domain.TimeSlot
import pt.isel.ls.domain.User
import pt.isel.ls.repository.TransactionManager
import pt.isel.ls.webapi.dto.PaginationInfo

class RentalService(
    private val trxManager: TransactionManager,
) {
    /**
     * Function that returns all rentals in of a court in a specific date
     * @param crid the court identifier
     * @param date the date
     * @param limit the maximum number of rentals to return
     * @param skip the number of rentals to skip
     * @return list of rentals
     */
    fun getRentals(
        crid: UInt,
        date: LocalDate?,
        limit: Int,
        skip: Int,
    ): Result<List<Rental>> =
        runCatching {
            trxManager.run {
                rentalRepo.findByCridAndDate(crid, date, limit, skip)
            }
        }

    fun numberOfRentals(
        crid: UInt,
        date: LocalDate?,
    ): Result<PaginationInfo> =
        runCatching {
            trxManager.run {
                PaginationInfo(rentalRepo.numRentalsOfCourt(crid, date))
            }
        }

    /**
     * Function that returns a rental by its identifier
     * @param rid the rental identifier
     * @return result with the rental or an error indicating that the rental was not found
     */
    fun getRentalById(rid: UInt): Result<Rental> =
        runCatching {
            trxManager.run {
                rentalRepo.findByIdentifier(rid)
                    ?: throw RentalError.RentalNotFound(rid)
            }
        }

    /**
     * Function that creates a new rental in the system
     * @param date the rental date
     * @param rentTime the rental time slot
     * @param renterId the renter identifier
     * @param courtId the court identifier
     * @return the new rental
     */
    fun createRental(
        date: LocalDate,
        rentTime: TimeSlot,
        renterId: UInt,
        courtId: UInt,
    ): Result<Rental> =
        runCatching {
            trxManager.run {
                rentalRepo.createRental(date, rentTime, renterId, courtId)
            }
        }

    /**
     * Get the available hours for a court in a specific date
     * @param crid the court identifier
     * @param date the date
     * @return the list of available hours
     */
    fun getAvailableHours(
        crid: UInt,
        date: LocalDate,
    ): Result<List<UInt>> =
        runCatching {
            trxManager.run {
                rentalRepo.findAvailableHoursForACourt(crid, date)
            }
        }

    /**
     * Function that returns all rentals that the user made
     * @param uid the user identifier
     * @param limit the maximum number of rentals to return
     * @param skip the number of rentals to skip
     * @return the list of rentals that the user made
     */
    fun getUserRentals(
        uid: UInt,
        limit: Int,
        skip: Int,
    ): Result<List<Rental>> =
        runCatching {
            trxManager.run {
                rentalRepo.findAllRentalsByRenterId(uid, limit, skip)
            }
        }

    fun numberOfUserRentals(uid: UInt): Result<PaginationInfo> =
        runCatching {
            trxManager.run {
                PaginationInfo(rentalRepo.numRentalsOfUser(uid))
            }
        }

    fun deleteRental(rid: UInt): Result<Unit> =
        runCatching {
            trxManager.run {
                rentalRepo.deleteByIdentifier(rid)
            }
        }

    fun updateDateAndRentTime(
        rid: UInt,
        date: LocalDate,
        rentTime: TimeSlot,
    ): Result<Rental> =
        runCatching {
            trxManager.run {
                rentalRepo.updateDateAndRentTime(
                    rid,
                    date,
                    rentTime,
                )
            }
        }

    fun numberOfUsersWithRentalsOnDate(date: LocalDate): Result<PaginationInfo> =
        runCatching {
            trxManager.run {
                PaginationInfo(rentalRepo.numberOfUsersWithRentalsOnDate(date))
            }
        }

    fun getUsersThatRentedOnDate(
        date: LocalDate,
        limit: Int,
        skip: Int,
    ): Result<Map<User, Int>> =
        runCatching {
            trxManager.run {
                rentalRepo.getUsersThatRentedOnDate(
                    date,
                    limit,
                    skip,
                )
            }
        }
}
