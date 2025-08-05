import com.marlow.globals.GlobalResponse
import com.marlow.globals.GlobalResponseData
import com.marlow.globals.RegistrationResult
import com.marlow.globals.VerificationResult
import com.marlow.systems.registration.controllers.RegistrationController
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.*

fun Route.registrationRouting(ds: HikariDataSource) {
    route("/api/user") {
        post ("/register") {
            val result = RegistrationController(ds).register(call)

            when (result) {
                is RegistrationResult.Success -> {
                    call.respond(
                        HttpStatusCode.Created,
                        GlobalResponseData(code = 201, status = true, message = result.message, data = "user_id: " + result.userId)
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
            val result    = RegistrationController(ds).verifyEmail(call)
            val uiLink    = "http://127.0.0.1:5500/welcome.html?status="
            val permanent = false

            when (result) {
                is VerificationResult.Success -> {
                    call.respondRedirect(uiLink + "success", permanent)
                }
                is VerificationResult.Failure -> {
                    call.respondRedirect(uiLink + "failed", permanent)
                }
                is VerificationResult.NotFound -> {
                    call.respondRedirect(uiLink + "not_found", permanent)
                }
                is VerificationResult.Error -> {
                    call.respondRedirect(uiLink + "error", permanent)
                }
            }
        }
    }
}
