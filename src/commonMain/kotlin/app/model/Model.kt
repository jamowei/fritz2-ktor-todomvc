package app.model

import dev.fritz2.identification.uniqueId
import dev.fritz2.lenses.Lenses
import kotlinx.serialization.Serializable

@Serializable
@Lenses
data class ToDo(
    val text: String,
    val id: String = uniqueId(),
    val completed: Boolean = false,
    val editing: Boolean = false
)