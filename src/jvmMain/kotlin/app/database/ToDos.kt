package app.database

import app.model.ToDo
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ToDos : LongIdTable() {
    val text = varchar("text", 255)
    val completed = bool("completed")
}

@ExperimentalStdlibApi
fun ToDos.getAll(): List<ToDo> = buildList {
    database {
        selectAll().forEach {
            add(
                ToDo(
                    id = it[ToDos.id].value.toString(),
                    text = it[text],
                    completed = it[completed]
                )
            )
        }
    }
}

fun ToDos.add(toDo: ToDo): ToDo = database {
    toDo.copy(id = insertAndGetId {
        it[text] = toDo.text
        it[completed] = toDo.completed
    }.value.toString())
}

fun ToDos.replace(id: String, toDo: ToDo): ToDo = database {
    update({ ToDos.id eq id.toLong() }) {
        it[text] = toDo.text
        it[completed] = toDo.completed
    }
    toDo
}

fun ToDos.replaceAll(toDos: List<ToDo>): List<ToDo> = toDos.map {
    replace(it.id, it)
}

fun ToDos.remove(id: String) {
    database {
        deleteWhere {
            ToDos.id eq id.toLong()
        }
    }
}

fun ToDos.exists(id: String): Boolean = database {
    select(ToDos.id eq id.toLong()).count() > 0
}

fun <T> database(statement: Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}