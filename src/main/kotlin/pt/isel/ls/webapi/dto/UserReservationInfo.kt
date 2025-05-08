package pt.isel.ls.webapi.dto

import pt.isel.ls.domain.User

data class UserReservationInfo(
    val user: User,
    val numOfReservation: UInt
)
