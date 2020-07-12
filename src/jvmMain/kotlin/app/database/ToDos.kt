package app.database

import app.model.ToDo
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object ToDos : LongIdTable() {
    val text = varchar("text", 255)
    val completed = bool("completed")
    val editing = bool("editing")

    @ExperimentalStdlibApi
    fun getAll(): List<ToDo> = buildList {
        database {
            ToDos.slice(ToDos.id, text, completed, editing).selectAll().forEach {
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

    fun remove(id: String) {
        database {
            ToDos.deleteWhere {
                ToDos.id.eq(id.toLong())
            }
        }
    }
}

fun <T> database(statement: Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}