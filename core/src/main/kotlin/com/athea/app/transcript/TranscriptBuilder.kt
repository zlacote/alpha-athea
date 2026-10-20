package com.athea.app.transcript

import com.athea.app.core.journal.JournalEvent
import com.athea.app.core.model.Block
import com.athea.app.core.model.CommandBlock
import com.athea.app.core.model.OutputBlock
import com.athea.app.core.model.PREVIEW_LINES
import com.athea.app.parse.StreamParser
import com.athea.app.parse.applyTo
import com.athea.app.text.StyledText
import com.athea.app.text.StyledTextBuilder
import com.athea.app.util.AtheaLog

/** A block plus its presentation-time collapse state. */
data class BlockView(
    val block: Block,
    val collapsed: Boolean,
)

/** Immutable view of what the transcript currently looks like. */
data class TranscriptSnapshot(
    val blocks: List<BlockView>,
    val running: Boolean,
    val rawText: String,
)

/**
 * Builds the block projection and the raw-stream projection from journal
 * events. Pure Kotlin and deterministic: replaying the same events always
 * produces the same transcript, which is how sessions survive restarts.
 *
 * Collapse rules (the manual override always wins):
 *  - a command bubble collapses when it exceeds [previewLines] lines;
 *  - an output block stays open while it runs or is the newest output;
 *    older outputs auto-collapse when the next command is submitted.
 */
class TranscriptBuilder(
    private val rawCapChars: Int = DEFAULT_RAW_CAP,
    previewLines: Int = PREVIEW_LINES,
) {

    private val blocks = ArrayList<Block>()
    private val expandedOverrides = HashMap<String, Boolean>()
    private var runningOutputId: String? = null
    private var runningOutputIndex: Int = -1
    private val runningText = StyledTextBuilder()
    private var outputCounter = 0

    // Raw projection kept as chunks: trimming drops whole head chunks in
    // O(1) instead of copying the surviving megabyte on every overflow.
    private val rawChunks = ArrayDeque<String>()
    private var rawSize = 0

    /** Lines shown in a collapsed command bubble; live-tunable in settings. */
    var previewLines: Int = previewLines
        private set

    fun applyPreviewLines(lines: Int) {
        previewLines = lines.coerceIn(1, 10)
    }

    // ---------------------------------------------------------------- input

    fun applyCommandSubmitted(seq: Long, text: String) {
        // A still-running output block is deliberately NOT closed here: the
        // shell buffers the typed bytes and the running command's OSC 133;D
        // still arrives later. Closing eagerly would drop its exit code and
        // open a new block below the newer bubble.
        if (expandedOverrides.isNotEmpty()) {
            expandedOverrides.keys.removeIf { it.startsWith("out-") }
        }
        blocks.add(CommandBlock(id = cmdId(seq), text = text))
        appendRaw("$text\n")
    }

    fun applyOutput(styled: StyledText) {
        val text = styled.text
        if (text.isEmpty()) return
        if (runningOutputId == null) {
            if (text.isBlank()) return
            val id = nextOutputId()
            runningText.clear()
            runningText.append(styled)
            blocks.add(OutputBlock(id = id, styled = styled, running = true))
            runningOutputId = id
            runningOutputIndex = blocks.lastIndex
        } else {
            runningText.append(styled)
            trimRunningText()
        }
        appendRaw(text)
    }

    fun applyCommandEnd(exitCode: Int?) {
        finishRunning(exitCode)
    }

    /**
     * Clears the screen: everything shown so far leaves the projections. A command that is still
     * streaming keeps running; its later output opens a new block below the cleared screen.
     */
    fun applyScreenCleared() {
        blocks.clear()
        expandedOverrides.clear()
        rawChunks.clear()
        rawSize = 0
        runningOutputId = null
        runningOutputIndex = -1
        runningText.clear()
    }

    // ---------------------------------------------------------- interaction

    /** Flips the manual expand/collapse override for one block. */
    fun toggleExpanded(blockId: String) {
        val index = indexOfBlock(blockId)
        if (index < 0) return
        val current = computeCollapsed(blocks[index], expandedOutputId())
        expandedOverrides[blockId] = !current
    }

    /** Forces one block open (search navigation), touching nothing else. */
    fun reveal(blockId: String) {
        if (indexOfBlock(blockId) >= 0) {
            expandedOverrides[blockId] = false
        }
    }

    // ------------------------------------------------------------- snapshot

    fun snapshot(displayRaw: Boolean): TranscriptSnapshot {
        val expandedOutput = expandedOutputId()
        val views = ArrayList<BlockView>(blocks.size)
        for (block in blocks) {
            val shown = if (block is OutputBlock && block.running) {
                block.copy(styled = runningText.tail(STREAM_RENDER_TAIL))
            } else {
                block
            }
            views.add(BlockView(shown, computeCollapsed(shown, expandedOutput)))
        }
        return TranscriptSnapshot(
            blocks = views,
            running = runningOutputId != null,
            rawText = if (displayRaw) rawTail(RAW_RENDER_CAP) else "",
        )
    }

    private fun computeCollapsed(block: Block, expandedOutputId: String?): Boolean {
        expandedOverrides[block.id]?.let { return it }
        return when (block) {
            is CommandBlock -> block.text.count { it == '\n' } + 1 > previewLines
            is OutputBlock -> !(block.running || block.id == expandedOutputId)
        }
    }

    // -------------------------------------------------------------- helpers

    private fun trimRunningText() {
        // Trim in batches: dropping the excess on every append would copy the
        // buffer once per chunk once the cap is reached.
        if (runningText.length > MAX_RUNNING_CHARS + RUNNING_TRIM_SLACK) {
            runningText.dropHead(runningText.length - MAX_RUNNING_CHARS)
        }
    }

    private fun finishRunning(exitCode: Int?) {
        val id = runningOutputId ?: return
        val index = if (blocks.getOrNull(runningOutputIndex)?.id == id) runningOutputIndex else indexOfBlock(id)
        runningOutputId = null
        runningOutputIndex = -1
        if (index >= 0) {
            val current = blocks[index] as OutputBlock
            blocks[index] = current.copy(styled = runningText.build(), running = false, exitCode = exitCode)
        }
        runningText.clear()
    }

    private fun indexOfBlock(id: String): Int =
        blocks.indexOfFirst { it.id == id }

    private fun nextOutputId(): String = "out-${++outputCounter}"

    /**
     * The output stays expanded only while it is the newest transcript
     * element. A newly submitted command lands after it, so every older
     * answer collapses automatically without extra bookkeeping.
     */
    private fun expandedOutputId(): String? {
        val lastCommandIndex = blocks.indexOfLast { it is CommandBlock }
        val lastOutputIndex = blocks.indexOfLast { it is OutputBlock }
        return if (lastOutputIndex > lastCommandIndex) blocks[lastOutputIndex].id else null
    }

    private fun appendRaw(text: String) {
        rawChunks.addLast(text)
        rawSize += text.length
        if (rawSize > rawCapChars + RAW_TRIM_SLACK) {
            // Drop head chunks only while the remainder still stays above
            // the cap: the buffer lands in [cap, cap + chunk) after a trim.
            while (rawChunks.size > 1 && rawSize - rawChunks.first().length >= rawCapChars) {
                rawSize -= rawChunks.removeFirst().length
            }
        }
    }

    /** The last [maxChars] characters of the raw projection, read from the newest chunks backwards. */
    private fun rawTail(maxChars: Int): String {
        val picked = ArrayList<String>()
        var budget = maxChars
        for (i in rawChunks.indices.reversed()) {
            val chunk = rawChunks[i]
            if (chunk.length >= budget) {
                picked.add(chunk.takeLast(budget))
                break
            }
            picked.add(chunk)
            budget -= chunk.length
        }
        return picked.asReversed().joinToString("")
    }

    companion object {
        const val DEFAULT_RAW_CAP = 1 shl 20
        private const val RAW_TRIM_SLACK = 1 shl 16
        private const val RUNNING_TRIM_SLACK = 64 * 1024

        /** Chars shown in the UI while a command is streaming. */
        const val STREAM_RENDER_TAIL = 3000

        /** Max chars rendered in raw view / finished block UI. */
        const val RAW_RENDER_CAP = 50_000

        /** Cap for the in-memory running buffer, to avoid OOM on huge streams. */
        const val MAX_RUNNING_CHARS = 1_200_000

        fun cmdId(seq: Long): String = "cmd-$seq"

        /**
         * Rebuilds a builder from persisted journal events. Raw output is
         * routed through a fresh parser so stored bytes are interpreted
         * with the current parsing rules.
         */
        fun replay(
            events: List<JournalEvent>,
            rawCapChars: Int = DEFAULT_RAW_CAP,
            previewLines: Int = PREVIEW_LINES,
        ): TranscriptBuilder {
            val builder = TranscriptBuilder(rawCapChars, previewLines)
            val parser = StreamParser()
            for (event in events) {
                when (event) {
                    is JournalEvent.CommandSubmitted ->
                        builder.applyCommandSubmitted(event.seq, event.text)

                    is JournalEvent.OutputArrived -> {
                        val parsed = try {
                            parser.feed(event.bytes)
                        } catch (e: RuntimeException) {
                            AtheaLog.error("replay", "parser failed on a stored chunk", e)
                            emptyList()
                        }
                        for (item in parsed) item.applyTo(builder)
                    }

                    is JournalEvent.CommandFinished ->
                        builder.applyCommandEnd(event.exitCode)

                    is JournalEvent.ScreenCleared -> builder.applyScreenCleared()
                }
            }
            return builder
        }
    }
}
