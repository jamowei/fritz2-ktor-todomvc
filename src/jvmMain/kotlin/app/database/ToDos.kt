package app.database

import app.model.ToDo
import app.model.ToDoValidator
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object ToDosTable : LongIdTable() {
    val text = varchar("text", ToDoValidator.maxTextLength)
    val completed = bool("completed")
}

class ToDoEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ToDoEntity>(ToDosTable)

    var text by ToDosTable.text
    var completed by ToDosTable.completed

    fun toToDo() = ToDo(this.id.value, this.text, this.completed)
}

object ToDoDB {

    fun find(id: Long): ToDoEntity? = database {
        ToDoEntity.findById(id)
    }

    fun all(): List<ToDo> = database {
        ToDoEntity.all().map { it.toToDo() }
    }

    fun add(toDo: ToDo): ToDo = database {
        ToDoEntity.new {
            text = toDo.text
            completed = toDo.completed
        }.toToDo()
    }

    fun update(old: ToDoEntity, new: ToDo): ToDo = database {
        old.text = new.text
        old.completed = new.completed
        new
    }

    fun remove(toDelete: ToDoEntity) = database {
        toDelete.delete()
    }
}

fun <T> database(statement: Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}