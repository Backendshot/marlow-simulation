package com.marlow.todo.query

class TodoQuery {
    companion object {
        const val CREATE_TODOS_TABLE = "CALL create_todos_table"
        // "CREATE TABLE IF NOT EXISTS TODOS (ID INT PRIMARY KEY, USERID INT, TITLE VARCHAR(255),
        // COMPLETED BOOLEAN)" //formerly CREATE_TABLE_TODOS
        const val GET_ALL_TODOS =
                "SELECT user_id, id, title, completed FROM get_all_todos()" // "SELECT * FROM todos"
        const val GET_TODO_BY_ID = "SELECT * FROM todos WHERE user_id = ?"
        const val INSERT_TODO =
                "CALL insert_todo(?, ?, ?, ?)" // "INSERT INTO todos (user_id, id, title, completed)
        // VALUES (?, ?, ?, ?)"
        const val UPDATE_TODO =
                "CALL update_todo(?, ?, ?, ?, ?)" // "UPDATE todos SET user_id = ?, title = ?,
        // completed = ? WHERE id = ?"
        const val DELETE_TODO = "CALL delete_todo(?, ?)" // "DELETE FROM todos WHERE id = ?"
        const val CHECK_DUPLICATE_TODO = "SELECT check_duplicate_todo(?)"
        //        const val SELECT_DUPLI_ID = "CALL select_dupli_id(?)"
        // SELECT EXISTS(SELECT 1 FROM todos WHERE id=?)
        // "SELECT column_name(s) FROM todos WHERE EXISTS ( SELECT 1 FROM another_table WHERE id = ?
        // );
        // "SELECT id FROM todos WHERE id = ?"
        const val GET_BEARER_TOKEN =
                "SELECT get_bearer_token() AS bearer_token" // "SELECT bearer_token FROM bearers"
        // //formerly SELECT_BEARER_TOKEN
    }
}
