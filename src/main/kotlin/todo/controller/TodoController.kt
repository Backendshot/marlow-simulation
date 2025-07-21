package com.marlow.todo.controller

import com.api.client
import com.api.query.TodoQuery
import config.TodoConfig
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import model.Todo
import model.TodoValidator
import java.net.URL
import java.sql.Types
import kotlin.collections.find
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.text.isEmpty
import kotlin.use

class TodoController {

    val connection = TodoConfig().connect()

    suspend fun createTodo(todo: Todo): Int = withContext(Dispatchers.IO) {
        val duplicateTodo = connection.prepareCall(TodoQuery.CHECK_DUPLICATE_TODO)
        duplicateTodo.setInt(1, todo.id)
        val resultDuplicate = duplicateTodo.executeQuery()
        resultDuplicate.next()
        if (resultDuplicate.getBoolean(1)) {
            throw kotlin.Exception("Todo at id #${todo.id} already exists")
        }

        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todo)
        val validationErrors = validator.validate(sanitizedTodo)

        if (validationErrors.isEmpty()) {
            println("Sanitized and Validated Todo: $sanitizedTodo")
        } else {
            throw kotlin.Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        val result = connection.prepareCall(TodoQuery.INSERT_TODO).use { stmt ->
            stmt.setInt(1, sanitizedTodo.userId)
            stmt.setInt(2, sanitizedTodo.id)
            stmt.setString(3, sanitizedTodo.title)
            stmt.setBoolean(4, sanitizedTodo.completed)
            stmt.registerOutParameter(5, Types.INTEGER)
            stmt.execute()

            return@use stmt.getInt(5)
        }

        //TODO: How do I return both the id (todo.id) and the amount of rows inputted (result)? Is it necessary to do in the first place?
        return@withContext todo.id;
    }

    suspend fun readAllTodos(): MutableList<Todo> = withContext(Dispatchers.IO) {
        val data = mutableListOf<Todo>()
        val query = connection.prepareStatement(TodoQuery.GET_ALL_TODOS)
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

    suspend fun readTodoById(id: Int): Todo = withContext(Dispatchers.IO) {
        val query = connection.prepareStatement(TodoQuery.GET_TODO_BY_ID)
        query.setInt(1, id)
        val result = query.executeQuery()
        if (result.next()) {
            val userId: Int = result.getInt("user_id")
            val id: Int = result.getInt("id")
            val title = result.getString("title")
            val completed = result.getBoolean("completed")
            return@withContext Todo(userId, id, title, completed)
        } else {
            throw kotlin.Exception("Record not found")
        }

    }

    suspend fun updateTodo(id: Int, todo: Todo): Int = withContext(Dispatchers.IO) {
        if (id != todo.id) {
            throw kotlin.Exception("id cannot be updated")
        }
        val validator = TodoValidator()
        val sanitizedTodo = validator.sanitize(todo)
        val validationErrors = validator.validate(sanitizedTodo)

        if (validationErrors.isEmpty()) {
            println("Sanitized and Validated Todo: $sanitizedTodo")
        } else {
            throw kotlin.Exception("Validation Errors: ${validationErrors.joinToString(", ")}")
        }

        val result = connection.prepareCall(TodoQuery.UPDATE_TODO).use { stmt ->
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
        val result = connection.prepareCall(TodoQuery.DELETE_TODO).use { stmt ->
            stmt.setInt(1, id)
            stmt.registerOutParameter(2, Types.INTEGER)

            stmt.execute()
            return@use stmt.getInt(2)
        }

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
            connection.use { conn ->
                val statement = conn.prepareStatement(TodoQuery.INSERT_TODO)
                for (todo in sanitizedTodos) {
                    statement.setInt(1, todo.userId)
                    statement.setInt(2, todo.id)
                    statement.setString(3, todo.title)
                    statement.setBoolean(4, todo.completed)
                    statement.addBatch()
                }
                val result = statement.executeBatch()
                insertCount = result.size
            }
            return@withContext insertCount
        } catch (e: Exception) {
            println("Error importing todo data: ${e.message}")
            throw e
        }
    }

}