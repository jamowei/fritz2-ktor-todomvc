package app.model

import dev.fritz2.identification.uniqueId
import dev.fritz2.lenses.Lenses
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Lenses
data class ToDo(
    val id: String = uniqueId(),
    val text: String = "",
    val completed: Boolean = false,
    @Transient
    val editing: Boolean = false
)