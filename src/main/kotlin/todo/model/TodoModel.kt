package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Todo(
//    @Serializable(with = StrictIntSerializer::class)
    val userId: Int,
//    @Serializable(with = StrictIntSerializer::class)
    val id: Int,
    val title: String,
//    @Serializable(with = StrictBooleanSerializer::class)
    val completed: Boolean
)

class TodoValidator {

    // Sanitize input data by trimming whitespace from title
    fun sanitize(todo: Todo): Todo {
        return todo.copy(
            title = todo.title.trim()
        )
    }

    fun <T> validate(todo: T): List<String> {
        val errors = mutableListOf<String>()
        when (todo) {
            is Todo -> {
                if (todo.title.trim().isBlank()) {
                    errors.add("Title cannot be blank.")
                }

                if (todo.userId <= 0) {
                    errors.add("User ID must be greater than 0.")
                }

                if (todo.id <= 0) {
                    errors.add("ID must be greater than 0.")
                }
            }

            is JsonObject -> {
                if (todo["id"]?.jsonPrimitive?.isString == true || todo["id"]?.jsonPrimitive?.isString == null) {
                    errors.add("id must be a JSON number")
                }

                if (todo["userId"]?.jsonPrimitive?.isString == true || todo["userId"]?.jsonPrimitive?.isString == null) {
                    errors.add("userId must be a JSON number")
                }

                if (todo["completed"]?.jsonPrimitive?.isString == true || todo["completed"]?.jsonPrimitive?.isString == null) {
                    errors.add("completed must be a JSON boolean")
                }
            }
        }
        return errors
    }

//    fun validateJson(todo: JsonObject): List<String> {
//        val errors = mutableListOf<String>()
//
//        if (todo["id"]?.jsonPrimitive?.isString == true || todo["id"]?.jsonPrimitive?.isString == null) {
//            errors.add("id must be a JSON number")
//        }
//
//        if (todo["userId"]?.jsonPrimitive?.isString == true || todo["userId"]?.jsonPrimitive?.isString == null) {
//            errors.add("userId must be a JSON number")
//        }
//
//        if (todo["completed"]?.jsonPrimitive?.isString == true || todo["completed"]?.jsonPrimitive?.isString == null) {
//            errors.add("completed must be a JSON boolean")
//        }
//
//        return errors;
//    }

    // Validate input data and return a list of errors
//    fun validate(todo: Todo): List<String> {
//        val errors = mutableListOf<String>()
//
//        if (todo.title.trim().isBlank()) {
//            errors.add("Title cannot be blank.")
//        }
//
//        if (todo.userId <= 0) {
//            errors.add("User ID must be greater than 0.")
//        }
//
//        checkInput(todo.userId)
//
//        if (todo.id <= 0) {
//            errors.add("ID must be greater than 0.")
//        }
//
//        checkInput(todo.id)
//
//        return errors
//    }

//    fun checkInput(input: Any) {
//        if (input is String) {
//            throw Exception("Invalid Input. UserID must be Integer")
//        }
//    }
}
