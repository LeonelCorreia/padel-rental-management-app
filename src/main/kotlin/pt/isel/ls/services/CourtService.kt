package pt.isel.ls.services

import pt.isel.ls.domain.Court
import pt.isel.ls.domain.Name
import pt.isel.ls.repository.TransactionManager
import pt.isel.ls.webapi.dto.PaginationInfo

class CourtService(
    private val trxManager: TransactionManager,
) {
    /**
     * Function that returns all courts in the system
     * @param cid the club identifier
     * @param limit the maximum number of courts to return
     * @param skip the number of courts to skip
     * @return list of courts
     */
    fun getCourts(
        cid: UInt,
        limit: Int,
        skip: Int,
    ): Result<List<Court>> =
        runCatching {
            trxManager.run {
                courtRepo.findByClubIdentifier(cid, limit, skip)
            }
        }

    fun numberOfCourts(cid: UInt): Result<PaginationInfo> =
        runCatching {
            trxManager.run {
                PaginationInfo(courtRepo.count(cid))
            }
        }

    fun numberOfUserCourtsThatHaveRentalsOfUser(uid: UInt): Result<PaginationInfo> =
        runCatching {
            trxManager.run {
                PaginationInfo(courtRepo.countByUser(uid))
            }
    }

    /**
     * Function that returns a court by its identifier
     * @param crid the court identifier
     * @return either the court or an error indicating that the court was not found
     */
    fun getCourtById(crid: UInt): Result<Court> =
        runCatching {
            trxManager.run {
                courtRepo.findByIdentifier(crid)
                    ?: throw CourtError.CourtNotFound(crid)
            }
        }

    /**
     * Function that creates a new court in the system
     * @param name the court name
     * @param clubId the club identifier
     * @return the new court
     */
    fun createCourt(
        name: Name,
        clubId: UInt,
    ): Result<Court> =
        runCatching {
            trxManager.run {
                courtRepo.createCourt(name, clubId)
            }
        }

    fun getCourtsUserRentals(
        uid: UInt,
        limit: Int,
        skip: Int,
    ): Result<List<Court>> =
        runCatching {
            trxManager.run {
                courtRepo.findAllCourtsThatHaveRentalsByRenterId(uid, limit, skip)
            }
        }
}
