package com.marlow.systems.todo.controller

import com.marlow.client
import com.marlow.systems.todo.model.Todo
import com.marlow.systems.todo.model.TodoValidator
import com.marlow.systems.todo.query.TodoQuery
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.sql.Statement
import java.sql.Types
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.emptyList
import kotlin.collections.find
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.text.isEmpty

class TodoController(
    private val ds: HikariDataSource, private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val apiUrl = "https://jsonplaceholder.typicode.com/todos"
    val todoRetrieveFail = "Failed to retrieve todos"

    suspend fun createTodo(todoParam: Todo): Pair<Int, Int> = withContext(dispatcher) {
        // Validate
        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todoParam)
        val validationErrors = validator.validate(sanitizedTodo)

        if (validationErrors.isEmpty()) {
            println("✅ Validation passed! No errors found.")
            println("Sanitized and Validated Todo: $sanitizedTodo")
        } else {
            throw Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.INSERT_TODO, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, sanitizedTodo.userId)
                stmt.setString(2, sanitizedTodo.title)
                stmt.setBoolean(3, sanitizedTodo.completed)
                stmt.setBoolean(4, sanitizedTodo.isDeleted)

                val rowsInserted = stmt.executeUpdate()

                val rs = stmt.generatedKeys
                val generatedId = if (rs.next()) rs.getInt(1) else null

                if (generatedId == null) throw Exception("Failed to retrieve generated ID")

                return@withContext generatedId to rowsInserted
            }
        }
    }

    suspend fun readAllTodos(): MutableList<Todo> = withContext(dispatcher) {
        val data = mutableListOf<Todo>()
        ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.GET_ALL_TODOS).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val userId: Int = rs.getInt("user_id")
                        val id: Int = rs.getInt("id")
                        val title = rs.getString("title")
                        val completed = rs.getBoolean("completed")
                        data.add(Todo(userId, id, title, completed))
                    }
                    return@withContext data
                }
            }
        }
    }

    suspend fun viewAllTodosById(userIdParam: Int): List<Todo> = withContext(dispatcher) {
        val todos = mutableListOf<Todo>()
        ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.GET_TODO_BY_ID).use { stmt ->
                stmt.setInt(1, userIdParam)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val userId: Int = rs.getInt("user_id")
                        val id: Int = rs.getInt("id")
                        val title = rs.getString("title")
                        val completed = rs.getBoolean("completed")
                        todos.add(Todo(userId, id, title, completed))
                    }
                }
            }
        }
        return@withContext todos
    }

    suspend fun updateTodo(idParam: Int, todoParam: Todo): Int = withContext(dispatcher) {
        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todoParam)
        val validationErrors = validator.validate(sanitizedTodo)

        if (validationErrors.isNotEmpty()) {
            throw Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.UPDATE_TODO).use { stmt ->
                stmt.setString(1, sanitizedTodo.title)
                stmt.setBoolean(2, sanitizedTodo.completed)
                stmt.setInt(3, idParam)

                return@use stmt.executeUpdate()
            }
        }
    }

    suspend fun updateDeleteStatus(idParam: Int): Int = withContext(dispatcher) {
        ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.DELETE_TABLE_QUERY).use { stmt ->
                stmt.setInt(1, idParam)
                return@use stmt.executeUpdate()
            }
        }
    }

    suspend fun deleteTodo(idParam: Int): Int = withContext(dispatcher) {
        ds.connection.use { conn ->
            conn.prepareCall(TodoQuery.DELETE_TODO).use { stmt ->
                stmt.setInt(1, idParam)
                stmt.registerOutParameter(2, Types.INTEGER)

                stmt.execute()
                stmt.getInt(2)
            }
        }
    }

    suspend fun fetchTodos(): List<Todo> = withContext(dispatcher) {
        try {
            val response: List<Todo> = client.get(apiUrl).body()
            if (response.isEmpty()) {
                throw kotlin.Exception(todoRetrieveFail)
            }

            val validator = TodoValidator()
            val sanitizedTodos = response.map { validator.sanitize(it) }
            val validationErrors = sanitizedTodos.flatMap { validator.validate(it) }

            if (validationErrors.isEmpty()) {
                println("Sanitized and Validated Todos Successfully")
            } else {
                println("Validation Errors: ${validationErrors.joinToString(", ")}")
            }

            return@withContext sanitizedTodos
        } catch (e: Exception) {
            println("Error fetching todos: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun fetchTodoById(idParam: Int): Todo = withContext(dispatcher) {
        try {
            val request: List<Todo> = client.get(apiUrl).body()
            println(request)
            if (request.isEmpty()) {
                throw kotlin.Exception(todoRetrieveFail)
            }
            return@withContext request.find { it.id == idParam }
                ?: throw kotlin.Exception("Todo with id #$idParam not found")
        } catch (e: Exception) {
            println("Error fetching todo by ID: ${e.message}")
            throw e
        }
    }

    suspend fun importTodoData(): Int = withContext(dispatcher) {
        try {
            val request: String = client.get(apiUrl).body()
            if (request.isEmpty()) {
                throw kotlin.Exception(todoRetrieveFail)
            }

            val todos = Json.decodeFromString<List<Todo>>(request)

            val validator = TodoValidator()
            val sanitizedTodos = todos.map { validator.sanitize(it) }
            val validationErrors = sanitizedTodos.flatMap { validator.validate(it) }

            if (validationErrors.isEmpty()) {
                println("Sanitized and Validated Todos")
            } else {
                println("Validation Errors: ${validationErrors.joinToString(", ")}")
            }

            var insertCount: Int
            ds.connection.use { conn ->
                conn.prepareCall(TodoQuery.INSERT_TODO).use { stmt ->
                    for (todo in sanitizedTodos) {
                        stmt.setInt(1, todo.userId)
                        stmt.setString(2, todo.title)
                        stmt.setBoolean(3, todo.completed)
                        stmt.addBatch()
                    }
                    val result = stmt.executeBatch()
                    insertCount = result.size
                    return@withContext insertCount
                }
            }
        } catch (e: Exception) {
            println("Error importing todo data: ${e.message}")
            throw e
        }
    }
}
