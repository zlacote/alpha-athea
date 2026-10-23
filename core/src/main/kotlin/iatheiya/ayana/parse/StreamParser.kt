package iatheiya.ayana.parse

import iatheiya.ayana.text.Style
import iatheiya.ayana.text.StyledText
import iatheiya.ayana.text.StyledTextBuilder
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** Semantic events extracted from the raw terminal byte stream. */
sealed interface StreamEvent {
    /** Human-readable text with its colour spans. */
    data class Text(val styled: StyledText) : StreamEvent {
        val value: String get() = styled.text
    }

    /** A command's output begins (OSC 133;C). */
    data object OutputBegin : StreamEvent

    /** The running command finished (OSC 133;D), exit code when reported. */
    data class CommandEnd(val exitCode: Int?) : StreamEvent
}

/**
 * Incremental washer between the engine and the transcript.
 *
 * Responsibilities:
 *  - stream-decode UTF-8 across chunk boundaries without replacement gaps;
 *  - keep ANSI SGR colours as spans, strip other VT noise;
 *  - apply carriage-return overwrite semantics;
 *  - recognise shell integration marks (OSC 133 C/D[;exit]).
 *
 * Pure Kotlin: no Android dependencies, fully unit-testable.
 */
class StreamParser {

    private enum class State { NORMAL, ESC, CSI, OSC, OSC_ESC, CHARSET }

    private var state = State.NORMAL
    private val csiBuffer = StringBuilder()
    private val oscBuffer = StringBuilder()

    private val sgr = SgrState()

    /** Style for printable text; rebuilt only when an SGR sequence changes it. */
    private var cachedStyle: Style? = null

    /** The line being assembled; moves into [pending] at a newline or mark. */
    private val line = StyledTextBuilder()

    /** Finished lines that have not been emitted as a [StreamEvent.Text] yet. */
    private val pending = StyledTextBuilder()

    private val events = ArrayList<StreamEvent>()
    private var cursorAtLineStart = false

    private val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    private var carry = ByteArray(0)

    fun feed(chunk: ByteArray): List<StreamEvent> {
        events.clear()
        for (ch in decode(chunk)) step(ch)
        finishLine()
        flushPending()
        return ArrayList(events)
    }

    // ------------------------------------------------------------- decoding

    private fun decode(chunk: ByteArray): String {
        if (chunk.isEmpty()) return ""
        val bytes = if (carry.isEmpty()) chunk else carry + chunk
        val input = ByteBuffer.wrap(bytes)
        val output = CharBuffer.allocate(bytes.size)
        decoder.decode(input, output, false)
        output.flip()
        val text = output.toString()
        carry = ByteArray(input.remaining()).also { input.get(it) }
        return text
    }

    // ------------------------------------------------------------ state machine

    private fun step(ch: Char) {
        when (state) {
            State.ESC -> stepEsc(ch)
            State.CSI -> stepCsi(ch)
            State.OSC -> stepOsc(ch)
            State.OSC_ESC -> stepOscEsc(ch)
            State.CHARSET -> state = State.NORMAL
            State.NORMAL -> stepNormal(ch)
        }
    }

    private fun stepEsc(ch: Char) {
        when (ch) {
            '[' -> {
                csiBuffer.setLength(0)
                state = State.CSI
            }
            ']' -> {
                oscBuffer.setLength(0)
                state = State.OSC
            }
            '(', ')' -> state = State.CHARSET
            else -> state = State.NORMAL
        }
    }

    private fun stepCsi(ch: Char) {
        when {
            ch in '0'..'?' || ch in ' '..'/' -> csiBuffer.append(ch)
            ch == 'm' -> {
                sgr.applySgr(csiBuffer.toString())
                cachedStyle = null
                state = State.NORMAL
            }
            else -> state = State.NORMAL // any other CSI is stripped
        }
    }

    private fun stepOsc(ch: Char) {
        when (ch) {
            BEL -> {
                state = State.NORMAL
                handleOsc(oscBuffer.toString())
            }
            ESC -> state = State.OSC_ESC
            else -> oscBuffer.append(ch)
        }
    }

    private fun stepOscEsc(ch: Char) {
        state = State.NORMAL
        if (ch == '\\') handleOsc(oscBuffer.toString())
    }

    private fun stepNormal(ch: Char) {
        when (ch) {
            ESC -> state = State.ESC
            '\n' -> {
                line.append('\n', currentStyle())
                pending.absorb(line)
                cursorAtLineStart = false
            }
            '\r' -> cursorAtLineStart = true
            '\b' -> {
                cursorAtLineStart = false
                line.removeLast()
            }
            '\t' -> line.append('\t', currentStyle())
            else -> if (ch >= ' ') {
                if (cursorAtLineStart) {
                    line.clear()
                    cursorAtLineStart = false
                }
                line.append(ch, currentStyle())
            }
        }
    }

    // ------------------------------------------------------------------ marks

    private fun handleOsc(payload: String) {
        val parts = payload.split(';')
        if (parts.firstOrNull()?.toIntOrNull() != 133) return
        finishLine()
        flushPending()
        when (parts.getOrNull(1)) {
            "C" -> events.add(StreamEvent.OutputBegin)
            "D" -> events.add(StreamEvent.CommandEnd(parts.getOrNull(2)?.toIntOrNull()))
            else -> Unit // "A" and "B" prompt marks carry nothing Ayana shows
        }
    }

    // ---------------------------------------------------------------- helpers

    /** The chat projection of the current attributes: colour and weight only. */
    private fun currentStyle(): Style =
        cachedStyle ?: Style(
            foreground = sgr.foreground,
            background = sgr.background,
            bold = sgr.bold,
        ).also { cachedStyle = it }

    private fun finishLine() {
        pending.absorb(line)
    }

    private fun flushPending() {
        if (pending.length == 0) return
        events.add(StreamEvent.Text(pending.build()))
        pending.clear()
    }

    private companion object {
        const val ESC = '\u001B'
        const val BEL = '\u0007'
    }
}
