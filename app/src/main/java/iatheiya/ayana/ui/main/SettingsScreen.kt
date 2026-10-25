package iatheiya.ayana.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import iatheiya.ayana.ui.common.AyanaConfirmDialog
import iatheiya.ayana.ui.theme.Ui
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.util.AyanaLog
import iatheiya.ayana.util.copyToClipboard
import java.io.File

/** Which room of the settings is on screen: the entry list or one of its sub-rooms. */
internal enum class SettingsPane { ROOT, INPUT, OUTPUT, APPEARANCE, FAVORITES }

@Composable
internal fun SettingsScreen(
    enterSends: Boolean,
    autoScrollOnSend: Boolean,
    rawStream: Boolean,
    signalLongCommands: Boolean,
    longCommandSec: Int,
    onSignalLongCommandsChange: (Boolean) -> Unit,
    notificationsAsked: Boolean,
    onNotificationsAsked: () -> Unit,
    onWaitForNotificationSettings: () -> Unit,
    onLongCommandSecChange: (Int) -> Unit,
    autocompleteEnabled: Boolean,
    virtualizeLargeOutput: Boolean,
    previewLines: Int,
    attachSendsNow: Boolean,
    favoriteReturnsHome: Boolean,
    favoriteSendsImmediately: Boolean,
    onFavoriteReturnsHomeChange: (Boolean) -> Unit,
    onFavoriteSendsImmediatelyChange: (Boolean) -> Unit,
    onEnterSendsChange: (Boolean) -> Unit,
    onAutoScrollOnSendChange: (Boolean) -> Unit,
    onRawStreamChange: (Boolean) -> Unit,
    onAutocompleteEnabledChange: (Boolean) -> Unit,
    onVirtualizeLargeOutputChange: (Boolean) -> Unit,
    onPreviewLinesChange: (Int) -> Unit,
    onAttachSendsNowChange: (Boolean) -> Unit,
    onOpenKeys: () -> Unit,
    /**
     * The open room, owned by the caller: a screen opened from a room (Glass, Glow) replaces the
     * settings on screen, and on its way back the settings must still be in that same room.
     */
    pane: SettingsPane,
    onPaneChange: (SettingsPane) -> Unit,
    appearanceScroll: ScrollState,
    appearance: AppearanceState,
    appearanceActions: AppearanceActions,
    scrollState: ScrollState,
    /** Everything back to how Ayana ships: every room's settings, the keys, the glass. */
    onResetAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Inside settings, back steps out of a sub-room first; at the root the outer
    // handler (in MainScreen) closes settings. The back arrow follows the same rule.
    BackHandler(enabled = pane != SettingsPane.ROOT) { onPaneChange(SettingsPane.ROOT) }
    val handleBack: () -> Unit = {
        if (pane != SettingsPane.ROOT) onPaneChange(SettingsPane.ROOT) else onBack()
    }

    AyanaScaffold(onBack = handleBack, modifier = modifier) {
        when (pane) {
            SettingsPane.ROOT -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                AyanaScrollHeader(stringResource(R.string.settings))

                SettingsEntryGroup {
                    SettingsEntryRow(
                        icon = Icons.Default.Keyboard,
                        title = stringResource(R.string.settings_section_input),
                        onClick = { onPaneChange(SettingsPane.INPUT) },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Default.Terminal,
                        title = stringResource(R.string.settings_section_output),
                        onClick = { onPaneChange(SettingsPane.OUTPUT) },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Outlined.StarBorder,
                        title = stringResource(R.string.favorites),
                        onClick = { onPaneChange(SettingsPane.FAVORITES) },
                    )
                }

                SettingsEntryGroup {
                    SettingsEntryRow(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.settings_section_appearance),
                        onClick = { onPaneChange(SettingsPane.APPEARANCE) },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Default.KeyboardCommandKey,
                        title = stringResource(R.string.settings_key_row),
                        onClick = onOpenKeys,
                    )
                }

                SettingsEntryGroup {
                    SettingsEntryRow(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.settings_diag_section),
                        onClick = { copyDiagnostics(context) },
                        // An action, not a room: it carries a copy affordance, not a chevron.
                        trailing = {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.settings_diag_copy),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }

                // The one way back to the defaults, for every room at once: at the very end,
                // centred and bare. It asks first, as it undoes a lot.
                var confirmReset by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Ui.spaceS),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { confirmReset = true }) {
                        Text(stringResource(R.string.glass_reset))
                    }
                }
                if (confirmReset) {
                    AyanaConfirmDialog(
                        title = stringResource(R.string.reset_all_title),
                        text = stringResource(R.string.reset_all_text),
                        onConfirm = {
                            confirmReset = false
                            onResetAll()
                        },
                        onDismiss = { confirmReset = false },
                        confirmLabel = stringResource(R.string.reset_all_confirm),
                        confirmTinted = true,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            SettingsPane.INPUT -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                AyanaScrollHeader(stringResource(R.string.settings_section_input))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_enter_sends),
                        subtitle = stringResource(R.string.settings_enter_sends_desc),
                        checked = enterSends,
                        onChange = onEnterSendsChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_autocomplete),
                        subtitle = stringResource(R.string.settings_autocomplete_desc),
                        checked = autocompleteEnabled,
                        onChange = onAutocompleteEnabledChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_attach_sends_now),
                        subtitle = stringResource(R.string.settings_attach_sends_now_desc),
                        checked = attachSendsNow,
                        onChange = onAttachSendsNowChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_auto_scroll),
                        subtitle = stringResource(R.string.settings_auto_scroll_desc),
                        checked = autoScrollOnSend,
                        onChange = onAutoScrollOnSendChange,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            SettingsPane.OUTPUT -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                AyanaScrollHeader(stringResource(R.string.settings_section_output))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_raw_stream),
                        subtitle = stringResource(R.string.settings_raw_stream_desc),
                        checked = rawStream,
                        onChange = onRawStreamChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_virtualize),
                        subtitle = stringResource(R.string.settings_virtualize_desc),
                        checked = virtualizeLargeOutput,
                        onChange = onVirtualizeLargeOutputChange,
                        // The raw stream is one continuous text, with no blocks to scroll inside.
                        enabled = !rawStream,
                    )
                    SettingsDivider()
                    SliderRow(
                        title = stringResource(R.string.settings_preview_lines),
                        value = previewLines,
                        range = 1f..10f,
                        onChange = onPreviewLinesChange,
                    )
                }
                // A long command's end: felt, or seen in the notifications while Ayana is away. The
                // switch turns on only once notifications are allowed: asked for in a dialog, or,
                // when Android no longer shows one, in the app's notification settings.
                val askNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> if (granted) onSignalLongCommandsChange(true) }
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_long_signal),
                        subtitle = stringResource(R.string.settings_long_signal_desc),
                        checked = signalLongCommands,
                        onChange = { on ->
                            when {
                                !on -> onSignalLongCommandsChange(false)
                                notificationsAllowed(context) -> onSignalLongCommandsChange(true)
                                notificationsAsked && !mayAskAgain(context) -> {
                                    onWaitForNotificationSettings()
                                    openNotificationSettings(context)
                                }
                                else -> {
                                    onNotificationsAsked()
                                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                    )
                    SettingsDivider()
                    SliderRow(
                        title = stringResource(R.string.settings_long_after),
                        value = longCommandSec,
                        range = 3f..120f,
                        onChange = onLongCommandSecChange,
                        enabled = signalLongCommands,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            SettingsPane.APPEARANCE -> AppearancePane(state = appearance, actions = appearanceActions, scrollState = appearanceScroll)

            SettingsPane.FAVORITES -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                AyanaScrollHeader(stringResource(R.string.favorites))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_fav_return),
                        subtitle = stringResource(R.string.settings_fav_return_desc),
                        checked = favoriteReturnsHome,
                        onChange = onFavoriteReturnsHomeChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_fav_send),
                        subtitle = stringResource(R.string.settings_fav_send_desc),
                        checked = favoriteSendsImmediately,
                        onChange = onFavoriteSendsImmediatelyChange,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Copies the last crash (if any) and the live log to the clipboard, then toasts. */
private fun copyDiagnostics(context: Context) {
    val crash = File(context.filesDir, "crash.log").takeIf { it.exists() }?.readText()
    val logs = AyanaLog.dump()
    val text = if (crash != null) {
        "=== last crash ===\n$crash\n\n=== live log ===\n$logs"
    } else {
        logs
    }
    context.copyToClipboard(text, "ayana-logs")
    android.widget.Toast.makeText(
        context,
        context.getString(R.string.settings_diag_copied),
        android.widget.Toast.LENGTH_SHORT,
    ).show()
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** False once the user has refused for good: Android then answers at once, without a dialog. */
private fun mayAskAgain(context: Context): Boolean {
    val activity = context.findActivity() ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
