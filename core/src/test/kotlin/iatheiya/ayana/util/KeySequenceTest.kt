package iatheiya.ayana.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeySequenceTest {

    @Test
    fun `escape names become control characters`() {
        assertEquals("\u001B[H", unescapeKeySequence("\\e[H"))
        assertEquals("\u001B[5~", unescapeKeySequence("\\E[5~"))
        assertEquals("a\tb\r", unescapeKeySequence("a\\tb\\r"))
        assertEquals("\u0003", unescapeKeySequence("\\x03"))
        assertEquals("\u0003", unescapeKeySequence("\\u0003"))
    }

    @Test
    fun `unknown or broken escapes stay literal`() {
        assertEquals("\\q", unescapeKeySequence("\\q"))
        assertEquals("\\x3", unescapeKeySequence("\\x3"))
        assertEquals("end\\", unescapeKeySequence("end\\"))
        assertEquals("a\\b", unescapeKeySequence("a\\\\b"))
    }

    @Test
    fun `escape and unescape round trip`() {
        val raw = "\u001B[1;5A\t\\x\u0003"
        assertEquals(raw, unescapeKeySequence(escapeKeySequence(raw)))
    }

    @Test
    fun `plain text is told from a key sequence`() {
        assertTrue("ls -la".isPlainText())
        assertFalse("\u001B[H".isPlainText())
    }
}
