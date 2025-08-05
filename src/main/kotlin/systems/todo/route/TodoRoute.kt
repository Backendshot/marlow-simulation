package com.marlow.systems.todo.route

import com.marlow.globals.GlobalResponse
import com.marlow.globals.GlobalResponseData
import com.marlow.systems.todo.controller.TodoController
import com.marlow.systems.todo.model.Todo
import com.zaxxer.hikari.HikariDataSource
import de.mkammerer.argon2.Argon2Factory
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

fun Route.todoRouting(ds: HikariDataSource) {
    route("/todos") {
        get {
            call.respond(
                    HttpStatusCode.OK,
                    GlobalResponseData(200, true, "Success", TodoController(ds).fetchTodos())
            )
        }
        get("/{id?}") {
            val id: Int = call.requireIntParam("id")
            call.respond(
                    HttpStatusCode.OK,
                    GlobalResponseData(200, true, "Success", TodoController(ds).fetchTodoById(id))
            )
        }
        get("/import-data-todos") {
            val count = TodoController(ds).importTodoData()
            call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Imported $count rows"))
        }
    }
    route("/api/v2/") {
        get("readall") {
            call.respond(
                    HttpStatusCode.OK,
                    GlobalResponseData(200, true, "Success", TodoController(ds).readAllTodos())
            )
        }

        get("read/{user_id?}") {
            try {
                val userId =
                        call.parameters["user_id"]?.toIntOrNull()
                                ?: throw IllegalArgumentException("Invalid User ID")
                val todos = TodoController(ds).viewAllTodosById(userId)
                call.respond(HttpStatusCode.OK, GlobalResponseData(200, true, "Success", todos))
            } catch (e: IllegalArgumentException) {
                call.respond(
                        HttpStatusCode.BadRequest,
                        GlobalResponse(400, false, e.message ?: "Invalid request")
                )
            } catch (e: Exception) {
                call.respond(
                        HttpStatusCode.InternalServerError,
                        GlobalResponse(500, false, e.localizedMessage)
                )
            }
        }

        post("create") {
            val raw = call.receiveText()
            val element = Json.parseToJsonElement(raw)

            val todo = Json.decodeFromJsonElement<Todo>(element)
            val (newId, rows) = TodoController(ds).createTodo(todo)
            if (rows == 0) throw Exception("Todo has not been created. Please try again.")
            call.respond(HttpStatusCode.Created, GlobalResponse(201, true, "Created #$newId"))
        }

        patch("update/{id}") {
            val id = call.requireIntParam("id")
            val raw = call.receiveText()
            val element = Json.parseToJsonElement(raw)

            val todo = Json.decodeFromJsonElement<Todo>(element).copy(id = id)
            if (TodoController(ds).updateTodo(id, todo) == 0) {
                throw Exception("Could not update Todo. Please try again.")
            }
            call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Todo updated successfully."))
        }

        delete("delete/{id?}") {
            val id = call.requireIntParam("id")
            if (TodoController(ds).deleteTodo(id) == 0)
                    throw Exception("Deletion not successful, please try again.")
            call.respond(
                    status = HttpStatusCode.OK,
                    GlobalResponse(200, true, "Todo deleted successfully.")
            )
        }

        patch("delete-table/{id?}") {
            val id = call.requireIntParam("id")
            if (TodoController(ds).updateDeleteStatus(id) == 0) {
                throw Exception("Deletion of table not successful, please try again.")
            }
            call.respond(
                    status = HttpStatusCode.OK,
                    GlobalResponse(200, true, "Table marked as deleted successfully.")
            )
        }
    }
}
