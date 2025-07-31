package com.marlow.todo.query

class TodoQuery {
    companion object {
        const val CREATE_TODOS_TABLE = "CALL create_todos_table"
        const val GET_ALL_TODOS = "SELECT user_id, id, title, completed FROM get_all_todos()"
        const val GET_TODO_BY_ID = "SELECT * FROM todos WHERE user_id = ? ORDER BY id ASC"
        const val INSERT_TODO = "INSERT INTO todos (user_id, title, completed) VALUES (?, ?, ?)"
        const val UPDATE_TODO = "UPDATE todos SET title = ?, completed = ? WHERE id = ?"
        const val DELETE_TODO = "CALL delete_todo(?, ?)"
        const val CHECK_DUPLICATE_TODO = "SELECT check_duplicate_todo(?)"
    }
}