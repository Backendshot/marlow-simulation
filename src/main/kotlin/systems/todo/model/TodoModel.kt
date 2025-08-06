package com.marlow.systems.todo.model

import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val userId: Int,
    val id: Int? = null,
    val title: String,
    val completed: Boolean,
    val isDeleted: Boolean = false
)

class TodoValidator {

    val minimumUserId = 1;

    fun sanitize(todo: Todo): Todo {
        return todo.copy(
            title = todo.title.trim()
        )
    }

    fun validate(todo: Todo): List<String> {
        return buildList {
            if (todo.title.isBlank()) add("Title cannot be blank.")
            if (todo.userId < minimumUserId) add("User ID must be equal or greater than 1.")
        }
    }
}
