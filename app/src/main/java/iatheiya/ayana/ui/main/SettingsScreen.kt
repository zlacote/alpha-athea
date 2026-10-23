package iatheiya.ayana.ui.main

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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

@Composable
fun SettingsScreen(
    enterSends: Boolean,
    autoScrollOnSend: Boolean,
    rawStream: Boolean,
    autocompleteEnabled: Boolean,
    pinchZoomEnabled: Boolean,
    virtualizeLargeOutput: Boolean,
    outputFontSizeSp: Int,
    previewLines: Int,
    bubbleFontSizeSp: Int,
    attachSendsNow: Boolean,
    onEnterSendsChange: (Boolean) -> Unit,
    onAutoScrollOnSendChange: (Boolean) -> Unit,
    onRawStreamChange: (Boolean) -> Unit,
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

    AyanaScaffold(onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            AyanaScrollHeader(stringResource(R.string.settings))

            SettingsSection(title = stringResource(R.string.settings_section_input)) {
                SwitchRow(
                    title = stringResource(R.string.settings_enter_sends),
                    subtitle = stringResource(R.string.settings_enter_sends_desc),
                    checked = enterSends,
                    onChange = onEnterSendsChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_autocomplete),
                    subtitle = stringResource(R.string.settings_autocomplete_desc),
                    checked = autocompleteEnabled,
                    onChange = onAutocompleteEnabledChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_attach_sends_now),
                    subtitle = stringResource(R.string.settings_attach_sends_now_desc),
                    checked = attachSendsNow,
                    onChange = onAttachSendsNowChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_auto_scroll),
                    subtitle = stringResource(R.string.settings_auto_scroll_desc),
                    checked = autoScrollOnSend,
                    onChange = onAutoScrollOnSendChange,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_output)) {
                SwitchRow(
                    title = stringResource(R.string.settings_raw_stream),
                    subtitle = stringResource(R.string.settings_raw_stream_desc),
                    checked = rawStream,
                    onChange = onRawStreamChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_pinch_zoom),
                    subtitle = stringResource(R.string.settings_pinch_zoom_desc),
                    checked = pinchZoomEnabled,
                    onChange = onPinchZoomEnabledChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_virtualize),
                    subtitle = stringResource(R.string.settings_virtualize_desc),
                    checked = virtualizeLargeOutput,
                    onChange = onVirtualizeLargeOutputChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_font_size),
                    value = outputFontSizeSp,
                    range = 10f..22f,
                    onChange = onOutputFontSizeChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_preview_lines),
                    value = previewLines,
                    range = 1f..10f,
                    onChange = onPreviewLinesChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_bubble_size),
                    value = bubbleFontSizeSp,
                    range = 12f..24f,
                    onChange = onBubbleFontSizeChange,
                )
            }

            // Each entry is one centred button that opens its own room; they carry no section title.
            SettingsCard {
                ActionRow(
                    title = stringResource(R.string.settings_glow_section),
                    onClick = onOpenGlow,
                )
            }
            SettingsCard {
                ActionRow(
                    title = stringResource(R.string.settings_key_row),
                    onClick = onOpenKeys,
                )
            }
            SettingsCard {
                ActionRow(
                    title = stringResource(R.string.glass_open),
                    onClick = onOpenGlassTuning,
                )
            }
            SettingsCard {
                ActionRow(title = stringResource(R.string.settings_diag_copy)) {
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
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
