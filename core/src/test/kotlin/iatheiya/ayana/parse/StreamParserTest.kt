package iatheiya.ayana.parse

import iatheiya.ayana.text.Rgb
import iatheiya.ayana.text.Span
import iatheiya.ayana.text.Style
import iatheiya.ayana.text.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamParserTest {

    private fun feedAll(vararg chunks: String): List<StreamEvent> {
        val parser = StreamParser()
        return chunks.flatMap { parser.feed(it.toByteArray(Charsets.UTF_8)) }
    }

    private fun texts(events: List<StreamEvent>): String =
        events.filterIsInstance<StreamEvent.Text>().joinToString("") { it.value }

    private fun textEvent(text: String) = StreamEvent.Text(StyledText(text))

    @Test
    fun `plain text passes through`() {
        val events = feedAll("hello world\n")
        assertEquals(1, events.size)
        assertEquals("hello world\n", texts(events))
    }

    @Test
    fun `color sequences are stripped`() {
        val events = feedAll("\u001B[31mred\u001B[0m plain\n")
        assertEquals("red plain\n", texts(events))
    }

    @Test
    fun `escape split across chunks is handled`() {
        val events = feedAll("\u001B", "[31mred\n")
        assertEquals("red\n", texts(events))
    }

    @Test
    fun `crlf line endings keep their content`() {
        // Real terminals emit CRLF; this exact shape once wiped every
        // line and produced empty outputs.
        val events = feedAll("ls: .: Permission denied\r\n")
        assertEquals("ls: .: Permission denied\n", texts(events))
    }

    @Test
    fun `carriage return keeps final line state`() {
        val events = feedAll("10%\r50%\r100%\nnext\n")
        assertEquals("100%\nnext\n", texts(events))
    }

    @Test
    fun `backspace deletes last character`() {
        val events = feedAll("abc\b\bd\n")
        assertEquals("ad\n", texts(events))
    }

    @Test
    fun `utf-8 multibyte split across chunks survives`() {
        val parser = StreamParser()
        val first = parser.feed(byteArrayOf(0xC3.toByte()))
        val second = parser.feed(
            byteArrayOf(0xA9.toByte()) + "x\n".toByteArray()
        )
        assertEquals(0, first.size)
        assertEquals("éx\n", texts(second))
    }

    @Test
    fun `finish mark reports exit code`() {
        val events = feedAll("\u001B]133;D;2\u0007rest\n")
        assertEquals(
            listOf<StreamEvent>(
                StreamEvent.CommandEnd(2),
                textEvent("rest\n"),
            ),
            events,
        )
    }

    @Test
    fun `mark split across chunks is recognized`() {
        val parser = StreamParser()
        val first = parser.feed("\u001B]133;D;".toByteArray())
        val second = parser.feed("2\u0007rest\n".toByteArray())
        assertEquals(0, first.size)
        assertEquals(
            listOf<StreamEvent>(
                StreamEvent.CommandEnd(2),
                textEvent("rest\n"),
            ),
            second,
        )
    }

    @Test
    fun `working directory mark is reported and hidden`() {
        val events = feedAll("out\n\u001B]7;/data/home/src\u0007")
        assertEquals("out\n", texts(events))
        assertEquals(
            listOf("/data/home/src"),
            events.filterIsInstance<StreamEvent.WorkingDirectory>().map { it.path },
        )
    }

    @Test
    fun `working directory url form keeps only the path`() {
        val events = feedAll("\u001B]7;file://phone/sdcard/Download\u0007")
        assertEquals(
            listOf("/sdcard/Download"),
            events.filterIsInstance<StreamEvent.WorkingDirectory>().map { it.path },
        )
    }

    @Test
    fun `output begin mark is emitted`() {
        val events = feedAll("\u001B]133;C\u0007out\n")
        assertEquals(
            listOf<StreamEvent>(
                StreamEvent.OutputBegin,
                textEvent("out\n"),
            ),
            events,
        )
    }

    @Test
    fun `prompt marks are consumed but text passes through`() {
        val parser = StreamParser()
        val first = parser.feed(
            "\u001B]133;A\u0007\u001B]133;B\u0007".toByteArray()
        )
        val second = parser.feed("$ ls\n".toByteArray())
        assertTrue(first.isEmpty())
        assertEquals(
            listOf<StreamEvent>(textEvent("$ ls\n")),
            second,
        )
    }

    @Test
    fun `sgr colour becomes a span over its own text`() {
        val events = feedAll("plain \u001B[31mred\u001B[0m end\n")
        val text = events.single() as StreamEvent.Text
        assertEquals("plain red end\n", text.value)
        assertEquals(
            listOf(Span(6, 9, Style(foreground = Rgb(0xFFCC0000)))),
            text.styled.spans,
        )
    }

    @Test
    fun `backspace keeps the colour of the characters that remain`() {
        val events = feedAll("\u001B[32mabc\b\bd")
        val text = events.single() as StreamEvent.Text
        assertEquals("ad", text.value)
        assertEquals(
            listOf(Span(0, 2, Style(foreground = Rgb(0xFF00CC00)))),
            text.styled.spans,
        )
    }

    @Test
    fun `unrelated osc sequences are swallowed`() {
        val events = feedAll("\u001B]0;window title\u0007hi\n")
        assertEquals("hi\n", texts(events))
    }

    @Test
    fun `text before a mark keeps chronological order`() {
        val events = feedAll("tail\u001B]133;D;0\u0007")
        assertEquals(
            listOf<StreamEvent>(
                textEvent("tail"),
                StreamEvent.CommandEnd(0),
            ),
            events,
        )
    }
}
