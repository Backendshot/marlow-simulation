package com.marlow.todo.route

import com.marlow.todo.controller.TodoController
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import com.marlow.global.GlobalResponse
import com.marlow.global.GlobalResponseData
import com.marlow.todo.model.Todo
import com.marlow.todo.model.TodoValidator
import com.zaxxer.hikari.HikariDataSource
import de.mkammerer.argon2.Argon2Factory
import kotlinx.serialization.encodeToString

fun Route.todoRouting(ds: HikariDataSource) {

    route("/todos") {
        get {
            val todos = TodoController(ds).fetchTodos()
            call.respond(HttpStatusCode.OK, GlobalResponseData(200, true, "Success", todos))
        }
        get("/{id?}") {
            try {
                val id: Int = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
                    "Missing id", status = HttpStatusCode.BadRequest
                )
                val todo = TodoController(ds).fetchTodoById(id)
//                call.respond(Json.encodeToString(todo))
                call.respond(HttpStatusCode.OK, GlobalResponseData(200, true, "Success", todo))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.toString()))
            }
        }
        get("/import-data-todos") {
            try {
                val todos = TodoController(ds).importTodoData()
                call.respond(
                    HttpStatusCode.OK, GlobalResponse(200, true, "Successfully imported todo data with $todos rows")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    GlobalResponse(500, false, "Invalid JSON types: ${e.localizedMessage}")
                )
            }
        }
        post("/hash") {
            try {
                val input = call.receive<Map<String, String>>()
                val password = input["password"]?.toCharArray()
//                val element: CharArray = Json.encodeToString(password).toCharArray()
                println("Char array flattened: ${password?.concatToString()}")
                println("Password plaintext: $password")
                val argon2 = Argon2Factory.create()
                val hash = argon2.hash(1, 65536, 1, password)
                println("Hashed password: $hash")

                println(argon2.verify(hash, password))
                call.respond(
                    HttpStatusCode.OK, GlobalResponseData(
                        200,
                        true,
                        "The following are equivalent:",
                        mapOf("password" to input["password"], "password_array" to password.toString(), "hash" to hash)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage)
                )
            }
        }
    }
    route("/api/v2/") {
        get("readall") {
            val getTodos = TodoController(ds).readAllTodos()
//            call.respond(Json.encodeToString(getTodos))
            call.respond(HttpStatusCode.OK, GlobalResponseData(200, true, "Success", getTodos))
        }

        get("read/{id?}") {
            try {
                val id: Int = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
                    "Missing id", status = HttpStatusCode.BadRequest
                )
                val getTodo = TodoController(ds).readTodoById(id);
//                call.respond(Json.encodeToString(getTodo))
                call.respond(HttpStatusCode.OK, GlobalResponseData(200, true, "Success", getTodo))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest, GlobalResponse(404, false, "Todo not found: ${e.localizedMessage}")
                )
            }
        }

        post("create") {
            try {
                val raw = call.receiveText()
                val element = Json.parseToJsonElement(raw)
                val obj = element.jsonObject

                val jsonValidationErrors = TodoValidator().validate(obj)

                if (jsonValidationErrors.isNotEmpty()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        GlobalResponse(400, false, "Invalid JSON types: $jsonValidationErrors")
                    )
                }
                println("No validation errors")

                val todo = Json.decodeFromJsonElement<Todo>(element)
                val resultMap = TodoController(ds).createTodo(todo)
                if (resultMap.component2() == 0) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        GlobalResponse(500, false, "Todo was not added. Please try again.")
                    )
                }
                call.respond(
                    HttpStatusCode.Created,
                    GlobalResponse(201, true, "Todo with ID #${resultMap.component1()} has been added.")
                )
            } catch (e: SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest, GlobalResponse(400, false, "Invalid JSON types: ${e.localizedMessage}")
                )
            } catch (e: CannotTransformContentToTypeException) {
                call.respond(
                    HttpStatusCode.BadRequest, GlobalResponse(400, false, "Wrong input: ${e.localizedMessage}")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage)
                )
            }
        }

        patch("update/{id?}") {
            try {
                val id: Int = call.parameters["id"]?.toIntOrNull() ?: return@patch call.respond(
                    status = HttpStatusCode.BadRequest, GlobalResponse(400, false, "Missing id")
                )
                val raw = call.receiveText()
                val element = Json.parseToJsonElement(raw)
                val obj = element.jsonObject

                val jsonValidationErrors = TodoValidator().validate(obj)

                if (jsonValidationErrors.isNotEmpty()) {
                    return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        GlobalResponse(400, false, "Invalid JSON types: $jsonValidationErrors")
                    )
                }
                val todoData = Json.decodeFromJsonElement<Todo>(obj)
                val result = TodoController(ds).updateTodo(id, todoData)
                when (result) {
                    0 -> call.respond(
                        HttpStatusCode.InternalServerError,
                        GlobalResponse(500, false, "Could not update Todo. Please try again.")
                    )

                    1 -> call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Todo updated successfully."))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError, GlobalResponse(500, false, "${e.localizedMessage}")
                )
            }
        }

        delete("delete/{id?}") {
            try {
                val id: Int = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(
                    status = HttpStatusCode.BadRequest, GlobalResponse(400, false, "Missing id")
                )

                val checkValue = TodoController(ds).deleteTodo(id)

                if (checkValue < 1) return@delete call.respond(
                    HttpStatusCode.InternalServerError,
                    GlobalResponse(500, false, "Deletion not successful, please try again.")
                )

                call.respond(status = HttpStatusCode.OK, GlobalResponse(200, true, "Todo deleted successfully."))
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.BadRequest, GlobalResponse(500, false, "Server Error: ${e.toString()}")
                )
            }
        }
    }
}
