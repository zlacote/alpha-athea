package iatheiya.ayana.data

import kotlinx.serialization.Serializable

/** Action a key-row button performs. */
enum class KeyKind { INSERT, SEND, CTRL, ALT }

/** One user-configurable button of the key row. */
@Serializable
data class CustomKey(
    val label: String,
    val payload: String,
    val kind: KeyKind = KeyKind.SEND,
    /** A sticky key latches when pressed and prefixes its payload onto the next key, like a modifier. */
    val sticky: Boolean = false,
)

@Serializable
data class KeysIndex(
    val items: List<CustomKey> = emptyList(),
)
