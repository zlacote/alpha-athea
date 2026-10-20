package com.athea.app.ui.main

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.data.SshTarget
import com.athea.app.ui.SshCheck
import com.athea.app.ui.common.AtheaScaffold
import com.athea.app.ui.common.AtheaScrollHeader
import com.athea.app.ui.theme.Ui
import com.athea.app.util.copyToClipboard

/** Where the Termux installation comes from: F-Droid carries the maintained build. */
private const val TERMUX_STORE_URL = "https://f-droid.org/packages/com.termux/"

/** Termux listens on this phone; the setup never targets another host. */
private const val LOOPBACK_HOST = "127.0.0.1"

private const val SSH_SERVER_COMMAND = "pkg install -y openssh && sshd"

/** The first page of the first run: what Athea is, then the choice of shell. */
@Composable
fun SetupWelcome(onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.setup_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_welcome_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_next))
            }
        }
    }
}

/** Where commands run: Athea's built-in shell, or Termux over SSH. */
@Composable
fun SetupShellChoice(
    onLocal: () -> Unit,
    onTermux: () -> Unit,
    onBack: () -> Unit,
) {
    AtheaScaffold(onBack = onBack) {
        AtheaScrollHeader(stringResource(R.string.setup_shell_title))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_shell_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ShellOption(
                title = stringResource(R.string.setup_shell_local),
                description = stringResource(R.string.setup_shell_local_desc),
                onClick = onLocal,
            )
            ShellOption(
                title = stringResource(R.string.setup_shell_termux),
                description = stringResource(R.string.setup_shell_termux_desc),
                onClick = onTermux,
            )
        }
    }
}

@Composable
private fun ShellOption(title: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = Ui.panelShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The Termux page: install it, start its SSH server, let it accept this app's key, then check
 * the connection. The shell is switched only after the check has passed for the values shown.
 */
@Composable
fun SetupTermux(
    publicKey: String,
    keyError: String?,
    check: SshCheck,
    onCheck: (String, Int) -> Unit,
    onUse: (String, Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("8022") }
    val port = portText.toIntOrNull()?.takeIf { it in 1..65535 }
    val candidate = port?.let { SshTarget(user.trim(), LOOPBACK_HOST, it) }
    val verified = candidate != null && check is SshCheck.Ok && check.target == candidate
    val keyCommand = if (publicKey.isEmpty()) {
        ""
    } else {
        // The home folder must not be writable by its group, or sshd refuses every key.
        "chmod go-w ~ && mkdir -p ~/.ssh && chmod 700 ~/.ssh && echo '$publicKey' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
    }

    AtheaScaffold(onBack = onBack) {
        AtheaScrollHeader(stringResource(R.string.setup_termux_title))
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepText(stringResource(R.string.setup_termux_step_install))
            OutlinedButton(onClick = { openUrl(context, TERMUX_STORE_URL) }) {
                Text(stringResource(R.string.setup_termux_open_store))
            }

            StepText(stringResource(R.string.setup_termux_step_packages))
            CodeLine(
                text = SSH_SERVER_COMMAND,
                copyable = true,
                onCopy = { context.copyToClipboard(SSH_SERVER_COMMAND, "athea-setup") },
            )

            StepText(stringResource(R.string.setup_termux_step_key))
            CodeLine(
                text = when {
                    keyCommand.isNotEmpty() -> keyCommand
                    keyError != null -> stringResource(R.string.setup_termux_key_failed, keyError)
                    else -> stringResource(R.string.setup_termux_key_loading)
                },
                copyable = keyCommand.isNotEmpty(),
                onCopy = { context.copyToClipboard(keyCommand, "athea-setup") },
            )

            StepText(stringResource(R.string.setup_termux_step_user))
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text(stringResource(R.string.setup_termux_user_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.setup_termux_port_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { candidate?.let { onCheck(it.user, it.port) } },
                enabled = candidate != null && check !is SshCheck.Checking,
            ) {
                Text(stringResource(R.string.setup_termux_check))
            }
            CheckStatus(check = check, verified = verified)
            Button(
                onClick = { candidate?.let { onUse(it.user, it.port) } },
                enabled = verified,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.setup_termux_use))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun CheckStatus(check: SshCheck, verified: Boolean) {
    val text = when {
        check is SshCheck.Checking -> stringResource(R.string.setup_termux_checking)
        check is SshCheck.Failed -> stringResource(R.string.setup_termux_failed, check.message)
        verified -> stringResource(R.string.setup_termux_ok)
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (check is SshCheck.Failed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun CodeLine(text: String, copyable: Boolean, onCopy: () -> Unit) {
    Surface(
        shape = Ui.attachmentShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCopy, enabled = copyable) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // No browser on the device: the address is still shown in the setup text.
    }
}
