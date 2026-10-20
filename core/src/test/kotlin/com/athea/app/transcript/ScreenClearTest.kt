package com.athea.app.transcript

import com.athea.app.core.journal.JournalEvent
import com.athea.app.core.model.CommandBlock
import com.athea.app.core.model.OutputBlock
import com.athea.app.text.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenClearTest {

    @Test
    fun `clearing the screen drops what was shown and keeps later output`() {
        val builder = TranscriptBuilder()
        builder.applyCommandSubmitted(1, "ls")
        builder.applyOutput(StyledText("one\n"))
        builder.applyCommandEnd(0)

        builder.applyScreenCleared()
        builder.applyCommandSubmitted(2, "pwd")
        builder.applyOutput(StyledText("/home\n"))
        builder.applyCommandEnd(0)

        val blocks = builder.snapshot(displayRaw = false).blocks.map { it.block }
        assertEquals(2, blocks.size)
        assertEquals("pwd", (blocks[0] as CommandBlock).text)
        assertTrue((blocks[1] as OutputBlock).text.startsWith("/home"))
    }

    @Test
    fun `replay applies a stored clear in order`() {
        val events = listOf(
            JournalEvent.CommandSubmitted(seq = 1, text = "ls", timestampMs = 0),
            JournalEvent.OutputArrived("one\n".toByteArray()),
            JournalEvent.CommandFinished(exitCode = 0),
            JournalEvent.ScreenCleared(timestampMs = 0),
            JournalEvent.CommandSubmitted(seq = 2, text = "pwd", timestampMs = 0),
        )

        val blocks = TranscriptBuilder.replay(events).snapshot(displayRaw = false).blocks.map { it.block }
        assertEquals(1, blocks.size)
        assertEquals("pwd", (blocks[0] as CommandBlock).text)
    }
}
