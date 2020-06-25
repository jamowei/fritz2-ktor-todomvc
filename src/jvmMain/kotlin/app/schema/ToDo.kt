package app.schema

import app.model.ToDo
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object ToDos : IntIdTable() {
    val text = varchar("text", 255)
    val completed = bool("completed")
    val editing = bool("editing")
}

fun ToDos.getAll(): List<ToDo> {
    val todos: ArrayList<ToDo> = arrayListOf()
    database {
        ToDos.selectAll().map {
            todos.add(
                ToDo(
                    text = it[text],
                    completed = it[completed],
                    editing = it[editing]
                )
            )
        }
    }
    return todos
}

fun ToDos.add(toDo: ToDo) {
    database {
        ToDos.insert {
            it[text] = toDo.text
            it[completed] = toDo.completed
            it[editing] = toDo.editing
        }
    }
}

fun <T> database(statement: Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}