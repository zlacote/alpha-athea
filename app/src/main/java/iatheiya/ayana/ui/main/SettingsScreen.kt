package iatheiya.ayana.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
private enum class SettingsPane { ROOT, INPUT, OUTPUT, FAVORITES }

@Composable
fun SettingsScreen(
    enterSends: Boolean,
    autoScrollOnSend: Boolean,
    rawStream: Boolean,
    immersiveMode: Boolean,
    autocompleteEnabled: Boolean,
    pinchZoomEnabled: Boolean,
    virtualizeLargeOutput: Boolean,
    outputFontSizeSp: Int,
    previewLines: Int,
    bubbleFontSizeSp: Int,
    attachSendsNow: Boolean,
    favoriteReturnsHome: Boolean,
    favoriteSendsImmediately: Boolean,
    onFavoriteReturnsHomeChange: (Boolean) -> Unit,
    onFavoriteSendsImmediatelyChange: (Boolean) -> Unit,
    onEnterSendsChange: (Boolean) -> Unit,
    onAutoScrollOnSendChange: (Boolean) -> Unit,
    onRawStreamChange: (Boolean) -> Unit,
    onImmersiveModeChange: (Boolean) -> Unit,
    onAutocompleteEnabledChange: (Boolean) -> Unit,
    onPinchZoomEnabledChange: (Boolean) -> Unit,
    onVirtualizeLargeOutputChange: (Boolean) -> Unit,
    onOutputFontSizeChange: (Int) -> Unit,
    onPreviewLinesChange: (Int) -> Unit,
    onBubbleFontSizeChange: (Int) -> Unit,
    onAttachSendsNowChange: (Boolean) -> Unit,
    onOpenGlow: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenGlassTuning: () -> Unit,
    scrollState: ScrollState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pane by remember { mutableStateOf(SettingsPane.ROOT) }

    // Inside settings, back steps out of a sub-room first; at the root the outer
    // handler (in MainScreen) closes settings. The back arrow follows the same rule.
    BackHandler(enabled = pane != SettingsPane.ROOT) { pane = SettingsPane.ROOT }
    val handleBack: () -> Unit = {
        if (pane != SettingsPane.ROOT) pane = SettingsPane.ROOT else onBack()
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
                        onClick = { pane = SettingsPane.INPUT },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Default.Terminal,
                        title = stringResource(R.string.settings_section_output),
                        onClick = { pane = SettingsPane.OUTPUT },
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Outlined.StarBorder,
                        title = stringResource(R.string.favorites),
                        onClick = { pane = SettingsPane.FAVORITES },
                    )
                }

                SettingsEntryGroup {
                    SettingsEntryRow(
                        icon = Icons.Default.LightMode,
                        title = stringResource(R.string.settings_glow_section),
                        onClick = onOpenGlow,
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Default.KeyboardCommandKey,
                        title = stringResource(R.string.settings_key_row),
                        onClick = onOpenKeys,
                    )
                    SettingsDivider()
                    SettingsEntryRow(
                        icon = Icons.Default.BlurOn,
                        title = stringResource(R.string.glass_open),
                        onClick = onOpenGlassTuning,
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
                        title = stringResource(R.string.settings_immersive),
                        subtitle = stringResource(R.string.settings_immersive_desc),
                        checked = immersiveMode,
                        onChange = onImmersiveModeChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_raw_stream),
                        subtitle = stringResource(R.string.settings_raw_stream_desc),
                        checked = rawStream,
                        onChange = onRawStreamChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_pinch_zoom),
                        subtitle = stringResource(R.string.settings_pinch_zoom_desc),
                        checked = pinchZoomEnabled,
                        onChange = onPinchZoomEnabledChange,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_virtualize),
                        subtitle = stringResource(R.string.settings_virtualize_desc),
                        checked = virtualizeLargeOutput,
                        onChange = onVirtualizeLargeOutputChange,
                    )
                    SettingsDivider()
                    SliderRow(
                        title = stringResource(R.string.settings_font_size),
                        value = outputFontSizeSp,
                        range = 10f..22f,
                        onChange = onOutputFontSizeChange,
                    )
                    SettingsDivider()
                    SliderRow(
                        title = stringResource(R.string.settings_preview_lines),
                        value = previewLines,
                        range = 1f..10f,
                        onChange = onPreviewLinesChange,
                    )
                    SettingsDivider()
                    SliderRow(
                        title = stringResource(R.string.settings_bubble_size),
                        value = bubbleFontSizeSp,
                        range = 12f..24f,
                        onChange = onBubbleFontSizeChange,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

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
