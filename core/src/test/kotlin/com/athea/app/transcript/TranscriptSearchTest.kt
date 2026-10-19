package com.athea.app.transcript

import com.athea.app.core.model.CommandBlock
import com.athea.app.core.model.OutputBlock
import com.athea.app.text.StyledText
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptSearchTest {

    private val blocks = listOf(
        BlockView(CommandBlock("cmd-1", "Hello World"), false),
        BlockView(OutputBlock("out-1", StyledText("hello world output")), false),
        BlockView(OutputBlock("out-2", StyledText("nothing here")), false),
    )

    @Test
    fun `matches are case-insensitive across commands and output`() {
        assertEquals(listOf("cmd-1", "out-1"), TranscriptSearch.matchingBlockIds(blocks, "HELLO"))
    }

    @Test
    fun `blank query matches nothing`() {
        assertEquals(emptyList<String>(), TranscriptSearch.matchingBlockIds(blocks, "   "))
    }
}
