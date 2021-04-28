package app.frontend

import app.model.*
import dev.fritz2.binding.*
import dev.fritz2.dom.html.Keys
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.render
import dev.fritz2.dom.key
import dev.fritz2.dom.states
import dev.fritz2.dom.values
import dev.fritz2.repositories.rest.restQuery
import dev.fritz2.routing.router
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class Filter(val text: String, val function: (List<ToDo>) -> List<ToDo>)

val filters = mapOf(
    "all" to Filter("All") { it },
    "active" to Filter("Active") { toDos -> toDos.filter { !it.completed } },
    "completed" to Filter("Completed") { toDos -> toDos.filter { it.completed } }
)

const val endpoint = "/api/todos"
val validator = ToDoValidator()
val router = router("all")

object ToDoListStore : RootStore<List<ToDo>>(emptyList(), id = "todos") {

    private val restRepo = restQuery<ToDo, Long, Unit>(ToDoResource, endpoint, -1)

    private val query = handle { restRepo.query(Unit) }

    val save = handle<ToDo> { toDos, new ->
        if (validator.isValid(new, Unit)) restRepo.addOrUpdate(toDos, new)
        else toDos
    }

    val remove = handle { toDos, id: Long ->
        restRepo.delete(toDos, id)
    }

    val toggleAll = handle { toDos, toggle: Boolean ->
        restRepo.updateMany(toDos, toDos.mapNotNull {
            if(it.completed != toggle) it.copy(completed = toggle) else null
        })
    }

    val clearCompleted = handle { toDos ->
        toDos.partition(ToDo::completed).let { (completed, uncompleted) ->
            restRepo.delete(toDos, completed.map(ToDo::id))
            uncompleted
        }
    }

    val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()
    val empty = data.map { it.isEmpty() }.distinctUntilChanged()
    val allChecked = data.map { todos -> todos.isNotEmpty() && todos.all { it.completed } }.distinctUntilChanged()

    init {
        query()
    }
}

fun RenderContext.filter(text: String, route: String) {
    li {
        a {
            className(router.data.map { if (it == route) "selected" else "" })
            href("#$route")
            +text
        }
    }
}

fun RenderContext.inputHeader() {
    header {
        h1 { +"todos" }

        validator.data.renderEach(ToDoMessage::id) {
            div("alert") {
                +it.text
            }
        }

        input("new-todo") {
            placeholder("What needs to be done?")
            autofocus(true)

            changes.values().map { domNode.value = ""; ToDo(text = it.trim()) } handledBy ToDoListStore.save
        }
    }
}

@ExperimentalCoroutinesApi
fun RenderContext.mainSection() {
    section("main") {
        input("toggle-all", id = "toggle-all") {
            type("checkbox")
            checked(ToDoListStore.allChecked)

            changes.states() handledBy ToDoListStore.toggleAll
        }
        label {
            `for`("toggle-all")
            +"Mark all as complete"
        }
        ul("todo-list") {
            ToDoListStore.data.combine(router.data) { all, route ->
                filters[route]?.function?.invoke(all) ?: all
            }.renderEach(ToDo::id) { toDo ->
                val toDoStore = storeOf(toDo)
                toDoStore.syncBy(ToDoListStore.save)
                val textStore = toDoStore.sub(L.ToDo.text)
                val completedStore = toDoStore.sub(L.ToDo.completed)

                val editingStore = storeOf(false)

                li {
                    attr("data-id", toDoStore.id)
                    classMap(toDoStore.data.combine(editingStore.data) { toDo, isEditing ->
                        mapOf(
                            "completed" to toDo.completed,
                            "editing" to isEditing
                        )
                    })
                    div("view") {
                        input("toggle") {
                            type("checkbox")
                            checked(completedStore.data)

                            changes.states() handledBy completedStore.update
                        }
                        label {
                            textStore.data.asText()

                            dblclicks.map { true } handledBy editingStore.update
                        }
                        button("destroy") {
                            clicks.events.map { toDo.id } handledBy ToDoListStore.remove
                        }
                    }
                    input("edit") {
                        value(textStore.data)
                        changes.values() handledBy textStore.update

                        editingStore.data.map { isEditing ->
                            if (isEditing) domNode.apply {
                                focus()
                                select()
                            }
                            isEditing.toString()
                        }.watch()
                        merge(
                            blurs.map { false },
                            keyups.key().filter { it == Keys.Enter }.map { false }
                        ) handledBy editingStore.update
                    }
                }
            }
        }
    }
}

fun RenderContext.appFooter() {
    footer("footer") {
        className(ToDoListStore.empty.map { if (it) "hidden" else "" })

        span("todo-count") {
            strong {
                ToDoListStore.count.map {
                    "$it item${if (it != 1) "s" else ""} left"
                }.asText()
            }
        }

        ul("filters") {
            filters.forEach { filter(it.value.text, it.key) }
        }
        button("clear-completed") {
            +"Clear completed"

            clicks handledBy ToDoListStore.clearCompleted
        }
    }
}

@ExperimentalCoroutinesApi
fun main() {
    render("#todoapp") {
        inputHeader()
        mainSection()
        appFooter()
    }
}