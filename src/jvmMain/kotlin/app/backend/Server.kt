package app.backend

import app.database.ToDoDB
import app.database.ToDosTable
import app.database.database
import app.model.ToDo
import app.model.ToDoValidator
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import java.io.File

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
            call.respondRedirect("/index.html", permanent = true)
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
                if (ToDoValidator.isValid(toDo, Unit)) {
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
                } else if(!ToDoValidator.isValid(newToDo, Unit)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
                } else {
                    environment.log.info("update ToDo[id=${oldToDo.id.value}] to: $newToDo")
                    call.respond(HttpStatusCode.Created, ToDoDB.update(oldToDo, newToDo.copy(id = oldToDo.id.value)))
                }
            }

            delete("/todos/{id}") {
                val oldToDo = call.parameters["id"]?.toLongOrNull()?.let { ToDoDB.find(it) }
                if(oldToDo == null) {
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
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") { main() }.start(wait = true)
}