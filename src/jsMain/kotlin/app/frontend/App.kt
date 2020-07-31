package app.frontend

import app.model.*
import dev.fritz2.binding.*
import dev.fritz2.dom.append
import dev.fritz2.dom.html.HtmlElements
import dev.fritz2.dom.html.Keys
import dev.fritz2.dom.html.render
import dev.fritz2.dom.key
import dev.fritz2.dom.states
import dev.fritz2.dom.values
import dev.fritz2.routing.router
import dev.fritz2.services.rest.RestEntityService
import dev.fritz2.services.rest.RestQueryService
import dev.fritz2.services.rest.RestResource
import dev.fritz2.validation.Validation
import dev.fritz2.validation.Validator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.UnstableDefault
import kotlin.time.ExperimentalTime

data class Filter(val text: String, val function: (List<ToDo>) -> List<ToDo>)

val filters = mapOf(
    "/" to Filter("All") { it },
    "/active" to Filter("Active") { toDos -> toDos.filter { !it.completed } },
    "/completed" to Filter("Completed") { toDos -> toDos.filter { it.completed } }
)


val toDoResource = RestResource(
    "/api/todos",
    ToDo::id,
    ToDoSerializer,
    ToDo()
)

@UnstableDefault
@ExperimentalTime
@ExperimentalCoroutinesApi
@FlowPreview
fun main() {

    val router = router("/")
//    val api = remote("/api/todos")
//    val serializer = ToDo.serializer()

    val restEntity = RestEntityService<ToDo, Long>(toDoResource)
    val restQuery = RestQueryService<ToDo, Long, Unit>(toDoResource)

    val toDos = object : RootStore<List<ToDo>>(emptyList(), dropInitialData = true, id = "todos"),
        Validation<ToDo, ToDoMessage, Unit> {

        override val validator: Validator<ToDo, ToDoMessage, Unit> = ToDoValidator

//        val load = handle<Unit> { toDos, query ->
//            restQuery.query(toDos, query)
//        }

        val load = handle<Unit>(execute = restQuery::query)


        val add = handleAndOffer<String, Unit> { toDos, text ->
//            restQuery.saveAll(toDos + ToDo(text = text)) funktioniert so nicht
            toDos + restEntity.saveOrUpdate(this, ToDo(text = text))
        }

        val remove = handle<Long> { toDos, id ->
            restQuery.delete(toDos, id)
        }

        val toggleAll = handleAndOffer<Boolean, Unit> { toDos, toggle ->
            toDos.map {
                val toDo = it.copy(completed = toggle)
                restEntity.saveOrUpdate(this, toDo)
            }
        }

        //FIXME: nicht optimal
        val clearCompleted = handle { toDos ->
            val ids = toDos.filter { it.completed }.map { it.id }.toTypedArray()
            restQuery.delete(toDos, *ids)
        }

        val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()
        val allChecked = data.map { todos -> todos.isNotEmpty() && todos.all { it.completed } }.distinctUntilChanged()

        init {
            action() handledBy load
            //FIXME: syncBy mit type parameter
//            syncBy(load)
        }
    }

    val inputHeader = render {
        header {
            h1 { +"todos" }

            toDos.validator.msgs.each(ToDoMessage::id).render {
                div("alert") {
                    +it.text
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
                    // FIXME: nicht wirklich schön, besser syncBy
                    toDoStore.data.drop(1) handledBy toDoStore.handleAndOffer<ToDo, Unit> { _, newToDo ->
                        restEntity.saveOrUpdate(this, newToDo)
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