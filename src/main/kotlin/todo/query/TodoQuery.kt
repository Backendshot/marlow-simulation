package com.marlow.todo.query

class TodoQuery {
    companion object {
        const val CREATE_TODOS_TABLE = "CALL create_todos_table"
        const val GET_ALL_TODOS = "SELECT user_id, id, title, completed FROM get_all_todos()"
        const val GET_TODO_BY_ID = "SELECT user_id, id, title, completed FROM get_todo_by_id(?)"
        const val INSERT_TODO = "CALL insert_todo(?, ?, ?, ?)"
        const val UPDATE_TODO = "CALL update_todo(?, ?, ?, ?, ?)"
        const val DELETE_TODO = "CALL delete_todo(?, ?)"
        const val CHECK_DUPLICATE_TODO = "SELECT check_duplicate_todo(?)"
        const val GET_BEARER_TOKEN = "SELECT bearer_token FROM bearers"
    }
}