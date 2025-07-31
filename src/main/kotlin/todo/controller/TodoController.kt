package com.marlow.todo.controller

import com.marlow.client
import com.marlow.todo.query.TodoQuery
import com.marlow.configuration.Config
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.marlow.todo.model.Todo
import com.marlow.todo.model.TodoValidator
import com.zaxxer.hikari.HikariDataSource
import java.net.URL
import java.sql.Statement
import java.sql.Types
import kotlin.collections.find
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.text.isEmpty
import kotlin.use

class TodoController(private val ds: HikariDataSource, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    val apiUrl = "https://jsonplaceholder.typicode.com/todos"
    val todoRetrieveFail = "Failed to retrieve todos"

    suspend fun createTodo(todo: Todo): Pair<Int, Int> = withContext(dispatcher) {
        // Validate
        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todo)
        val validationErrors = validator.validate(sanitizedTodo)

        if (validationErrors.isEmpty()) {
            println("✅ Validation passed! No errors found.")
            println("Sanitized and Validated Todo: $sanitizedTodo")
        } else {
            throw Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        val conn = ds.connection
        conn.use {
            val stmt = it.prepareStatement(
                TodoQuery.INSERT_TODO,
                Statement.RETURN_GENERATED_KEYS
            )
            stmt.use {
                it.setInt(1, sanitizedTodo.userId)
                it.setString(2, sanitizedTodo.title)
                it.setBoolean(3, sanitizedTodo.completed)

                val rowsInserted = it.executeUpdate()

                val rs = it.generatedKeys
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

    suspend fun viewAllTodosById(user_id: Int): List<Todo> = withContext(Dispatchers.IO) {
        val todos = mutableListOf<Todo>()
        ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.GET_TODO_BY_ID).use { stmt ->
                stmt.setInt(1, user_id)
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

    suspend fun updateTodo(id: Int, todo: Todo): Int = withContext(dispatcher) {
        if (id != todo.id) {
            throw kotlin.Exception("id to be updated must match api request")
        }
        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todo)
        val validationErrors = validator.validate(sanitizedTodo)

        if (validationErrors.isEmpty()) {
            println("Sanitized and Validated Todo: $sanitizedTodo")
        } else {
            throw kotlin.Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        ds.connection.use { conn ->
            conn.prepareCall(TodoQuery.UPDATE_TODO).use { stmt ->
                stmt.setInt(1, todo.userId)
                stmt.setInt(2, id)
                stmt.setString(3, todo.title)
                stmt.setBoolean(4, todo.completed)
                stmt.registerOutParameter(5, Types.INTEGER)
                stmt.execute()
                stmt.getInt(5)
            }
        }
    }

    suspend fun deleteTodo(id: Int): Int = withContext(dispatcher) {
        ds.connection.use { conn ->
            conn.prepareCall(TodoQuery.DELETE_TODO).use { stmt ->
                stmt.setInt(1, id)
                stmt.registerOutParameter(2, Types.INTEGER)

                stmt.execute()
                stmt.getInt(2)
            }
        }

        //Todo: add a message that indicates that the todo to be deleted does not exist
//        return@withContext result
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


    suspend fun fetchTodoById(id: Int): Todo = withContext(dispatcher) {
        try {
            val request: List<Todo> = client.get(apiUrl).body()
            println(request)
            if (request.isEmpty()) {
                throw kotlin.Exception(todoRetrieveFail)
            }
            return@withContext request.find { it.id == id } ?: throw kotlin.Exception("Todo with id #$id not found")
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