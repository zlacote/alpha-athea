package iatheiya.ayana.transcript

import iatheiya.ayana.core.model.CommandBlock
import iatheiya.ayana.core.model.OutputBlock

/** Case-insensitive search over transcript blocks. The UI decides what to do with the hits. */
object TranscriptSearch {

    fun matchingBlockIds(blocks: List<BlockView>, query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val needle = query.lowercase()
        return blocks.mapNotNull { view ->
            val haystack = when (val block = view.block) {
                is CommandBlock -> block.text
                is OutputBlock -> block.text
            }
            view.block.id.takeIf { haystack.lowercase().contains(needle) }
        }
    }
}
