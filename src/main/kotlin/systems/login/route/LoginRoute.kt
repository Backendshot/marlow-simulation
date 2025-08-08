package com.marlow.systems.login.route

import com.marlow.globals.ErrorHandler
import com.marlow.globals.GlobalMethods
import com.marlow.globals.GlobalResponse
import com.marlow.systems.login.controller.LoginController
import com.marlow.systems.login.model.ImageUploadResponse
import com.marlow.systems.login.model.LoginRequest
import com.marlow.systems.login.model.LogoutRequest
import com.marlow.systems.login.model.Validator
import com.marlow.systems.login.util.LoginAudit
import com.marlow.systems.login.util.LoginJWT
import com.marlow.systems.login.util.LoginSession
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.LoginRoute(ds: HikariDataSource) {

    val loginController = LoginController(ds)
    val globalMethod = GlobalMethods()

    route("/user") {
        post("/login") {
            try {
                val loginData = call.receive<LoginRequest>()
                val browserInfo = LoginAudit().parseBrowser(call.request.headers["User-Agent"] ?: "Unknown")
                // Validate input
                val validator = Validator()
                val sanitizedLogin = validator.sanitizeInput(loginData)
                val errors = validator.validateLoginInput(sanitizedLogin)

                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, "Validation Errors: ${errors.joinToString(", ")}"))
                }
                // Get userId and hash
                val userIdAndHash = loginController.getUserIdAndHash(sanitizedLogin.username)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalResponse(401, false, "Invalid username or password."))

                val (userId, storedHash) = userIdAndHash

                // Check password
                val argon2 = de.mkammerer.argon2.Argon2Factory.create()
                if (!argon2.verify(storedHash, sanitizedLogin.password.toCharArray())) {
                    return@post call.respond(HttpStatusCode.Unauthorized, GlobalResponse(401, false, "Invalid username or password."))
                }
                // Check email status
                if (!loginController.checkEmailStatus(userId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalResponse(403, false, "Email not verified. Please check your email to verify."))
                }
                // Generate JWT and session
                val jwtToken = LoginJWT.generateJWT(userId)
                val sessionId = LoginSession.generatedSessionId()
                val sessionDeleted = false
                // Update session
                loginController.updateSession(userId, sessionId, jwtToken, sessionDeleted)
                // Insert audit
                loginController.insertAudit(userId, browserInfo)
                // Create Response
                val response = loginController.loginResponse(userId, sanitizedLogin.username, jwtToken, sessionId, sessionDeleted)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)

            }
        }

        get("/profile-image/{userId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalResponse(404, false, "Invalid User ID"))

                val userProfile = loginController.getUserProfile(userId)
                val imageUrlOrFilename = userProfile.userImg

                //Commented section for local uploads
//                val filename = imageUrlOrFilename.substringAfterLast('/')
//
//                val imageFile = File("image-uploads", filename)
//                val fallback = File("image-uploads/default.png")
//
//                if (imageFile.exists()) {
//                    call.respondFile(imageFile)
//                } else if (fallback.exists()) {
//                    call.respondFile(fallback)
//                } else {
//                    call.respond(HttpStatusCode.NotFound, GlobalResponse(404, false, "No image found"))
//                }

                //uncomment if storing URLs instead of filenames
//                val dotenv = dotenv()
//                val supabaseUrl = dotenv["SUPABASE_URL"]
//                val bucketName = "image-uploads"
//                val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/${URLEncoder.encode(imageUrlOrFilename, "UTF-8")}"

                call.respond(HttpStatusCode.OK, mapOf("imageUrl" to imageUrlOrFilename))
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)
            }
        }

        patch("/profile-image/update/{userId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest,GlobalResponse(404, false, "Invalid User ID for image update"))

                val multipart = call.receiveMultipart()
                var uploadedFileName: String? = null
                var imageError: String? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem && part.name == "image") {
                        try {
                            uploadedFileName = globalMethod.uploadImageToSupabase(part, "image-uploads")
                                //.saveImage(part) //for local uploads
                        } catch (e: Exception) {
                            imageError = "Invalid image format."
                        }
                    }
                    part.dispose()
                }

                if (imageError != null)
                    return@patch call.respond(HttpStatusCode.BadRequest,GlobalResponse(400, false, imageError))

                if (uploadedFileName == null)
                    return@patch call.respond(HttpStatusCode.BadRequest,GlobalResponse(400, false, "No image uploaded."))

                val currentImageFilename = loginController.getCurrentUserImage(userId)
                currentImageFilename?.let { filename ->
//                    val file = File("image-uploads/$filename") //for local uploads
//                    if (file.exists()) file.delete() //for local uploads
                    if (filename.isNotBlank()) { //handles edge cases like blank strings ("")
                        globalMethod.deleteImageFromSupabase(filename, bucketName = "image-uploads")
                    }
                    return@patch call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Image File Removed"))
                }

                val result = loginController.patchUserProfile(userId, uploadedFileName)

                if (!result)
                    return@patch call.respond(HttpStatusCode.InternalServerError,GlobalResponse(500, false, "Failed to update profile image."))

                call.respond(HttpStatusCode.OK,
                    ImageUploadResponse(
                        success = true,
                        message = "Profile image updated.",
                        filename = uploadedFileName
                    ))
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)
            }
        }

        post("/logout") {
            try {
                val logoutData = call.receive<LogoutRequest>()
                val result = loginController.logout(logoutData.userId)
                if (!result) {
                    return@post call.respond(HttpStatusCode.NotFound, GlobalResponse(404, false, "User session not found"))
                }
                call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Logout successful"))
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)
            }
        }

        get("/audit/{userId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid User ID")
                val auditData = loginController.viewAllAuditById(userId)
                call.respond(HttpStatusCode.OK, auditData)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, e.message ?: "Invalid request"))
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)
            }
        }
    }
}