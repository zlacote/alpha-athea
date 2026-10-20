package com.athea.app.engine

import com.athea.app.core.terminal.EngineEvent
import com.athea.app.core.terminal.TerminalEngine
import com.athea.app.data.SshTarget
import com.athea.app.util.AtheaLog
import com.athea.app.util.dropOldestSharedFlow
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A shell on an SSH server: a PTY channel opened with this app's key. Connecting and reading run
 * on their own thread, writes and window changes on one more, so nothing blocks the main thread.
 * The UI sees only [TerminalEngine].
 *
 * Host keys are not checked yet: the setup only targets the loopback address of this phone.
 */
internal class SshShellEngine(
    private val target: SshTarget,
    private val identityFile: File,
) : TerminalEngine {

    private val _events = dropOldestSharedFlow<EngineEvent>(extraBufferCapacity = EVENT_BUFFER)
    override val events: SharedFlow<EngineEvent> = _events

    private val _isAlive = MutableStateFlow(false)
    override val isAlive: StateFlow<Boolean> = _isAlive

    private val started = AtomicBoolean(false)
    private val exitReported = AtomicBoolean(false)

    // Writes and window changes share one thread, in order, so they never interleave.
    private val io: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "athea-ssh-io") }

    @Volatile private var session: Session? = null
    @Volatile private var channel: ChannelShell? = null
    @Volatile private var rows = 24
    @Volatile private var cols = 80

    override fun start(initialRows: Int, initialCols: Int): Boolean {
        if (!started.compareAndSet(false, true)) return isAlive.value
        rows = initialRows
        cols = initialCols
        Thread({ connectAndPump() }, "athea-ssh").start()
        return true
    }

    // The remote shell sends no command marks, so Athea cannot see where a command ends.
    override val reportsCommandEnds: Boolean get() = false

    override fun write(data: ByteArray) {
        val shell = channel ?: return
        if (data.isEmpty() || !_isAlive.value) return
        onIo {
            try {
                shell.outputStream.write(data)
                shell.outputStream.flush()
            } catch (e: IOException) {
                AtheaLog.error("ssh", "write failed", e)
            }
        }
    }

    override fun resize(rows: Int, cols: Int) {
        this.rows = rows
        this.cols = cols
        val shell = channel ?: return
        if (!_isAlive.value) return
        onIo { shell.setPtySize(cols, rows, 0, 0) }
    }

    override fun terminate() {
        Thread({ disconnect() }, "athea-ssh-close").start()
    }

    private fun onIo(action: () -> Unit) {
        try {
            io.execute { action() }
        } catch (_: RejectedExecutionException) {
            // The connection has already ended, so there is nothing left to write to.
        }
    }

    private fun connectAndPump() {
        try {
            val jsch = JSch()
            jsch.addIdentity(identityFile.absolutePath)
            val connected = jsch.getSession(target.user, target.host, target.port)
            connected.setConfig("StrictHostKeyChecking", "no")
            connected.connect(CONNECT_TIMEOUT_MS)
            session = connected
            val shell = connected.openChannel("shell") as ChannelShell
            shell.setPtyType(PTY_TYPE, cols, rows, 0, 0)
            shell.connect(CONNECT_TIMEOUT_MS)
            channel = shell
            _isAlive.value = true
            AtheaLog.log("ssh", "connected to ${target.host}:${target.port} as ${target.user}")
            pump(shell)
        } catch (e: Throwable) {
            // A Throwable escaping this thread would end the app, so every failure is reported here.
            AtheaLog.error("ssh", "connection failed", e)
            val reason = e.message ?: e.javaClass.simpleName
            _events.tryEmit(EngineEvent.Output("ssh: could not connect ($reason)\n".toByteArray()))
            finish(-1)
        }
    }

    private fun pump(shell: ChannelShell) {
        val input = shell.inputStream
        val buffer = ByteArray(READ_BUFFER_SIZE)
        while (true) {
            val n = try {
                input.read(buffer)
            } catch (e: IOException) {
                AtheaLog.error("ssh", "read failed", e)
                -1
            }
            if (n < 0) break
            if (n > 0) _events.tryEmit(EngineEvent.Output(buffer.copyOf(n)))
        }
        finish(shell.exitStatus)
    }

    private fun finish(exitCode: Int) {
        _isAlive.value = false
        if (exitReported.compareAndSet(false, true)) {
            AtheaLog.log("ssh", "channel closed, code=$exitCode")
            _events.tryEmit(EngineEvent.Exited(exitCode))
        }
        disconnect()
        io.shutdown()
    }

    private fun disconnect() {
        try {
            channel?.disconnect()
        } catch (_: Exception) {
        }
        try {
            session?.disconnect()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val PTY_TYPE = "xterm-256color"
        const val READ_BUFFER_SIZE = 8192
        const val EVENT_BUFFER = 256
    }
}

/**
 * Tries the connection a shell would use, then closes it. Returns null when the server accepts
 * this app's key, or the reason it does not. Blocking: call off the main thread.
 */
internal fun probeSsh(target: SshTarget, identityFile: File): String? =
    try {
        val jsch = JSch()
        jsch.addIdentity(identityFile.absolutePath)
        val probe = jsch.getSession(target.user, target.host, target.port)
        probe.setConfig("StrictHostKeyChecking", "no")
        probe.connect(10_000)
        probe.disconnect()
        null
    } catch (e: Throwable) {
        // Linkage errors are caught too: they would otherwise end the app on the check.
        e.message ?: e.javaClass.simpleName
    }
