package iatheiya.ayana.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandRecallTest {

    private val history = listOf("ls", "git status", "make")

    @Test
    fun `up walks from the newest command to the oldest and then stops`() {
        val recall = CommandRecall()
        assertEquals("make", recall.up(history, ""))
        assertEquals("git status", recall.up(history, "make"))
        assertEquals("ls", recall.up(history, "git status"))
        assertNull(recall.up(history, "ls"))
    }

    @Test
    fun `down walks back to the empty field`() {
        val recall = CommandRecall()
        recall.up(history, "")
        recall.up(history, "make")
        assertEquals("make", recall.down(history, "git status"))
        assertEquals("", recall.down(history, "make"))
        assertNull(recall.down(history, ""))
    }

    @Test
    fun `each command comes up once, at its newest place`() {
        val recall = CommandRecall()
        val repeated = listOf("one", "one", "two", "one", "one")
        assertEquals("one", recall.up(repeated, ""))
        assertEquals("two", recall.up(repeated, "one"))
        assertNull(recall.up(repeated, "two"))
    }

    @Test
    fun `down sets written drafts aside and up finds them all, newest first`() {
        val recall = CommandRecall()
        assertEquals("", recall.down(history, "8"))
        assertEquals("", recall.down(history, "7"))
        assertEquals("7", recall.up(history, ""))
        assertEquals("8", recall.up(history, "7"))
        assertEquals("make", recall.up(history, "8"))
    }

    @Test
    fun `up on a written draft keeps it, so an earlier draft is not lost`() {
        val recall = CommandRecall()
        recall.down(history, "8")
        // "7" is typed and Up pressed at once: 7 is set aside, and the walk shows 8.
        assertEquals("8", recall.up(history, "7"))
        assertEquals("7", recall.down(history, "8"))
        assertEquals("", recall.down(history, "7"))
    }

    @Test
    fun `empty presses set nothing aside`() {
        val recall = CommandRecall()
        recall.down(history, "7")
        assertNull(recall.down(history, ""))
        recall.down(history, "6")
        assertEquals("6", recall.up(history, ""))
        assertEquals("7", recall.up(history, "6"))
        assertEquals("make", recall.up(history, "7"))
    }

    @Test
    fun `sending drops the drafts set aside`() {
        val recall = CommandRecall()
        recall.down(history, "draft")
        recall.sent()
        assertEquals("make", recall.up(history, ""))
    }

    @Test
    fun `without setting aside the live draft comes back as in a shell`() {
        val recall = CommandRecall().apply { setsAside = false }
        assertEquals("make", recall.up(history, "echo wip"))
        assertEquals("echo wip", recall.down(history, "make"))
        assertNull(recall.down(history, "echo wip"))
    }

    @Test
    fun `an empty history never moves`() {
        assertNull(CommandRecall().up(emptyList(), ""))
    }
}
