package com.athea.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.ui.common.AtheaScaffold
import com.athea.app.ui.common.AtheaScrollHeader

/**
 * The glow in its own room. The switch stays on top; the sliders below wait for it and are
 * greyed out while the glow is off.
 */
@Composable
fun GlowScreen(
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AtheaScaffold(onBack = onBack, modifier = modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AtheaScrollHeader(stringResource(R.string.settings_glow_section))
            SettingsCard {
                SwitchRow(
                    title = stringResource(R.string.settings_glow),
                    subtitle = stringResource(R.string.settings_glow_desc),
                    checked = glowEnabled,
                    onChange = onGlowEnabledChange,
                )
            }
            SettingsCard {
                Column(Modifier.padding(vertical = 8.dp)) {
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
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
