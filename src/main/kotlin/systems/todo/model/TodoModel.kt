package com.marlow.systems.todo.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Todo(
    val userId: Int,
    val id: Int? = null,
    val title: String,
    val completed: Boolean
)

class TodoValidator {

    fun sanitize(todo: Todo): Todo {
        return todo.copy(
            title = todo.title.trim()
        )
    }

    fun validate(todo: Todo): List<String> {
        return buildList {
            if (todo.title.isBlank()) add("Title cannot be blank.")
            if (todo.userId <= 0) add("User ID must be greater than 0.")
        }
    }
}
