package com.athea.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SshTargetTest {

    @Test
    fun `reads user, host and port from an ssh shell path`() {
        assertEquals(
            SshTarget("u0_a12", "127.0.0.1", 8022),
            SshTarget.fromShellPath("ssh://u0_a12@127.0.0.1:8022"),
        )
    }

    @Test
    fun `port defaults to 22 when none is given`() {
        assertEquals(SshTarget("me", "server", 22), SshTarget.fromShellPath("ssh://me@server"))
    }

    @Test
    fun `a local shell path is not an ssh target`() {
        assertNull(SshTarget.fromShellPath("/system/bin/sh"))
    }

    @Test
    fun `malformed ssh paths are rejected`() {
        assertNull(SshTarget.fromShellPath("ssh://@127.0.0.1:8022"))
        assertNull(SshTarget.fromShellPath("ssh://u0_a12@"))
        assertNull(SshTarget.fromShellPath("ssh://u0_a12@127.0.0.1:99999"))
        assertNull(SshTarget.fromShellPath("ssh://u0_a12@127.0.0.1:port"))
    }

    @Test
    fun `the shell path round trips`() {
        val target = SshTarget("u0_a12", "127.0.0.1", 8022)
        assertEquals(target, SshTarget.fromShellPath(target.toShellPath()))
    }
}
