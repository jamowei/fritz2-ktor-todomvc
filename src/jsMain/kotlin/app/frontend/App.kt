package app.frontend

import app.model.*
import dev.fritz2.binding.*
import dev.fritz2.dom.append
import dev.fritz2.dom.html.Keys
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.render
import dev.fritz2.dom.key
import dev.fritz2.dom.states
import dev.fritz2.dom.values
import dev.fritz2.repositories.Resource
import dev.fritz2.repositories.rest.restEntity
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

val toDoResource = Resource(
    ToDo::id,
    ToDoSerializer,
    ToDo()
)

val router = router("all")
val query = restQuery<ToDo, Long, Unit>(toDoResource, "/api/todos")
val entity = restEntity(toDoResource, "/api/todos")
val validator = ToDoValidator()

object ToDoListStore : RootStore<List<ToDo>>(emptyList(), id = "todos") {
    val load = handle(execute = query::query)

    val add = handle<String> { toDos, text ->
        val newTodo = ToDo(text = text)
        if (validator.isValid(newTodo, Unit))
            query.addOrUpdate(toDos, newTodo)
        else toDos
    }

    val remove = handle { toDos, id: Long ->
        query.delete(toDos, id)
    }

    val toggleAll = handle { toDos, toggle: Boolean ->
        query.updateMany(toDos, toDos.mapNotNull {
            if(it.completed != toggle) it.copy(completed = toggle) else null
        })
    }

    val clearCompleted = handle { toDos ->
        toDos.partition(ToDo::completed).let { (completed, uncompleted) ->
            query.delete(toDos, completed.map(ToDo::id))
            uncompleted
        }
    }

//    val addOrUpdate = handle<ToDo> { toDos, toDo ->
//        if (validator.isValid(toDo, Unit)) query.addOrUpdate(toDos, toDo)
//        else toDos
//    }

    val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()
    val empty = data.map { it.isEmpty() }.distinctUntilChanged()
    val allChecked = data.map { todos -> todos.isNotEmpty() && todos.all { it.completed } }.distinctUntilChanged()

    init {
        load()
    }
}

class ToDoStore(toDo: ToDo): RootStore<ToDo>(toDo) {
    val validateAndUpdate = handle { toDo, newText: String ->
        val newTodo = toDo.copy(text = newText)
        if (validator.isValid(newTodo, Unit)) entity.addOrUpdate(newTodo)
        else toDo
    }
}

@ExperimentalCoroutinesApi
fun main() {

    val inputHeader = render {
        header {
            h1 { +"todos" }

            validator.msgs.renderEach(ToDoMessage::id) {
                div("alert") {
                    +it.text
                }
            }

            input("new-todo") {
                placeholder("What needs to be done?")
                autofocus(true)

                changes.values().onEach { domNode.value = "" } handledBy ToDoListStore.add
            }
        }
    }

    val mainSection = render {
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
                ToDoListStore.data.combine(router) { all, route ->
                    filters[route]?.function?.invoke(all) ?: all
                }.renderEach(ToDo::id) { toDo ->
//                    val toDoStore = toDos.sub(toDo, ToDo::id)
//                    val toDoStore = toDos.detach(toDo, ToDo::id)
                    val toDoStore = ToDoStore(toDo)

//                    toDoStore.syncBy(ToDoListStore.addOrUpdate)

                    val textStore = toDoStore.sub(L.ToDo.text)
                    val completedStore = toDoStore.sub(L.ToDo.completed)

                    val editingStore = object : RootStore<Boolean>(false) {}

                    li {
                        attr("data-id", toDoStore.id)
                        classMap(toDoStore.data.combine(editingStore.data) { toDo, editing ->
                            mapOf(
                                "completed" to toDo.completed,
                                "editing" to editing
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
                            changes.values() handledBy toDoStore.validateAndUpdate

                            editingStore.data.map { isEditing ->
                                if (isEditing) domNode.apply {
                                    focus()
                                    select()
                                }
                                isEditing.toString()
                            }.watch()
                            merge(
                                blurs.map { false },
                                keyups.key().filter { it.isKey(Keys.Enter) }.map { false }
                            ) handledBy editingStore.update
                        }
                    }
                }
            }
        }
    }

    fun RenderContext.filter(text: String, route: String) {
        li {
            a {
                className(router.map { if (it == route) "selected" else "" })
                href("#$route")
                +text
            }
        }
    }

    val appFooter = render {
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

    append("todoapp", inputHeader, mainSection, appFooter)
}