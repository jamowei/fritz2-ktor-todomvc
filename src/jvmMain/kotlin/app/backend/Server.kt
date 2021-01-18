package app.backend

import app.database.ToDoDB
import app.database.ToDosTable
import app.database.database
import app.model.ToDo
import app.model.ToDoValidator
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import java.io.File

val validator = ToDoValidator()

fun Application.main() {
    val currentDir = File(".").absoluteFile
    environment.log.info("Current directory: $currentDir")

    install(ContentNegotiation) {
        json()
    }

    Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")

    database {
        SchemaUtils.create(ToDosTable)
    }

    routing {
        get("/") {
            call.resolveResource("index.html")?.let {
                call.respond(it)
            }
        }

        static("/") {
            resources("/")
        }

        route("/api") {

            get("/todos") {
                environment.log.info("getting all ToDos")
                call.respond(ToDoDB.all())
            }

            post("/todos") {
                val toDo = call.receive<ToDo>()
                if (validator.isValid(toDo, Unit)) {
                    environment.log.info("save new ToDo: $toDo")
                    call.respond(HttpStatusCode.Created, ToDoDB.add(toDo))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
                }
            }

            put("/todos/{id}") {
                val oldToDo = call.parameters["id"]?.toLongOrNull()?.let { ToDoDB.find(it) }
                val newToDo = call.receive<ToDo>()
                if (oldToDo == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                } else if (!validator.isValid(newToDo, Unit)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
                } else {
                    environment.log.info("update ToDo[id=${oldToDo.id.value}] to: $newToDo")
                    call.respond(HttpStatusCode.Created, ToDoDB.update(oldToDo, newToDo.copy(id = oldToDo.id.value)))
                }
            }

            delete("/todos/{id}") {
                val oldToDo = call.parameters["id"]?.toLongOrNull()?.let { ToDoDB.find(it) }
                if (oldToDo == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                } else {
                    environment.log.info("remove ToDo with id: ${oldToDo.id.value}")
                    call.respond(HttpStatusCode.OK, ToDoDB.remove(oldToDo))
                }
            }
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port) {
        main()
    }.start(wait = true)
}