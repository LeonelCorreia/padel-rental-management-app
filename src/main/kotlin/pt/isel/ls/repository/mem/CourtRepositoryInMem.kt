package pt.isel.ls.repository.mem

import pt.isel.ls.domain.Court
import pt.isel.ls.domain.Name
import pt.isel.ls.repository.CourtRepository
import pt.isel.ls.repository.mem.ClubRepositoryInMem.clubs
import pt.isel.ls.repository.mem.RentalRepositoryInMem.rentals
import pt.isel.ls.repository.mem.UserRepositoryInMem.users
import pt.isel.ls.services.CourtError
import pt.isel.ls.services.RentalError
import pt.isel.ls.services.getOrThrow

object CourtRepositoryInMem : CourtRepository {
    val courts = mutableListOf<Court>()

    private var currId = 0u

    override fun createCourt(
        name: Name,
        clubId: UInt,
    ): Court {
        val club =
            getOrThrow(CourtError.MissingClub(clubId)) {
                clubs.firstOrNull { it.cid == clubId }
            }

        currId += 1u

        val court =
            Court(
                crid = currId,
                name = name,
                club = club,
            )

        courts.add(court)
        return court
    }

    override fun findByClubIdentifier(
        cid: UInt,
        limit: Int,
        offset: Int,
    ): List<Court> = courts.filter { it.club.cid == cid }.drop(offset).take(limit)

    override fun findAllCourtsThatHaveRentalsByRenterId(
        renter: UInt,
        limit: Int,
        offset: Int
    ): List<Court> {
        getOrThrow(RentalError.RenterNotFound(renter)) {
            users.firstOrNull { it.uid == renter }
        }

        return rentals
            .filter { it.renter.uid == renter }
            .map { it.court }
            .drop(offset)
            .take(limit)
    }


    override fun save(element: Court) {
        courts.removeIf { it.crid == element.crid }
        currId += 1u
        courts.add(
            Court(
                crid = currId,
                name = element.name,
                club = element.club,
            ),
        )
    }

    override fun findByIdentifier(id: UInt): Court? = courts.firstOrNull { it.crid == id }

    override fun findAll(
        limit: Int,
        offset: Int,
    ): List<Court> = courts.drop(offset).take(limit)

    override fun deleteByIdentifier(id: UInt) {
        courts.removeIf { it.crid == id }
    }

    override fun clear() {
        courts.clear()
        currId = 0u
    }

    override fun count(cid: UInt): Int = courts.count { it.club.cid == cid }
}
