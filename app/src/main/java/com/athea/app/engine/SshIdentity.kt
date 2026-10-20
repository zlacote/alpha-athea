package com.athea.app.engine

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.File

/**
 * The app's own SSH key. It is made once and kept in the app's private files. Its public half is
 * the line the user pastes into Termux, so the server accepts this app without a password.
 */
internal object SshIdentity {

    private const val KEY_BITS = 2048
    private const val COMMENT = "athea"

    fun privateKey(filesDir: File): File = File(filesDir, "ssh/id_rsa")

    /**
     * Creates the key pair on first use and returns the public key line. Blocking: call off the
     * main thread.
     */
    fun publicKeyLine(filesDir: File): String {
        val private = privateKey(filesDir)
        val public = File(private.parentFile, "id_rsa.pub")
        if (!private.isFile || !public.isFile) {
            private.parentFile?.mkdirs()
            val pair = KeyPair.genKeyPair(JSch(), KeyPair.RSA, KEY_BITS)
            pair.writePrivateKey(private.absolutePath)
            pair.writePublicKey(public.absolutePath, COMMENT)
            pair.dispose()
        }
        return public.readText().trim()
    }
}
