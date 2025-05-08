package pt.isel.ls.repository

import pt.isel.ls.domain.Court
import pt.isel.ls.domain.Name

/**
 * Generic Interface for a Court repository that supports CRUD operations.
 */
interface CourtRepository : Repository<Court> {
    /**
     * Function that creates a new Court.
     * crid is automatically incremented so it's not received as a parameter to the function.
     */
    fun createCourt(
        name: Name,
        clubId: UInt,
    ): Court

    /**
     * Function that returns all courts in the system.
     * @param cid the club identifier
     * @param limit the maximum number of courts to return
     * @param offset the number of courts to skip
     * @return list of courts
     */
    fun findByClubIdentifier(
        cid: UInt,
        limit: Int = 30,
        offset: Int = 0,
    ): List<Court>

    fun findAllCourtsThatHaveRentalsByRenterId(
        renter: UInt,
        limit: Int = 30,
        offset: Int = 0,
    ): List<Court>

    /**
     * Function that returns the number of courts in the system.
     * @param cid the club identifier
     * @return the number of courts in the system
     */
    fun count(cid: UInt): Int

    fun countByUser(uid: UInt): Int
}
