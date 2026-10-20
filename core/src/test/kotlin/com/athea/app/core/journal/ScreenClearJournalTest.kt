package com.athea.app.core.journal

import com.athea.app.core.journal.JournalEvent.CommandSubmitted
import com.athea.app.core.journal.JournalEvent.ScreenCleared
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenClearJournalTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `a stored screen clear is read back in order after a restart`() {
        val file = File(tmp.root, "sessions/1/journal.log")
        val journal = SessionJournal(file)
        journal.append(CommandSubmitted(seq = 1, text = "ls", timestampMs = 5))
        journal.append(ScreenCleared(timestampMs = 9))
        journal.append(CommandSubmitted(seq = 2, text = "pwd", timestampMs = 10))
        journal.close()

        assertEquals(
            listOf(
                CommandSubmitted(1, "ls", 5),
                ScreenCleared(9),
                CommandSubmitted(2, "pwd", 10),
            ),
            SessionJournal(file).readAll(),
        )
    }
}
