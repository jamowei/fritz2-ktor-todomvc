package app.model

import dev.fritz2.identification.inspect
import dev.fritz2.lenses.Lenses
import dev.fritz2.serialization.Serializer
import dev.fritz2.validation.ValidationMessage
import dev.fritz2.validation.Validator
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Lenses
@Serializable
data class ToDo(
    val id: Long = -1,
    val text: String = "",
    val completed: Boolean = false
)

data class ToDoMessage(val id: String, val text: String) : ValidationMessage {
    override fun isError(): Boolean = true
}

class ToDoValidator : Validator<ToDo, ToDoMessage, Unit>() {

    val maxTextLength = 50

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

object ToDoSerializer : Serializer<ToDo, String> {
    override fun read(msg: String): ToDo = Json.decodeFromString(ToDo.serializer(), msg)

    override fun readList(msg: String): List<ToDo> = Json.decodeFromString(ListSerializer(ToDo.serializer()), msg)

    override fun write(item: ToDo): String = Json.encodeToString(ToDo.serializer(), item)

    override fun writeList(items: List<ToDo>): String = Json.encodeToString(ListSerializer(ToDo.serializer()), items)
}