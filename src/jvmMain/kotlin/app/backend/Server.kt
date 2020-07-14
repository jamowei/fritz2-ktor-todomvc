package app.backend

import app.database.ToDos
import app.database.database
import app.model.ToDo
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

@ExperimentalStdlibApi
fun Application.main() {
    val currentDir = File(".").absoluteFile
    environment.log.info("Current directory: $currentDir")

    install(ContentNegotiation) {
        json()
    }

    Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")

    database {
        SchemaUtils.create(ToDos)
    }

//    launch {
//        val todos = listOf(ToDo(text = "build good programs"), ToDo(text = "testing"))
//
//        database {
//            ToDos.batchInsert(todos) {
//                this[ToDos.text] = it.text
//                this[ToDos.completed] = it.completed
//                this[ToDos.editing] = it.editing
//            }
//        }
//    }

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
                call.respond(ToDos.getAll())
            }
            post("/todos") {
                val toDo = call.receive<ToDo>()
                environment.log.info("save new ToDo: $toDo")
                call.respond(HttpStatusCode.Created, ToDos.add(toDo))
            }
            put("/todos/{id}") {
                val id = call.parameters["id"]
                val toDo = call.receive<ToDo>()
                if (id != null && ToDos.exists(id)) {
                    environment.log.info("replace ToDo with id: $id")
                    call.respond(HttpStatusCode.Created, ToDos.replace(id, toDo))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                }
            }
            delete("/todos/{id}") {
                val id = call.parameters["id"]
                if(id != null && ToDos.exists(id)) {
                    environment.log.info("remove ToDo with id: $id")
                    ToDos.remove(id)
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                }
            }
        }
    }
}

@ExperimentalStdlibApi
fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") { main() }.start(wait = true)
}