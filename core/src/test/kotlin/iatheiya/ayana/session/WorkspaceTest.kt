package iatheiya.ayana.session

import iatheiya.ayana.core.model.CommandBlock
import iatheiya.ayana.core.model.OutputBlock
import iatheiya.ayana.core.terminal.EngineEvent
import iatheiya.ayana.core.terminal.TerminalEngine
import iatheiya.ayana.data.AyanaStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val started = mutableListOf<FakeEngine>()

    private fun TestScope.workspace(
        storage: AyanaStorage = AyanaStorage(tmp.root),
        markless: Boolean = false,
    ): Workspace =
        Workspace(
            storage = storage,
            scope = backgroundScope,
            engines = { FakeEngine(reportsCommandEnds = !markless).also { started += it } },
            defaultSessionName = { "Session $it" },
            dispatcher = StandardTestDispatcher(testScheduler),
        ).also { runCurrent() }

    @Test
    fun `submitted command reaches the shell and the transcript`() = runTest {
        val ws = workspace()
        ws.updateDraft("ls -la")
        assertTrue(ws.sendDraft())

        assertEquals(listOf("ls -la\n"), started.single().written)
        val session = ws.state.value.sessions.single()
        assertEquals("", session.draft)
        assertEquals("ls -la", (session.blocks.single().block as CommandBlock).text)
    }

    @Test
    fun `history suggests the newest command that extends the draft`() = runTest {
        val ws = workspace()
        ws.updateDraft("git status")
        ws.sendDraft()
        ws.updateDraft("git log")
        ws.sendDraft()

        ws.updateDraft("git")
        assertEquals("git log", ws.state.value.suggestion)
        ws.acceptSuggestion()
        assertEquals("git log", ws.state.value.sessions.single().draft)
    }

    @Test
    fun `a prepared command waits in the composer on its own line`() = runTest {
        val ws = workspace()
        ws.updateDraft("Something")
        ws.placeCommand("cp '/x/a.txt' ./")

        assertEquals("Something\ncp '/x/a.txt' ./", ws.state.value.sessions.single().draft)
        assertTrue(started.single().written.isEmpty())
    }

    @Test
    fun `a prepared command runs at once when the setting says so`() = runTest {
        val ws = workspace()
        ws.updateSettings { it.copy(attachSendsNow = true) }
        ws.updateDraft("Something")
        ws.placeCommand("cp '/x/a.txt' ./")

        assertEquals(listOf("cp '/x/a.txt' ./\n"), started.single().written)
        assertEquals("Something", ws.state.value.sessions.single().draft)
    }

    @Test
    fun `a path goes after a space on the same line`() = runTest {
        val ws = workspace()
        ws.updateDraft("ls")
        ws.placePath("'/data/a b.txt'")

        assertEquals("ls '/data/a b.txt'", ws.state.value.sessions.single().draft)
    }

    @Test
    fun `shell output reaches the transcript after the refresh tick`() = runTest {
        val ws = workspace()
        ws.updateDraft("echo hi")
        ws.sendDraft()

        started.single().printOutput("hi\n")
        runCurrent() // the batch worker takes the output
        advanceTimeBy(200) // the refresh loop publishes it (100 ms tick)

        val output = ws.state.value.sessions.single().blocks.last().block as OutputBlock
        assertEquals("hi\n", output.text)
    }

    @Test
    fun `without command marks each submission opens its own output below it`() = runTest {
        val ws = workspace(markless = true)
        ws.updateDraft("pkg")
        ws.sendDraft()
        started.single().printOutput("first\n")
        runCurrent()
        advanceTimeBy(200)

        ws.updateDraft("ls")
        ws.sendDraft()
        started.single().printOutput("second\n")
        runCurrent()
        advanceTimeBy(200)

        val texts = ws.state.value.sessions.single().blocks.map { view ->
            when (val block = view.block) {
                is CommandBlock -> block.text
                is OutputBlock -> block.text
                else -> error("unexpected block $block")
            }
        }
        assertEquals(listOf("pkg", "first\n", "ls", "second\n"), texts)
    }

    @Test
    fun `a full-screen program takes the session and gets typed text as raw input`() = runTest {
        val ws = workspace(markless = true)
        started.single().printOutput("\u001B[?1049h")
        runCurrent()
        advanceTimeBy(200)

        assertNotNull(ws.state.value.sessions.single().fullScreen)
        ws.updateDraft("ihi")
        assertTrue(ws.sendDraft())

        assertEquals(listOf("ihi\r"), started.single().written)
        assertTrue(ws.state.value.sessions.single().blocks.isEmpty())
    }

    @Test
    fun `the full screen ends when the program leaves the alternate screen`() = runTest {
        val ws = workspace(markless = true)
        started.single().printOutput("\u001B[?1049h")
        runCurrent()
        advanceTimeBy(200)
        started.single().printOutput("\u001B[?1049l")
        runCurrent()
        advanceTimeBy(200)

        assertNull(ws.state.value.sessions.single().fullScreen)
    }

    @Test
    fun `favourites are unique by text and survive a restart`() = runTest {
        val storage = AyanaStorage(tmp.root)
        val ws = workspace(storage)
        ws.addFavorite("ls -la")
        ws.addFavorite("ls -la")
        ws.addFavorite("pwd")
        assertEquals(listOf("ls -la", "pwd"), ws.state.value.favorites.map { it.text })

        val reopened = workspace(storage)
        assertEquals(listOf("ls -la", "pwd"), reopened.state.value.favorites.map { it.text })
    }

    @Test
    fun `the shell directory shows as the prompt and survives a restart`() = runTest {
        val storage = AyanaStorage(tmp.root)
        val ws = workspace(storage)
        ws.updateDraft("cd sub")
        ws.sendDraft()
        val home = storage.shellHome().canonicalPath
        started.single().printOutput("\u001B]133;D;0\u0007\u001B]7;$home/sub\u0007")
        runCurrent()
        advanceTimeBy(200)
        assertEquals("~/sub", ws.state.value.sessions.single().directory)
        ws.close()

        // The stored session is projected while the new workspace is being built.
        val reopened = workspace(storage)
        assertEquals("~/sub", reopened.state.value.sessions.single().directory)
    }

    @Test
    fun `deleting the current session selects its neighbour`() = runTest {
        val ws = workspace()
        ws.newSession()
        ws.newSession()
        assertEquals(listOf(1L, 2L, 3L), ws.state.value.sessions.map { it.id })

        ws.selectSession(2L)
        ws.deleteSession(2L)

        assertEquals(3L, ws.state.value.currentSessionId)
        assertEquals(listOf(1L, 3L), ws.state.value.sessions.map { it.id })
    }

    @Test
    fun `search finds matching blocks and cycles through them`() = runTest {
        val ws = workspace()
        ws.updateDraft("alpha one")
        ws.sendDraft()
        ws.updateDraft("beta")
        ws.sendDraft()
        ws.updateDraft("alpha two")
        ws.sendDraft()

        ws.enterSearch()
        val first = ws.setSearchQuery("ALPHA")

        assertEquals(listOf("cmd-1", "cmd-3"), ws.state.value.search?.matchBlockIds)
        assertEquals("cmd-1", first)
        assertEquals("cmd-3", ws.nextSearchMatch())
        assertEquals("cmd-1", ws.nextSearchMatch())
    }

    @Test
    fun `settings are bounded and persisted with the shell path intact`() = runTest {
        val storage = AyanaStorage(tmp.root)
        val ws = workspace(storage)
        ws.updateSettings { it.copy(previewLines = 50, outputFontSizeSp = 99, shellPath = "/bin/custom") }
        ws.updateSettings { it.copy(rawStream = true) }

        val settings = ws.state.value.settings
        assertEquals(10, settings.previewLines)
        assertEquals(22, settings.outputFontSizeSp)
        assertTrue(settings.rawStream)

        val reloaded = storage.loadSettings()
        assertEquals("/bin/custom", reloaded.shellPath)
        assertEquals(10, reloaded.previewLines)
    }

    @Test
    fun `typing a draft keeps the transcript block list`() = runTest {
        val ws = workspace()
        ws.updateDraft("ls -la")
        ws.sendDraft()
        val before = ws.state.value.sessions.single().blocks

        ws.updateDraft("ls -la /tmp")

        assertSame(before, ws.state.value.sessions.single().blocks)
        assertEquals("ls -la /tmp", ws.state.value.sessions.single().draft)
    }

    @Test
    fun `shell exit waits in the queue until someone collects it`() = runTest {
        val ws = workspace()
        started.single().exit(3)
        runCurrent()

        assertEquals(WorkspaceEvent.ShellExited(3), ws.events.first())
    }

    /** Terminal backend that only records what was written and lets the test push output. */
    private class FakeEngine(override val reportsCommandEnds: Boolean = true) : TerminalEngine {
        private val output = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)
        override val events: SharedFlow<EngineEvent> = output

        private val alive = MutableStateFlow(false)
        override val isAlive: StateFlow<Boolean> = alive

        val written = mutableListOf<String>()

        override fun start(initialRows: Int, initialCols: Int): Boolean {
            alive.value = true
            return true
        }

        override fun write(data: ByteArray) {
            written += data.decodeToString()
        }

        override fun resize(rows: Int, cols: Int) = Unit

        override fun terminate() {
            alive.value = false
        }

        suspend fun printOutput(text: String) {
            output.emit(EngineEvent.Output(text.toByteArray()))
        }

        suspend fun exit(code: Int) {
            output.emit(EngineEvent.Exited(code))
        }
    }
}
