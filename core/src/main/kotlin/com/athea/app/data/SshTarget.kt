package com.athea.app.data

/**
 * A shell on an SSH server, as the settings store it: `ssh://user@host:port`. A shell path
 * without that scheme is a program on this phone and does not reach this class.
 */
data class SshTarget(
    val user: String,
    val host: String,
    val port: Int,
) {
    /** The shell path that stores this target in the settings. */
    fun toShellPath(): String = "$SCHEME$user@$host:$port"

    companion object {
        const val SCHEME = "ssh://"
        const val DEFAULT_PORT = 22

        /** The target of an `ssh://` shell path, or null when the path is not one or is malformed. */
        fun fromShellPath(shellPath: String): SshTarget? {
            if (!shellPath.startsWith(SCHEME)) return null
            val authority = shellPath.removePrefix(SCHEME)
            val user = authority.substringBefore('@', missingDelimiterValue = "")
            val hostAndPort = authority.substringAfter('@', missingDelimiterValue = "")
            if (user.isBlank() || hostAndPort.isBlank()) return null
            val host = hostAndPort.substringBeforeLast(':')
            val portText = hostAndPort.substringAfterLast(':', missingDelimiterValue = "")
            val port = if (portText.isEmpty()) DEFAULT_PORT else portText.toIntOrNull() ?: return null
            if (host.isBlank() || port !in 1..65535) return null
            return SshTarget(user, host, port)
        }
    }
}
