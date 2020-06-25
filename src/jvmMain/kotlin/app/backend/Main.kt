package app.backend

import app.model.ToDo
import app.schema.ToDos
import app.schema.add
import app.schema.database
import app.schema.getAll
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
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.batchInsert
import java.io.File

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

    launch {
        val todos = listOf(ToDo("build good programms"), ToDo("testing"))

        database {
            ToDos.batchInsert(todos) {
                this[ToDos.text] = it.text
                this[ToDos.completed] = it.completed
                this[ToDos.editing] = it.editing
            }
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
                val todos = ToDos.getAll()
                call.respond(todos)
            }
            post("/todos") {
                ToDos.add(call.receive())
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") { main() }.start(wait = true)
}