import com.marlow.global.GlobalResponse
import com.marlow.global.RegistrationResult
import com.marlow.registrationSystem.controllers.RegistrationController
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDateTime

fun Route.registrationRouting(ds: HikariDataSource) {
    route("/api/user") {
        post ("/register") {
            // TODO: Add a logger here

            val result = RegistrationController(ds).register(call)

            when (result) {
                is RegistrationResult.Success -> {
                    call.respond(
                        HttpStatusCode.Created,
                        GlobalResponse(code = 201, status = true, message = result.message)
                    )
                }

                is RegistrationResult.ValidationError -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GlobalResponse(code = 400, status = false, message = result.message)
                    )
                }

                is RegistrationResult.Conflict -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        GlobalResponse(code = 409, status = false, message = result.message)
                    )
                }

                is RegistrationResult.Failure -> {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        GlobalResponse(code = 500, status = false, message = result.message)
                    )
                }
            }
        }
    }

    route("/api/user") {
        get ("email/verify") {
            val userIdParam = call.request.queryParameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing userId"
            )

            val userId = userIdParam.toIntOrNull()
            if (userId == null) {
                return@get call.respond(HttpStatusCode.BadRequest, "Invalid userId")
            }

            val now = LocalDateTime.now()

            try {
                ds.connection.use { conn ->
                    val stmt = conn.prepareStatement("""
                        UPDATE tbl_email_sending
                        SET status = 'VERIFIED', verified_at = ?
                        WHERE user_id = ?
                    """.trimIndent())

                    stmt.setObject(1, now)
                    stmt.setInt(2, userId)

                    val rows = stmt.executeUpdate()
                    if (rows > 0) {
                        call.respondText("Email verification successful!", ContentType.Text.Plain)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "User email log not found.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "An error occurred.")
            }
        }
    }
}
