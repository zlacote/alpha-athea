package iatheiya.ayana.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandRecallTest {

    private val history = listOf("ls", "git status", "make")

    @Test
    fun `up walks from the newest command to the oldest and then stops`() {
        val recall = CommandRecall()
        assertEquals("make", recall.step(history, "", older = true))
        assertEquals("git status", recall.step(history, "make", older = true))
        assertEquals("ls", recall.step(history, "git status", older = true))
        assertNull(recall.step(history, "ls", older = true))
    }

    @Test
    fun `down past the newest command restores the draft that was being typed`() {
        val recall = CommandRecall()
        assertEquals("make", recall.step(history, "echo wip", older = true))
        assertEquals("echo wip", recall.step(history, "make", older = false))
        assertNull(recall.step(history, "echo wip", older = false))
    }

    @Test
    fun `down does nothing before the walk has started`() {
        assertNull(CommandRecall().step(history, "draft", older = false))
    }

    @Test
    fun `an empty history never moves`() {
        assertNull(CommandRecall().step(emptyList(), "", older = true))
    }

    @Test
    fun `reset starts the walk again from the newest command`() {
        val recall = CommandRecall()
        recall.step(history, "", older = true)
        recall.step(history, "make", older = true)
        recall.reset()
        assertEquals("make", recall.step(history, "", older = true))
    }

    @Test
    fun `each command comes up once, at its newest place`() {
        val recall = CommandRecall()
        val repeated = listOf("one", "one", "two", "one", "one")
        assertEquals("one", recall.step(repeated, "", older = true))
        assertEquals("two", recall.step(repeated, "one", older = true))
        assertNull(recall.step(repeated, "two", older = true))
    }
}
