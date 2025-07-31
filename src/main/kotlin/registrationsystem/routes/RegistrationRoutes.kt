import com.marlow.global.GlobalResponse
import com.marlow.global.RegistrationResult
import com.marlow.registrationsystem.controllers.RegistrationController
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
        post("/register") {
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

        get("email/verify") {
            val result = RegistrationController(ds).verifyEmail(call)

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
}
