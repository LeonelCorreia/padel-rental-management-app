package pt.isel.ls

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import pt.isel.ls.repository.jdbc.TransactionManagerJdbc
import pt.isel.ls.services.ClubService
import pt.isel.ls.services.CourtService
import pt.isel.ls.services.RentalService
import pt.isel.ls.services.UserService
import pt.isel.ls.webapi.ClubWebApi
import pt.isel.ls.webapi.CourtWebApi
import pt.isel.ls.webapi.RentalWebApi
import pt.isel.ls.webapi.UserWebApi
import java.sql.DriverManager

private val logger = LoggerFactory.getLogger("HTTPServer")

val DB_URL = System.getenv("JDBC_DATABASE_URL") ?: throw Exception("Missing DB_URL environment variable")

fun main() {
    val connection = DriverManager.getConnection(DB_URL)
    val trxManagerJdbc = TransactionManagerJdbc(connection)

    val userApi = UserWebApi(UserService(trxManagerJdbc))
    val clubApi =
        ClubWebApi(
            ClubService(trxManagerJdbc),
            UserService(trxManagerJdbc),
            RentalService(trxManagerJdbc),
        )
    val courtApi =
        CourtWebApi(
            CourtService(trxManagerJdbc),
            UserService(trxManagerJdbc),
        )
    val rentalApi =
        RentalWebApi(
            RentalService(trxManagerJdbc),
            UserService(trxManagerJdbc),
        )

    val userRoutes =
        routes(
            "/" bind POST to userApi::createUser,
            "/{uid}" bind GET to userApi::getUserInfo,
            "/{uid}/rentals" bind GET to rentalApi::getUserRentals,
        )
    val clubsRoutes =
        routes(
            "/" bind POST to clubApi::createClub,
            "/" bind GET to clubApi::getAllClubs,
            "/{cid}" bind GET to clubApi::getClubInfo,
        )
    val courtsRoutes =
        routes(
            "/" bind POST to courtApi::createCourt,
            "/clubs/{cid}" bind GET to courtApi::getCourtsByClub,
            "/{crid}" bind GET to courtApi::getCourtInfo,
            "/{crid}/available" bind POST to clubApi::getAvailableHours,
            "/users/{uid}" bind GET to courtApi::getCourtsUserRentals,
        )
    val rentalsRoutes =
        routes(
            "/" bind POST to rentalApi::createRental,
            "/{rid}" bind GET to rentalApi::getRentalInfo,
            "/{rid}" bind DELETE to rentalApi::deleteRental,
            "/{rid}" bind PUT to rentalApi::updateRental,
            "/courts/{crid}" bind GET to rentalApi::getRentalsOnCourt,
        )

    val app =
        routes(
            "/users" bind userRoutes,
            "/clubs" bind clubsRoutes,
            "/courts" bind courtsRoutes,
            "/rentals" bind rentalsRoutes,
            singlePageApp(ResourceLoader.Directory("static-content")),
        )

    val jettyServer = app.asServer(Jetty(9000)).start()
    logger.info("server started listening")

    readln()
    jettyServer.stop()

    connection.close()

    logger.info("leaving Main")
}
