package com.athea.app.text

/** A color packed as 0xAARRGGBB. Plain data: the core never depends on a UI toolkit. */
@JvmInline
value class Rgb(val argb: Long)

/** Attributes of one run of characters. Carries only what chat output renders. */
data class Style(
    val foreground: Rgb? = null,
    val background: Rgb? = null,
    val bold: Boolean = false,
) {
    companion object {
        val NONE = Style()
    }
}

/** [style] covers the characters in the half-open range [start, end). */
data class Span(val start: Int, val end: Int, val style: Style)

/** Immutable text with styling spans. Spans never extend past [text]. */
data class StyledText(
    val text: String,
    val spans: List<Span> = emptyList(),
) {
    /** The last [maxChars] characters, with spans re-based onto the tail. */
    fun tail(maxChars: Int): StyledText =
        if (text.length <= maxChars) this else slice(text.length - maxChars, text.length)

    /** Characters in [from, to), with spans re-based onto the slice. */
    fun slice(from: Int, to: Int): StyledText =
        StyledText(text.substring(from, to), sliceSpans(spans, from, to))

    /** Drops trailing newlines; returns this instance when there are none. */
    fun trimTrailingNewlines(): StyledText {
        var end = text.length
        while (end > 0 && text[end - 1] == '\n') end--
        return if (end == text.length) this else slice(0, end)
    }
}

/** Mutable counterpart of [StyledText] for streams that grow and are trimmed from the head. */
class StyledTextBuilder {

    private val text = StringBuilder()
    private val spans = ArrayList<Span>()

    val length: Int get() = text.length

    fun append(chunk: CharSequence, style: Style = Style.NONE) {
        if (chunk.isEmpty()) return
        val start = text.length
        text.append(chunk)
        addSpan(start, style)
    }

    fun append(char: Char, style: Style = Style.NONE) {
        val start = text.length
        text.append(char)
        addSpan(start, style)
    }

    /** Appends a finished [StyledText] as one unit. */
    fun append(styled: StyledText) {
        val base = text.length
        text.append(styled.text)
        for (span in styled.spans) spans.add(span.shifted(base))
    }

    /** Moves everything from [other] to the end of this builder and empties [other]. */
    fun absorb(other: StyledTextBuilder) {
        val base = text.length
        text.append(other.text)
        for (span in other.spans) spans.add(span.shifted(base))
        other.clear()
    }

    /** Removes the last character; a span that covered only it disappears. */
    fun removeLast() {
        if (text.isEmpty()) return
        text.setLength(text.length - 1)
        while (spans.isNotEmpty() && spans.last().start >= text.length) spans.removeAt(spans.lastIndex)
        if (spans.isNotEmpty() && spans.last().end > text.length) {
            spans[spans.lastIndex] = spans.last().copy(end = text.length)
        }
    }

    /** Drops the first [count] characters, re-basing the spans that remain. */
    fun dropHead(count: Int) {
        val n = count.coerceIn(0, text.length)
        if (n == 0) return
        text.delete(0, n)
        val kept = sliceSpans(spans, n, Int.MAX_VALUE)
        spans.clear()
        spans.addAll(kept)
    }

    /** The last [maxChars] characters, copied without touching the rest of the buffer. */
    fun tail(maxChars: Int): StyledText {
        val from = (text.length - maxChars).coerceAtLeast(0)
        return StyledText(text.substring(from), sliceSpans(spans, from, text.length))
    }

    fun build(): StyledText = StyledText(text.toString(), spans.toList())

    fun clear() {
        text.setLength(0)
        spans.clear()
    }

    private fun addSpan(start: Int, style: Style) {
        if (style == Style.NONE) return
        val last = spans.lastOrNull()
        if (last != null && last.end == start && last.style == style) {
            spans[spans.lastIndex] = last.copy(end = text.length)
        } else {
            spans.add(Span(start, text.length, style))
        }
    }
}

private fun Span.shifted(offset: Int): Span = Span(start + offset, end + offset, style)

/** Spans overlapping [from, to), clipped and re-based to start at zero. */
private fun sliceSpans(spans: List<Span>, from: Int, to: Int): List<Span> {
    val out = ArrayList<Span>(spans.size)
    for (span in spans) {
        if (span.end <= from || span.start >= to) continue
        out.add(Span(maxOf(span.start, from) - from, minOf(span.end, to) - from, span.style))
    }
    return out
}
