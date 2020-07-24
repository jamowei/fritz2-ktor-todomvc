package app.backend

import app.database.ToDoDB
import app.database.ToDoEntity
import app.database.ToDosTable
import app.database.database
import app.model.ToDo
import app.model.ToDoValidator
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
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
        SchemaUtils.create(ToDosTable)
    }

    suspend fun doWhenValid(call: ApplicationCall, toDo: ToDo, run: (ToDo) -> ToDo) {
        if (ToDoValidator.isValid(toDo, Unit)) {
            call.respond(HttpStatusCode.Created, run(toDo))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
        }
    }

    suspend fun doWhenExists(call: ApplicationCall, run: (ToDoEntity) -> Unit) {
        val id = call.parameters["id"]?.toLongOrNull()
        val toDo = ToDoDB.single(id)
        if (toDo != null) {
            run(toDo)
            call.respond(HttpStatusCode.Created)
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
        }
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
                doWhenValid(call, toDo) {
                    environment.log.info("save new ToDo: $toDo")
                    ToDoDB.add(toDo)
                }
            }
            put("/todos/{id}") {
                val toDo = call.receive<ToDo>()
                doWhenExists(call) {
                    environment.log.info("update ToDo with id: $it")
                    ToDoDB.update(it, toDo)
                }
            }
            delete("/todos/{id}") {
                doWhenExists(call) {
                    environment.log.info("remove ToDo with id: $it")
                    ToDoDB.remove(it)
                }
            }
        }
    }
}

@ExperimentalStdlibApi
fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") { main() }.start(wait = true)
}