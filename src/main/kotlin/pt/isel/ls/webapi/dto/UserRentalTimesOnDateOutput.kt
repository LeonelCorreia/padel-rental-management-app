package pt.isel.ls.webapi.dto

import kotlinx.serialization.Serializable
import pt.isel.ls.domain.User

@Serializable
class UserRentalTimesOnDateOutput(
    val renter: UserDetails,
    val times: Int,
) {
    constructor(
        user: User,
        times: Int
    ) : this(
        UserDetails(user),
        times
    )
}