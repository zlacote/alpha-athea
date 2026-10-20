package com.athea.app.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalScreenTest {

    private fun TerminalScreen.write(text: String) = feed(text.toByteArray(Charsets.UTF_8))

    @Test
    fun `text wraps at the right edge`() {
        val screen = TerminalScreen(rows = 3, cols = 4)
        screen.write("abcdef")

        assertEquals(listOf("abcd", "ef", ""), screen.snapshot().rows)
    }

    @Test
    fun `carriage return overwrites from the start of the line`() {
        val screen = TerminalScreen(rows = 3, cols = 10)
        screen.write("abc\rX")

        assertEquals(listOf("Xbc", "", ""), screen.snapshot().rows)
    }

    @Test
    fun `cursor position is one-based`() {
        val screen = TerminalScreen(rows = 5, cols = 10)
        screen.write("\u001B[3;5HX")

        assertEquals(listOf("", "", "    X", "", ""), screen.snapshot().rows)
        assertEquals(2, screen.snapshot().cursorRow)
    }

    @Test
    fun `relative cursor moves overwrite earlier text`() {
        val screen = TerminalScreen(rows = 2, cols = 10)
        screen.write("ab\u001B[2DX")

        assertEquals("Xb", screen.snapshot().rows[0])
    }

    @Test
    fun `erase display clears everything and erase line clears to the end`() {
        val screen = TerminalScreen(rows = 2, cols = 20)
        screen.write("hello\r\nworld\u001B[2J")
        assertEquals(listOf("", ""), screen.snapshot().rows)

        screen.write("\u001B[1;1Hhello world\r\u001B[6G\u001B[K")
        assertEquals("hello", screen.snapshot().rows[0])
    }

    @Test
    fun `line feed on the last row scrolls the screen up`() {
        val screen = TerminalScreen(rows = 2, cols = 5)
        screen.write("one\r\ntwo\r\nthree")

        assertEquals(listOf("two", "three"), screen.snapshot().rows)
    }

    @Test
    fun `insert line pushes the rows below down`() {
        val screen = TerminalScreen(rows = 3, cols = 3)
        screen.write("aaa\r\nbbb\r\nccc")
        screen.write("\u001B[2;1H\u001B[L")

        assertEquals(listOf("aaa", "", "bbb"), screen.snapshot().rows)
    }

    @Test
    fun `colours and window titles do not reach the grid`() {
        val screen = TerminalScreen(rows = 1, cols = 10)
        screen.write("\u001B]0;title\u0007\u001B[31mred\u001B[0m")

        assertEquals("red", screen.snapshot().rows[0])
    }

    @Test
    fun `alternate screen shows the program and then restores the shell output`() {
        val screen = TerminalScreen(rows = 2, cols = 10)
        screen.write("main")
        screen.write("\u001B[?1049h")
        assertTrue(screen.altActive)
        assertEquals(listOf("", ""), screen.snapshot().rows)

        screen.write("vim")
        assertEquals("vim", screen.snapshot().rows[0])

        screen.write("\u001B[?1049l")
        assertFalse(screen.altActive)
        assertEquals("main", screen.snapshot().rows[0])
    }

    @Test
    fun `a utf-8 character split across chunks is decoded whole`() {
        val screen = TerminalScreen(rows = 1, cols = 4)
        val bytes = "ж".toByteArray(Charsets.UTF_8)
        screen.feed(bytes.copyOfRange(0, 1))
        screen.feed(bytes.copyOfRange(1, bytes.size))

        assertEquals("ж", screen.snapshot().rows[0])
    }

    @Test
    fun `resizing keeps the text that still fits`() {
        val screen = TerminalScreen(rows = 1, cols = 5)
        screen.write("abcde")
        screen.resize(rows = 2, cols = 3)

        assertEquals(listOf("abc", ""), screen.snapshot().rows)
    }
}
