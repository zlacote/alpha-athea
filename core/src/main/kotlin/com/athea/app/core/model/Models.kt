package com.athea.app.core.model

import com.athea.app.text.StyledText
import kotlinx.serialization.Serializable

/** Number of command lines shown in a collapsed command bubble. */
const val PREVIEW_LINES = 3

/** Stable identity for a transcript element; unique within a session. */
sealed interface Block {
    val id: String
}

/** A command submitted by the user. */
data class CommandBlock(
    override val id: String,
    val text: String,
) : Block

/**
 * Terminal output produced after a submission. [running] is true while its
 * command is still executing. [styled] carries colour spans next to the text.
 */
data class OutputBlock(
    override val id: String,
    val styled: StyledText,
    val running: Boolean = false,
    val exitCode: Int? = null,
) : Block {
    val text: String get() = styled.text
}

@Serializable
data class FavoriteCommand(
    val id: Long,
    val text: String,
)

