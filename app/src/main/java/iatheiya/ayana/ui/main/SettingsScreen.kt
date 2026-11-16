package iatheiya.ayana.ui.main

import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.clickable
import iatheiya.ayana.ui.common.AyanaPage
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.RestartAlt
import iatheiya.ayana.ui.common.AyanaLight
import iatheiya.ayana.BuildConfig
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.LaunchedEffect
import iatheiya.ayana.ui.common.warmLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import iatheiya.ayana.util.toast
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.core.app.ActivityCompat
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import iatheiya.ayana.util.AyanaLog
import iatheiya.ayana.util.notificationsAllowed
import iatheiya.ayana.util.copyToClipboard
import java.io.File

/** Which room of the settings is on screen: the entry list or one of its sub-rooms. */
internal enum class SettingsPane { ROOT, INPUT, OUTPUT, APPEARANCE, ABOUT }

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
    downSetsDraftAside: Boolean,
    onDownSetsDraftAsideChange: (Boolean) -> Unit,
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

    // The light About shows is painted now, away from the main thread, so About opens with it
    // already shimmering instead of stopping on its first frame to paint it.
    val windowWidth = LocalWindowInfo.current.containerSize.width
    LaunchedEffect(windowWidth) {
        withContext(Dispatchers.Default) { warmLight(windowWidth) }
    }

    AyanaScaffold(onBack = handleBack, modifier = modifier) {
        when (pane) {
            SettingsPane.ROOT -> AyanaPage(stringResource(R.string.settings), scrollState) {
                SettingsCard {
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
                }
                SettingsCard {
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
                SettingsCard {
                    SettingsEntryRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_about),
                        onClick = { onPaneChange(SettingsPane.ABOUT) },
                    )
                }
            }

            SettingsPane.INPUT -> AyanaPage(stringResource(R.string.settings_section_input)) {
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
                        title = stringResource(R.string.settings_down_aside),
                        subtitle = stringResource(R.string.settings_down_aside_desc),
                        checked = downSetsDraftAside,
                        onChange = onDownSetsDraftAsideChange,
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
                // A tapped favorite: stays in the favorites and goes into the input (off), takes
                // you back to the chat with it in the input, or is sent and takes you back to see
                // its answer. One of the three, so one switch with three places.
                SettingsCard {
                    ThreeWaySwitchRow(
                        title = stringResource(R.string.favorites),
                        subtitle = stringResource(R.string.settings_fav_desc),
                        choices = listOf(
                            FAV_OFF to stringResource(R.string.settings_fav_off),
                            FAV_RETURN to stringResource(R.string.settings_fav_return),
                            FAV_SEND to stringResource(R.string.settings_fav_send),
                        ),
                        selected = when {
                            favoriteSendsImmediately -> FAV_SEND
                            favoriteReturnsHome -> FAV_RETURN
                            else -> FAV_OFF
                        },
                        onSelect = { choice ->
                            onFavoriteReturnsHomeChange(choice != FAV_OFF)
                            onFavoriteSendsImmediatelyChange(choice == FAV_SEND)
                        },
                    )
                }
            }

            SettingsPane.OUTPUT -> AyanaPage(stringResource(R.string.settings_section_output)) {
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
                                context.notificationsAllowed() -> onSignalLongCommandsChange(true)
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
                    SettingsDivider()
                    // The light at the top edge while a command runs: how a running command shows.
                    SettingsEntryRow(
                        icon = Icons.Default.LightMode,
                        title = stringResource(R.string.settings_glow_section),
                        onClick = appearanceActions.onOpenGlow,
                    )
                }
            }

            SettingsPane.APPEARANCE -> AppearancePane(state = appearance, actions = appearanceActions, scrollState = appearanceScroll)

            SettingsPane.ABOUT -> AyanaPage(stringResource(R.string.settings_about)) {
                // The icon's light as the room's emblem, across the whole width.
                AyanaLight(
                    Modifier
                        .fillMaxWidth()
                        .height(ABOUT_LIGHT_HEIGHT),
                )
                val uriHandler = LocalUriHandler.current
                val sourceUrl = stringResource(R.string.about_source_url)
                // Everything About offers, in one card, like the other rooms.
                var confirmReset by remember { mutableStateOf(false) }
                SettingsCard {
                    SettingsEntryRow(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.about_source),
                        onClick = { uriHandler.openUri(sourceUrl) },
                        trailing = { OpenOutside() },
                    )
                    SettingsDivider()
                    // Actions, not rooms: their words say what they do, so they need no chevron.
                    SettingsEntryRow(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.settings_diag_copy),
                        onClick = { copyDiagnostics(context) },
                        trailing = {},
                    )
                    SettingsDivider()
                    // The one way back to the defaults, for every room at once. It asks first, as
                    // it undoes a lot.
                    SettingsEntryRow(
                        icon = Icons.Default.RestartAlt,
                        title = stringResource(R.string.glass_reset),
                        onClick = { confirmReset = true },
                        trailing = {},
                    )
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
                // The version, quiet, at the very end.
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = Ui.spaceL),
                )
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
    context.toast(R.string.settings_diag_copied)
}

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

@Composable
private fun OpenOutside() {
    Icon(
        Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val FAV_OFF = 0
private const val FAV_RETURN = 1
private const val FAV_SEND = 2

private val ABOUT_LIGHT_HEIGHT = 300.dp
