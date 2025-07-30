package com.marlow.todo.controller

import com.marlow.client
import com.marlow.todo.query.TodoQuery
import com.marlow.configuration.Config
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.marlow.todo.model.Todo
import com.marlow.todo.model.TodoValidator
import com.zaxxer.hikari.HikariDataSource
import java.net.URL
import java.sql.Types
import kotlin.collections.find
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.text.isEmpty
import kotlin.use

class TodoController(private val ds: HikariDataSource) {

//    val connection = Config().createDataSource() //.connect()

    suspend fun createTodo(todo: Todo): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // Check duplicates
        ds.connection.use { conn ->
            conn.prepareCall(TodoQuery.CHECK_DUPLICATE_TODO).use { duplicateStmt ->
                duplicateStmt.setInt(1, todo.id)
                duplicateStmt.executeQuery().use { rs ->
                    rs.next()
                        .takeIf { !rs.getBoolean(1) } //takeIf will null the value if the returned value is true (there *is* a duplicate id)
                        ?: throw kotlin.Exception("Todo at id #${todo.id} already exists") //a null value results in an Exception being thrown
                }
            }
        }

        // Validate
        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todo)
        val validationErrors = validator.validate(sanitizedTodo)
        // Inspect and either print or throw
        if (validationErrors.isEmpty()) {
            println("✅ Validation passed! No errors found.")
            println("Sanitized and Validated Todo: $sanitizedTodo")
        } else {
            throw Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        var count: Int
        val rowsInserted = ds.connection.use { conn ->
            conn.prepareStatement(TodoQuery.INSERT_TODO).use { stmt ->
                stmt.setInt(1, sanitizedTodo.userId)
                stmt.setInt(2, sanitizedTodo.id)
                stmt.setString(3, sanitizedTodo.title)
                stmt.setBoolean(4, sanitizedTodo.completed)
                stmt.execute()
                count = stmt.updateCount

                println(count) //currently returns -1 on successful update, but otherwise works with current logic

            }
        }

        return@withContext todo.id to count
    }

    suspend fun readAllTodos(): MutableList<Todo> = withContext(Dispatchers.IO) {
        val data = mutableListOf<Todo>()
        val query = ds.connection.prepareStatement("SELECT * FROM todos")
        val result = query.executeQuery()
        while (result.next()) {
            val userId: Int = result.getInt("user_id")
            val id: Int = result.getInt("id")
            val title = result.getString("title")
            val completed = result.getBoolean("completed")
            data.add(Todo(userId, id, title, completed))
        }
        return@withContext data
    }

    suspend fun viewAllTodosById(user_id: Int): List<Todo> = withContext(Dispatchers.IO) {
        val todos = mutableListOf<Todo>()
        val query = ds.connection.prepareStatement(TodoQuery.GET_TODO_BY_ID)
        query.setInt(1, user_id)
        val result = query.executeQuery()
        while (result.next()) {
            val userId = result.getInt("user_id")
            val id = result.getInt("id")
            val title = result.getString("title")
            val completed = result.getBoolean("completed")
            todos.add(Todo(userId, id, title, completed))
        }
        return@withContext todos
    }


    suspend fun updateTodo(id: Int, todo: Todo): Int = withContext(Dispatchers.IO) {
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

        val result = ds.connection.prepareCall(TodoQuery.UPDATE_TODO).use { stmt ->
            stmt.setInt(1, sanitizedTodo.userId)
            stmt.setInt(2, id)
            stmt.setString(3, sanitizedTodo.title)
            stmt.setBoolean(4, sanitizedTodo.completed)
            stmt.registerOutParameter(5, Types.INTEGER)
            stmt.execute()

            return@use stmt.getInt(5)
        }

        return@withContext result
    }

    suspend fun deleteTodo(id: Int): Int = withContext(Dispatchers.IO) {
        val result = ds.connection.prepareCall(TodoQuery.DELETE_TODO).use { stmt ->
            stmt.setInt(1, id)
            stmt.registerOutParameter(2, Types.INTEGER)

            stmt.execute()
            return@use stmt.getInt(2)
        }

        //Todo: add a message that indicates that the todo to be deleted does not exist
        return@withContext result
    }

    suspend fun fetchTodos(): List<Todo> = withContext(Dispatchers.IO) {
        try {
            val response: List<Todo> = client.get("https://jsonplaceholder.typicode.com/todos").body()
            if (response.isEmpty()) {
                throw kotlin.Exception("Failed to retrieve todos")
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


    suspend fun fetchTodoById(id: Int): Todo = withContext(Dispatchers.IO) {
        try {
            val request: List<Todo> = client.get("https://jsonplaceholder.typicode.com/todos").body()
            println(request)
            if (request.isEmpty()) {
                throw kotlin.Exception("Failed to retrieve todos")
            }
            return@withContext request.find { it.id == id } ?: throw kotlin.Exception("Todo with id #$id not found")
        } catch (e: Exception) {
            println("Error fetching todo by ID: ${e.message}")
            throw e
        }
    }

    suspend fun importTodoData(): Int = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://jsonplaceholder.typicode.com/todos")
            val request: String = client.get(url).body()
            if (request.isEmpty()) {
                throw kotlin.Exception("Failed to retrieve todos")
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

            var insertCount = 0
            ds.connection.use { conn ->
                conn.prepareCall(TodoQuery.INSERT_TODO).use { stmt ->
                    for (todo in sanitizedTodos) {
                        stmt.setInt(1, todo.userId)
                        stmt.setInt(2, todo.id)
                        stmt.setString(3, todo.title)
                        stmt.setBoolean(4, todo.completed)
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