package iatheiya.ayana.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.FONT_CUSTOM
import iatheiya.ayana.data.FONT_MONO
import iatheiya.ayana.data.FONT_SERIF
import iatheiya.ayana.data.FONT_SYSTEM
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.liquid.GlassTuning

/** What the Appearance room shows. */
data class AppearanceState(
    val immersiveMode: Boolean,
    val keepScreenOn: Boolean,
    val tintArgb: Long?,
    val font: String,
    val hasCustomFont: Boolean,
    val hasWallpaper: Boolean,
    val wallpaperDimPct: Int,
    val pinchZoomEnabled: Boolean,
    val outputFontSizeSp: Int,
    val bubbleFontSizeSp: Int,
)

/** What the Appearance room changes. */
class AppearanceActions(
    val onImmersiveModeChange: (Boolean) -> Unit,
    val onKeepScreenOnChange: (Boolean) -> Unit,
    val onTintChange: (Long?) -> Unit,
    val onFontChange: (String) -> Unit,
    val onFontPicked: (Uri) -> Unit,
    val onWallpaperPicked: (Uri) -> Unit,
    val onRemoveWallpaper: () -> Unit,
    val onWallpaperDimChange: (Int) -> Unit,
    val onPinchZoomEnabledChange: (Boolean) -> Unit,
    val onOutputFontSizeChange: (Int) -> Unit,
    val onBubbleFontSizeChange: (Int) -> Unit,
    val onOpenGlassTuning: () -> Unit,
    val onOpenGlow: () -> Unit,
)

/** Colours offered as the tint of buttons and panels. */
private val TINTS = listOf(
    0xFF5B8DEFL, // blue
    0xFF7C5CFFL, // violet
    0xFFE0559BL, // pink
    0xFFE5534BL, // red
    0xFFF0A13AL, // orange
    0xFFE8D44DL, // yellow
    0xFF4CC38AL, // green
    0xFF3FC1C9L, // teal
)

private val SWATCH = 34.dp

/**
 * How Ayana looks, in one room: the screen, the material of the surfaces (glass, or one solid
 * tone) with its colour, the text, and the wallpaper. The colour sits outside the glass switch on
 * purpose: it tints the glass when glass is on and the solid tone when it is off.
 */
@Composable
internal fun AppearancePane(state: AppearanceState, actions: AppearanceActions) {
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.onFontPicked(uri)
    }
    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.onWallpaperPicked(uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AyanaScrollHeader(stringResource(R.string.settings_section_appearance))

        // The screen.
        SettingsCard {
            SwitchRow(
                title = stringResource(R.string.settings_immersive),
                subtitle = stringResource(R.string.settings_immersive_desc),
                checked = state.immersiveMode,
                onChange = actions.onImmersiveModeChange,
            )
            SettingsDivider()
            SwitchRow(
                title = stringResource(R.string.appearance_keep_screen_on),
                subtitle = stringResource(R.string.appearance_keep_screen_on_desc),
                checked = state.keepScreenOn,
                onChange = actions.onKeepScreenOnChange,
            )
        }

        // The material of the surfaces, and its colour.
        SettingsCard {
            SwitchRow(
                title = stringResource(R.string.glass_toggle),
                subtitle = stringResource(R.string.glass_toggle_desc),
                checked = GlassTuning.enabled,
                onChange = { GlassTuning.enabled = it },
            )
            SettingsDivider()
            SettingsEntryRow(
                icon = Icons.Default.BlurOn,
                title = stringResource(R.string.glass_open),
                onClick = actions.onOpenGlassTuning,
            )
            SettingsDivider()
            SettingsEntryRow(
                icon = Icons.Default.LightMode,
                title = stringResource(R.string.settings_glow_section),
                onClick = actions.onOpenGlow,
            )
            SettingsDivider()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.appearance_tint), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.appearance_tint_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TintSwatch(color = null, selected = state.tintArgb == null) { actions.onTintChange(null) }
                    TINTS.forEach { argb ->
                        TintSwatch(color = Color(argb), selected = state.tintArgb == argb) { actions.onTintChange(argb) }
                    }
                }
            }
        }

        // The text.
        SettingsCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.appearance_font), style = MaterialTheme.typography.bodyLarge)
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        FONT_SYSTEM to R.string.appearance_font_system,
                        FONT_MONO to R.string.appearance_font_mono,
                        FONT_SERIF to R.string.appearance_font_serif,
                    ).forEach { (font, label) ->
                        FilterChip(
                            selected = state.font == font,
                            onClick = { actions.onFontChange(font) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                    // Without a file yet, the custom choice goes straight to picking one.
                    FilterChip(
                        selected = state.font == FONT_CUSTOM,
                        onClick = {
                            if (state.hasCustomFont) actions.onFontChange(FONT_CUSTOM) else pickFont.launch(arrayOf("*/*"))
                        },
                        label = { Text(stringResource(R.string.appearance_font_custom)) },
                    )
                }
                if (state.font == FONT_CUSTOM) {
                    TextButton(
                        onClick = { pickFont.launch(arrayOf("*/*")) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(stringResource(R.string.appearance_font_change))
                    }
                }
            }
            SettingsDivider()
            SliderRow(
                title = stringResource(R.string.settings_font_size),
                value = state.outputFontSizeSp,
                range = 10f..22f,
                onChange = actions.onOutputFontSizeChange,
            )
            SettingsDivider()
            SliderRow(
                title = stringResource(R.string.settings_bubble_size),
                value = state.bubbleFontSizeSp,
                range = 12f..24f,
                onChange = actions.onBubbleFontSizeChange,
            )
            SettingsDivider()
            SwitchRow(
                title = stringResource(R.string.settings_pinch_zoom),
                subtitle = stringResource(R.string.settings_pinch_zoom_desc),
                checked = state.pinchZoomEnabled,
                onChange = actions.onPinchZoomEnabledChange,
            )
        }

        // The wallpaper behind the transcript; the glass refracts it like any other content.
        SettingsCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.appearance_wallpaper),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                if (state.hasWallpaper) {
                    TextButton(onClick = actions.onRemoveWallpaper) {
                        Text(stringResource(R.string.appearance_wallpaper_remove))
                    }
                }
                TextButton(onClick = { pickWallpaper.launch(arrayOf("image/*")) }) {
                    Text(
                        stringResource(
                            if (state.hasWallpaper) R.string.appearance_wallpaper_change else R.string.appearance_wallpaper_choose,
                        ),
                    )
                }
            }
            if (state.hasWallpaper) {
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.appearance_wallpaper_dim),
                    value = state.wallpaperDimPct,
                    range = 0f..90f,
                    onChange = actions.onWallpaperDimChange,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** One colour to pick; [color] null is "no tint", drawn as an empty circle crossed out. */
@Composable
private fun TintSwatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(SWATCH)
            .clip(CircleShape)
            .then(if (color != null) Modifier.background(color) else Modifier)
            .border(
                if (selected) BorderStroke(2.dp, ring) else BorderStroke(1.dp, outline),
                CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (color == null) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = size.width * 0.28f
                drawLine(
                    color = outline,
                    start = Offset(size.width - inset, inset),
                    end = Offset(inset, size.height - inset),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
