package app.database

import app.model.ToDo
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ToDos : LongIdTable() {
    val text = varchar("text", 255)
    val completed = bool("completed")
    val editing = bool("editing")

    @ExperimentalStdlibApi
    fun getAll(): List<ToDo> = buildList {
        database {
            ToDos.selectAll().forEach {
                add(
                    ToDo(
                        id = it[ToDos.id].value.toString(),
                        text = it[text],
                        completed = it[completed],
                        editing = it[editing]
                    )
                )
            }
        }
    }

    fun add(toDo: ToDo): ToDo = database {
        toDo.copy(id = ToDos.insertAndGetId {
            it[text] = toDo.text
            it[completed] = toDo.completed
            it[editing] = toDo.editing
        }.value.toString())
    }

    fun replace(id: String, toDo: ToDo): ToDo = database {
        ToDos.update({ ToDos.id eq id.toLong() }) {
            it[text] = toDo.text
            it[completed] = toDo.completed
            it[editing] = toDo.editing
        }
        toDo
    }

    fun replaceAll(toDos: List<ToDo>): List<ToDo> = toDos.map {
        replace(it.id, it)
    }

    fun remove(id: String) {
        database {
            ToDos.deleteWhere {
                ToDos.id eq id.toLong()
            }
        }
    }

    fun exists(id: String): Boolean = database {
        ToDos.select(ToDos.id eq id.toLong()).count() > 0
    }
}

fun <T> database(statement: Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}