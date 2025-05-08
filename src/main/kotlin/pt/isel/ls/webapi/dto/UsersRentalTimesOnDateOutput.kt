package pt.isel.ls.webapi.dto

import kotlinx.serialization.Serializable
import pt.isel.ls.domain.User

@Serializable
class UsersRentalTimesOnDateOutput (
    val usersRentalTimes: List<UserRentalTimesOnDateOutput>,
    val paginationInfo: PaginationInfo,
)

fun Map<User, Int>.toUsersRentalTimes(paginationInfo: PaginationInfo) =
    UsersRentalTimesOnDateOutput(
        this.map { (user, times) ->
            UserRentalTimesOnDateOutput(user, times)
        },
        paginationInfo,
    )
