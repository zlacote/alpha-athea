package iatheiya.ayana.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestionStepTest {
    @Test
    fun `a one line suggestion is taken whole`() {
        assertEquals("ho hi", suggestionStep("ec", "echo hi"))
    }

    @Test
    fun `a longer suggestion stops at the end of the line`() {
        assertEquals("r i in 1 2; do", suggestionStep("fo", "for i in 1 2; do\necho \$i\ndone"))
    }

    @Test
    fun `at the end of a line the next line follows`() {
        assertEquals("\necho \$i", suggestionStep("for i in 1 2; do", "for i in 1 2; do\necho \$i\ndone"))
    }

    @Test
    fun `nothing when it does not extend the draft`() {
        assertNull(suggestionStep("ls", "echo"))
        assertNull(suggestionStep("", "echo"))
        assertNull(suggestionStep("echo", "echo"))
    }
}
