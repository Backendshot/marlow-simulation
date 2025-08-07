import com.marlow.globals.GlobalResponse
import com.marlow.globals.RegistrationResult
import com.marlow.systems.registration.controllers.RegistrationController
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.registrationRouting(ds: HikariDataSource) {
    route("/user") {
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
        get("/email/verify") {
            RegistrationController(ds).verifyEmail(call)
        }
    }
}
