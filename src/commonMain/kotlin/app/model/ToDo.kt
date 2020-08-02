package app.model

import dev.fritz2.identification.inspect
import dev.fritz2.lenses.Lenses
import dev.fritz2.serialization.Serializer
import dev.fritz2.validation.ValidationMessage
import dev.fritz2.validation.Validator
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

@Lenses
@Serializable
data class ToDo(
    val id: Long = -1,
    val text: String = "",
    val completed: Boolean = false
)

data class ToDoMessage(val id: String, val text: String) : ValidationMessage {
    override fun failed(): Boolean = true
}

object ToDoValidator : Validator<ToDo, ToDoMessage, Unit>() {

    const val maxTextLength = 50

    override fun validate(data: ToDo, metadata: Unit): List<ToDoMessage> {
        val msgs = mutableListOf<ToDoMessage>()
        val inspector = inspect(data, "todos")

        val textInspector = inspector.sub(L.ToDo.text)

        if (textInspector.data.startsWith(" ")) msgs.add(
            ToDoMessage(
                textInspector.id,
                "Text should not start with space."
            )
        )
        if (textInspector.data.trim().length < 3) msgs.add(
            ToDoMessage(
                textInspector.id,
                "Text length must be at least 3 characters."
            )
        )
        if (textInspector.data.length > maxTextLength) msgs.add(
            ToDoMessage(
                textInspector.id,
                "Text length is to long."
            )
        )

        return msgs
    }
}

@UnstableDefault
object ToDoSerializer: Serializer<ToDo, String> {
    override fun read(msg: String): ToDo = Json.parse(ToDo.serializer(), msg)

    override fun readList(msg: String): List<ToDo> = Json.parse(ToDo.serializer().list, msg)

    override fun write(item: ToDo): String = Json.stringify(ToDo.serializer(), item)

    override fun writeList(items: List<ToDo>): String = Json.stringify(ToDo.serializer().list, items)

}