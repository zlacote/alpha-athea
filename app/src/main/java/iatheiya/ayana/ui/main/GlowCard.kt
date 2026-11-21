package iatheiya.ayana.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import iatheiya.ayana.R

/**
 * The glow of a running command, as one card for the Notifications room: the switch on top, and
 * under it what the glow is like, greyed out while it is off.
 */
@Composable
internal fun GlowCard(
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
) {
    SettingsCard {
        SwitchRow(
            title = stringResource(R.string.settings_glow),
            subtitle = stringResource(R.string.settings_glow_desc),
            checked = glowEnabled,
            onChange = onGlowEnabledChange,
        )
        SettingsDivider()
        SliderRow(
            title = stringResource(R.string.settings_glow_strength),
            value = glowStrengthPct,
            range = 10f..100f,
            onChange = onGlowStrengthChange,
            enabled = glowEnabled,
        )
        SettingsDivider()
        SliderRow(
            title = stringResource(R.string.settings_glow_height),
            value = glowHeightDp,
            range = 12f..56f,
            onChange = onGlowHeightChange,
            enabled = glowEnabled,
        )
        SettingsDivider()
        SliderRow(
            title = stringResource(R.string.settings_glow_width),
            value = glowWidthPct,
            range = 40f..100f,
            onChange = onGlowWidthChange,
            enabled = glowEnabled,
        )
        SettingsDivider()
        SliderRow(
            title = stringResource(R.string.settings_glow_period),
            value = glowPeriodMs,
            range = 1000f..5000f,
            onChange = onGlowPeriodChange,
            enabled = glowEnabled,
        )
    }
}
