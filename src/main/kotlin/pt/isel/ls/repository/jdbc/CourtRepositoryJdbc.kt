@file:Suppress("ktlint:standard:no-wildcard-imports")

package pt.isel.ls.repository.jdbc

import pt.isel.ls.domain.*
import pt.isel.ls.repository.CourtRepository
import pt.isel.ls.services.CourtError
import pt.isel.ls.services.ensureOrThrow
import java.rmi.server.UID
import java.sql.Connection
import java.sql.ResultSet

/**
 * Repository in jdbc responsible for direct interactions with the database for courts related actions
 * @param connection The database connection for the SQL queries
 */
class CourtRepositoryJdbc(
    private val connection: Connection,
) : CourtRepository {
    /**
     * Function responsible for the creation of a court.
     * @param name Name of the new court
     * @param clubId Identifier of the club that owns the court
     * @return The Court created
     * @throws IllegalArgumentException if clubId doesn't exist
     */
    override fun createCourt(
        name: Name,
        clubId: UInt,
    ): Court =
        connection.executeMultipleQueries {
            val sqlCheckFk =
                """
                ${clubSqlReturnFormat()}
                WHERE c.cid = ?
                """.trimIndent()

            val club =
                connection.prepareStatement(sqlCheckFk).use { stmt ->
                    stmt.setInt(1, clubId.toInt())
                    stmt.executeQuery().use { rs ->
                        ensureOrThrow(
                            condition = rs.next(),
                            exception = CourtError.MissingClub(clubId),
                        )
                        rs.mapClub()
                    }
                }

            val sqlInsert =
                """
                INSERT INTO courts (name, club_id)
                VALUES (?, ?)
                RETURNING crid as court_id, name as court_name, club_id
                """.trimIndent()

            val newCourt =
                connection.prepareStatement(sqlInsert).use { stmt ->
                    stmt.setString(1, name.value)
                    stmt.setInt(2, clubId.toInt())

                    stmt.executeQuery().use { rs ->
                        ensureOrThrow(
                            condition = rs.next(),
                            exception = RuntimeException("Error inserting court"),
                        )
                        rs.mapCourt(club)
                    }
                }
            newCourt
        }

    /**
     * Function that finds all courts of a club.
     * @param cid The identifier of the club to search for the courts
     * @param limit Number of tuples to retrieve, default of 30
     * @param offset Number of tuples to skip at the beginning, default of 0
     * @return The list of courts owned by the club
     */
    override fun findByClubIdentifier(
        cid: UInt,
        limit: Int,
        offset: Int,
    ): List<Court> {
        val sqlSelect =
            """
            ${courtSqlReturnFormat()}
            WHERE cr.club_id = ?
            ORDER BY cr.crid DESC
            LIMIT ? OFFSET ?
            """.trimIndent()

        return connection.prepareStatement(sqlSelect).use { stmt ->
            stmt.setInt(1, cid.toInt())
            stmt.setInt(2, limit)
            stmt.setInt(3, offset)

            stmt.executeQuery().use { rs ->
                val courts = mutableListOf<Court>()
                while (rs.next()) {
                    courts.add(rs.mapCourt())
                }
                courts
            }
        }
    }

    /**
     * Function that creates a new court or updates, with the information given, if one with the crid already exists.
     * @param element court to be created or updated
     */
    override fun save(element: Court) {
        val sqlSave =
            """
            INSERT INTO courts (name, club_id)
            VALUES (?, ?)
            ON CONFLICT (crid)
            DO UPDATE SET
                name = EXCLUDED.name,
                club_id = EXCLUDED.club_id;
            """.trimIndent()

        connection.prepareStatement(sqlSave).use { stmt ->
            stmt.setString(1, element.name.value)
            stmt.setInt(2, element.club.cid.toInt())

            stmt.executeUpdate()
        }
    }

    /**
     * Function that finds a court by its crid.
     * @param id Identifier of the class, corresponding to the PK of the respective table, in this case crid
     * @return Court if found, otherwise null
     */
    override fun findByIdentifier(id: UInt): Court? {
        val sqlSelect =
            """
            ${courtSqlReturnFormat()}
            WHERE cr.crid = ?
            """.trimIndent()

        return connection.prepareStatement(sqlSelect).use { stmt ->
            stmt.setInt(1, id.toInt())

            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.mapCourt() else null
            }
        }
    }

    /**
     * Function that returns limit elements after offset, from latest tuple to be created to oldest
     * @param limit Number of tuples to retrieve, default of 30
     * @param offset Number of tuples to skip at the beginning, default of 0
     * @return List of Courts retrieved
     */
    override fun findAll(
        limit: Int,
        offset: Int,
    ): List<Court> {
        val sqlSelect =
            """
            ${courtSqlReturnFormat()}
            ORDER BY cr.crid DESC
            LIMIT ? OFFSET ?
            """.trimIndent()

        return connection.prepareStatement(sqlSelect).use { stmt ->
            stmt.setInt(1, limit)
            stmt.setInt(2, offset)

            stmt.executeQuery().use { rs ->
                val courts = mutableListOf<Court>()
                while (rs.next()) {
                    courts.add(rs.mapCourt())
                }
                courts
            }
        }
    }

    override fun findAllCourtsThatHaveRentalsByRenterId(
        renter: UInt,
        limit: Int,
        offset: Int,
    ): List<Court> {
        val sqlSelect =
            """
            ${courtSqlReturnFormat()}
            WHERE cr.crid IN (
                SELECT DISTINCT cr.crid
                FROM rentals r
                JOIN courts cr ON r.crid = cr.crid
                WHERE r.renter_id = ?
            )
            ORDER BY cr.crid DESC
            LIMIT ? OFFSET ?
            """.trimIndent()

        return connection.prepareStatement(sqlSelect).use { stmt ->
            stmt.setInt(1, renter.toInt())
            stmt.setInt(2, limit)
            stmt.setInt(3, offset)

            stmt.executeQuery().use { rs ->
                val courts = mutableListOf<Court>()
                while (rs.next()) {
                    courts.add(rs.mapCourt())
                }
                courts
            }
        }
    }

    /**
     * Function that deletes a court if exists a tuple with the crid, if it doesn't exist, does nothing
     * @param id Identifier of the Court to delete
     */
    override fun deleteByIdentifier(id: UInt) {
        val sqlDelete = "DELETE FROM courts WHERE crid = ?"

        connection.prepareStatement(sqlDelete).use { stmt ->
            stmt.setInt(1, id.toInt())
            stmt.executeUpdate()
        }
    }

    /**
     * Function that deletes every entry of the table,
     *  resets autoincremented values and any rows that have references to it
     */
    override fun clear() {
        val sqlDelete = "TRUNCATE TABLE courts RESTART IDENTITY CASCADE"
        connection.prepareStatement(sqlDelete).use { stmt ->
            stmt.executeUpdate()
        }
    }

    override fun count(cid: UInt): Int {
        val sqlCount = "SELECT COUNT(*) FROM courts where club_id = ?"
        return connection.prepareStatement(sqlCount).use { stmt ->
            stmt.setInt(1, cid.toInt())
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    override fun countByUser(uid: UInt): Int{
        val sqlCount = """
            SELECT COUNT(*)
            FROM courts cr
            JOIN rentals r ON cr.crid = r.court_id
            WHERE r.renter_id = ?
            """.trimIndent()
        return connection.prepareStatement(sqlCount).use { stmt ->
            stmt.setInt(1, uid.toInt())
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }
}

/**
 * Function with the default select query to retrieve a court with the information of the club
 * @return the default select query string
 */
fun courtSqlReturnFormat() =
    """
    SELECT cr.crid as court_id, cr.name as court_name, cr.club_id as club_id,
        c.name as club_name, c.owner as club_owner_id,
        u.name as club_owner_name, u.email as club_owner_email, u.token as club_owner_token
    FROM courts cr
    LEFT JOIN clubs c ON cr.club_id = c.cid
    LEFT JOIN users u ON c.owner = u.uid
    """.trimIndent()

/**
 * Function that maps a ResultSet to a Court, according to the name dictionary defined,
 *  in this case the one defined in the default select query
 * @return The mapped Court
 */
fun ResultSet.mapCourt(): Court =
    Court(
        crid = getInt("court_id").toUInt(),
        name = getString("court_name").toName(),
        club =
            Club(
                cid = getInt("club_id").toUInt(),
                name = getString("club_name").toName(),
                owner =
                    User(
                        uid = getInt("club_owner_id").toUInt(),
                        name = getString("club_owner_name").toName(),
                        email = getString("club_owner_email").toEmail(),
                        token = getString("club_owner_token").toToken(),
                    ),
            ),
    )

/**
 * Function that maps a ResultSet to a Court, according to the name dictionary defined,
 *  in this case the one defined in the default select query, doesn't read club through ResultSet
 * @param club Club
 * @return The mapped Court
 */
fun ResultSet.mapCourt(club: Club) =
    Court(
        crid = getInt("court_id").toUInt(),
        name = getString("court_name").toName(),
        club = club,
    )
