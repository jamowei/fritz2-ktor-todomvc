package app.frontend

import app.model.L
import app.model.ToDo
import app.model.ToDoMessage
import app.model.ToDoValidator
import dev.fritz2.binding.*
import dev.fritz2.dom.append
import dev.fritz2.dom.html.HtmlElements
import dev.fritz2.dom.html.Keys
import dev.fritz2.dom.html.render
import dev.fritz2.dom.key
import dev.fritz2.dom.states
import dev.fritz2.dom.values
import dev.fritz2.remote.getBody
import dev.fritz2.remote.remote
import dev.fritz2.routing.router
import dev.fritz2.validation.Validation
import dev.fritz2.validation.Validator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

data class Filter(val text: String, val function: (List<ToDo>) -> List<ToDo>)

val filters = mapOf(
    "/" to Filter("All") { it },
    "/active" to Filter("Active") { toDos -> toDos.filter { !it.completed } },
    "/completed" to Filter("Completed") { toDos -> toDos.filter { it.completed } }
)

@UnstableDefault
@ExperimentalTime
@ExperimentalCoroutinesApi
@FlowPreview
fun main() {
    val router = router("/")
    val api = remote("/api/todos")
    val serializer = ToDo.serializer()

    val toDos = object : RootStore<List<ToDo>>(emptyList(), dropInitialData = true, id = "todos"),
        Validation<ToDo, ToDoMessage, Unit> {

        override val validator: Validator<ToDo, ToDoMessage, Unit> = ToDoValidator

        val load = handle {
            runCatching {
                Json.parse(serializer.list, api.get().getBody())
            }.getOrDefault(emptyList())
        }

        val add = handle<String> { toDos, text ->
            val newTodo = ToDo(text = text)
            if (validate(newTodo, Unit)) {
                runCatching {
                    toDos + Json.parse(
                        serializer, api.contentType("application/json")
                            .body(Json.stringify(serializer, newTodo))
                            .post()
                            .getBody()
                    )
                }.getOrDefault(toDos)
            } else toDos
        }

        val remove = handle<Long> { toDos, id ->
            runCatching {
                api.delete(id.toString())
            }
            toDos.filterNot { it.id == id }
        }

        val toggleAll = handle<Boolean> { toDos, toggle ->
            toDos.map {
                val toDo = it.copy(completed = toggle)
                runCatching {
                    api.contentType("application/json")
                        .body(Json.stringify(serializer, toDo))
                        .put(toDo.id.toString())
                }
                toDo
            }
        }

        val clearCompleted = handle { toDos ->
            runCatching {
                toDos.filter { it.completed }.forEach {
                    api.delete(it.id.toString())
                }
            }
            toDos.filterNot { it.completed }
        }

        val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()
        val allChecked = data.map { todos -> todos.isNotEmpty() && todos.all { it.completed } }.distinctUntilChanged()

        init {
            action() handledBy load
        }
    }

    val inputHeader = render {
        header {
            h1 { +"todos" }

            toDos.validator.msgs.each(ToDoMessage::id).map {
                render {
                    div("alert") {
                        +it.text
                    }
                }
            }.bind()

            input("new-todo") {
                placeholder = const("What needs to be done?")
                autofocus = const(true)

                changes.values().onEach { domNode.value = "" } handledBy toDos.add
            }
        }
    }

    val mainSection = render {
        section("main") {
            input("toggle-all", id = "toggle-all") {
                type = const("checkbox")
                checked = toDos.allChecked

                changes.states() handledBy toDos.toggleAll
            }
            label(`for` = "toggle-all") {
                text("Mark all as complete")
            }
            ul("todo-list") {
                toDos.data.flatMapLatest { all ->
                    router.routes.map { route ->
                        filters[route]?.function?.invoke(all) ?: all
                    }
                }.each(ToDo::id).map { toDo ->
                    val toDoStore = toDos.sub(toDo, ToDo::id)

                    toDoStore.data.drop(1) handledBy toDoStore.handle { _, changedToDo ->
                        runCatching {
                            api.contentType("application/json")
                                .body(Json.stringify(serializer, changedToDo))
                                .put(changedToDo.id.toString())
                        }
                        changedToDo
                    }

                    val textStore = toDoStore.sub(L.ToDo.text)
                    val completedStore = toDoStore.sub(L.ToDo.completed)
                    val editingStore = toDoStore.sub(L.ToDo.editing)

                    render {
                        li {
                            attr("data-id", toDoStore.id)
                            //TODO: better flatmap over editing and completed
                            classMap = toDoStore.data.map {
                                mapOf(
                                    "completed" to it.completed,
                                    "editing" to it.editing
                                )
                            }
                            div("view") {
                                input("toggle") {
                                    type = const("checkbox")
                                    checked = completedStore.data

                                    changes.states() handledBy completedStore.update
                                }
                                label {
                                    textStore.data.bind()

                                    dblclicks.map { true } handledBy editingStore.update
                                }
                                button("destroy") {
                                    clicks.events.map { toDo.id } handledBy toDos.remove
                                }
                            }
                            input("edit") {
                                value = textStore.data
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
                                    keyups.key().filter { it.isKey(Keys.Enter) }.map { false }
                                ) handledBy editingStore.update
                            }
                        }
                    }
                }.bind()
            }
        }
    }

    fun HtmlElements.filter(text: String, route: String) {
        li {
            a {
                className = router.routes.map { if (it == route) "selected" else "" }
                href = const("#$route")
                text(text)
            }
        }
    }

    val appFooter = render {
        footer("footer") {
            span("todo-count") {
                strong {
                    toDos.count.map {
                        "$it item${if (it != 1) "s" else ""} left"
                    }.bind()
                }
            }

            ul("filters") {
                filters.forEach { filter(it.value.text, it.key) }
            }
            button("clear-completed") {
                text("Clear completed")

                clicks handledBy toDos.clearCompleted
            }
        }
    }

    append("todoapp", inputHeader, mainSection, appFooter)
}