package com.marlow.todo.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Todo(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean
)

class TodoValidator {

    // Sanitize input data by trimming whitespace from title
    fun sanitize(todo: Todo): Todo {
        return todo.copy(
            title = todo.title.trim()
        )
    }

    /**
     * Validates a Todo domain object.
     */
    fun validate(todo: Todo): List<String> {
        return buildList {
            if (todo.title.isBlank()) add("Title cannot be blank.")
            if (todo.userId <= 0) add("User ID must be greater than 0.")
            if (todo.id <= 0) add("ID must be greater than 0.")
        }
    }

    /**
     * Validates a JSON representation of a Todo.
     */
    fun validate(json: JsonObject): List<String> {
        return buildList {
            json.ensureNumber("id")?.let { add(it) }
            json.ensureNumber("userId")?.let { add(it) }
            json.ensureBoolean("completed")?.let { add(it) }
        }
    }

    /**
     * Helper to check that a JSON property is a number.
     * @return error message if invalid, null otherwise
     */
    private fun JsonObject.ensureNumber(key: String): String? {
        val element = this[key]?.jsonPrimitive
            ?: return "$key is required and must be a JSON number"
        return if (element.isString) {
            "$key must be a JSON number"
        } else null
    }

    /**
     * Helper to check that a JSON property is a boolean.
     * @return error message if invalid, null otherwise
     */
    private fun JsonObject.ensureBoolean(key: String): String? {
        val element = this[key]?.jsonPrimitive
            ?: return "$key is required and must be a JSON boolean"
        return if (element.isString) {
            "$key must be a JSON boolean"
        } else null
    }
}
