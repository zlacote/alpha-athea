package com.athea.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.ui.common.AtheaScaffold
import com.athea.app.ui.common.AtheaScrollHeader
import com.athea.app.util.AtheaLog
import com.athea.app.util.copyToClipboard

@Composable
fun SettingsScreen(
    keyRowVisible: Boolean,
    enterSends: Boolean,
    autoScrollOnSend: Boolean,
    rawStream: Boolean,
    autocompleteEnabled: Boolean,
    pinchZoomEnabled: Boolean,
    virtualizeLargeOutput: Boolean,
    outputFontSizeSp: Int,
    previewLines: Int,
    bubbleFontSizeSp: Int,
    onKeyRowVisibleChange: (Boolean) -> Unit,
    attachSendsNow: Boolean,
    onAttachSendsNowChange: (Boolean) -> Unit,
    glowEnabled: Boolean,
    glowStrengthPct: Int,
    glowHeightDp: Int,
    glowWidthPct: Int,
    glowPeriodMs: Int,
    onGlowEnabledChange: (Boolean) -> Unit,
    onGlowStrengthChange: (Int) -> Unit,
    onGlowHeightChange: (Int) -> Unit,
    onGlowWidthChange: (Int) -> Unit,
    onGlowPeriodChange: (Int) -> Unit,
    onEnterSendsChange: (Boolean) -> Unit,
    onAutoScrollOnSendChange: (Boolean) -> Unit,
    onRawStreamChange: (Boolean) -> Unit,
    onAutocompleteEnabledChange: (Boolean) -> Unit,
    onPinchZoomEnabledChange: (Boolean) -> Unit,
    onVirtualizeLargeOutputChange: (Boolean) -> Unit,
    onOutputFontSizeChange: (Int) -> Unit,
    onPreviewLinesChange: (Int) -> Unit,
    onBubbleFontSizeChange: (Int) -> Unit,
    shellLabel: String,
    onOpenShell: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenGlassTuning: () -> Unit,
    scrollState: ScrollState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AtheaScaffold(onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            AtheaScrollHeader(stringResource(R.string.settings))

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

            SettingsSection(title = stringResource(R.string.settings_glow_section)) {
                SwitchRow(
                    title = stringResource(R.string.settings_glow),
                    subtitle = stringResource(R.string.settings_glow_desc),
                    checked = glowEnabled,
                    onChange = onGlowEnabledChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_glow_strength),
                    value = glowStrengthPct,
                    range = 10f..100f,
                    onChange = onGlowStrengthChange,
                    enabled = glowEnabled,
                )
                SliderRow(
                    title = stringResource(R.string.settings_glow_height),
                    value = glowHeightDp,
                    range = 12f..56f,
                    onChange = onGlowHeightChange,
                    enabled = glowEnabled,
                )
                SliderRow(
                    title = stringResource(R.string.settings_glow_width),
                    value = glowWidthPct,
                    range = 40f..100f,
                    onChange = onGlowWidthChange,
                    enabled = glowEnabled,
                )
                SliderRow(
                    title = stringResource(R.string.settings_glow_period),
                    value = glowPeriodMs,
                    range = 1000f..5000f,
                    onChange = onGlowPeriodChange,
                    enabled = glowEnabled,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_shell_section)) {
                ActionRow(
                    title = stringResource(R.string.shell_current, shellLabel),
                    onClick = onOpenShell,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_keys_section)) {
                SwitchRow(
                    title = stringResource(R.string.settings_key_row),
                    subtitle = stringResource(R.string.settings_key_row_desc),
                    checked = keyRowVisible,
                    onChange = onKeyRowVisibleChange,
                )
                ActionRow(
                    title = stringResource(R.string.keys_open),
                    onClick = onOpenKeys,
                )
            }

            SettingsSection(title = stringResource(R.string.glass_section)) {
                ActionRow(
                    title = stringResource(R.string.glass_open),
                    onClick = onOpenGlassTuning,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_diag_section)) {
                ActionRow(title = stringResource(R.string.settings_diag_copy)) {
                    context.copyToClipboard(AtheaLog.dump(), "athea-logs")
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
