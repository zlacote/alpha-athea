package iatheiya.ayana.ui.main

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.AWAKE_ALWAYS
import iatheiya.ayana.data.AWAKE_OFF
import iatheiya.ayana.data.AWAKE_RUNNING
import iatheiya.ayana.data.FONT_CUSTOM
import iatheiya.ayana.data.FONT_MONO
import iatheiya.ayana.data.FONT_SERIF
import iatheiya.ayana.data.FONT_SYSTEM
import iatheiya.ayana.data.TINT_COLOR
import iatheiya.ayana.data.TINT_DEFAULT
import iatheiya.ayana.data.TINT_SYSTEM
import iatheiya.ayana.ui.common.AyanaDialogContainer
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.theme.Ui

/** What the Appearance room shows. */
data class AppearanceState(
    val immersiveMode: Boolean,
    val screenAwake: String,
    val tintMode: String,
    val tintArgb: Long?,
    val font: String,
    val hasCustomFont: Boolean,
    val hasWallpaper: Boolean,
    val wallpaperDimPct: Int,
    val wallpaperBlurDp: Int,
    val pinchZoomEnabled: Boolean,
    val outputFontSizeSp: Int,
    val bubbleFontSizeSp: Int,
)

/** What the Appearance room changes. */
class AppearanceActions(
    val onImmersiveModeChange: (Boolean) -> Unit,
    val onScreenAwakeChange: (String) -> Unit,
    val onTintChange: (mode: String, argb: Long?) -> Unit,
    val onFontChange: (String) -> Unit,
    val onFontPicked: (Uri) -> Unit,
    val onWallpaperPicked: (Uri) -> Unit,
    val onRemoveWallpaper: () -> Unit,
    val onWallpaperDimChange: (Int) -> Unit,
    val onWallpaperBlurChange: (Int) -> Unit,
    val onPinchZoomEnabledChange: (Boolean) -> Unit,
    val onOutputFontSizeChange: (Int) -> Unit,
    val onBubbleFontSizeChange: (Int) -> Unit,
    val onOpenGlassTuning: () -> Unit,
    val onOpenGlow: () -> Unit,
    val onReset: () -> Unit,
)

/** The basic colours offered for buttons and panels. */
private val BASIC_TINTS = listOf(
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

/** The phone's own accent (Android 12 and later), or null where the system has none to give. */
@Composable
internal fun systemAccent(): Color? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(LocalContext.current).primary else null

/**
 * How Ayana looks, in one room: the screen, the surfaces (glass, glow), their colour, the text and
 * the wallpaper. The colour has a card of its own and sits outside glass on purpose: it tints the
 * glass when glass is on and the solid tone when it is off.
 */
@Composable
internal fun AppearancePane(state: AppearanceState, actions: AppearanceActions, scrollState: ScrollState) {
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.onFontPicked(uri)
    }
    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.onWallpaperPicked(uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
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
            ChoiceRow(
                title = stringResource(R.string.appearance_screen_awake),
                choices = listOf(
                    AWAKE_OFF to R.string.appearance_screen_awake_off,
                    AWAKE_RUNNING to R.string.appearance_screen_awake_running,
                    AWAKE_ALWAYS to R.string.appearance_screen_awake_always,
                ),
                selected = state.screenAwake,
                onSelect = actions.onScreenAwakeChange,
            )
        }

        // The surfaces.
        SettingsEntryGroup {
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
        }

        // Their colour.
        SettingsCard { TintChooser(state, actions) }

        // The text, everywhere in the app.
        SettingsCard {
            Column(Modifier.padding(horizontal = Ui.spaceL, vertical = Ui.spaceM)) {
                Text(stringResource(R.string.appearance_font), style = MaterialTheme.typography.bodyLarge)
                Row(
                    Modifier
                        .padding(top = Ui.spaceS)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Ui.spaceS),
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
                    .padding(start = Ui.spaceL, end = Ui.spaceS, top = Ui.spaceXs, bottom = Ui.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.appearance_wallpaper),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Ui.controlGap),
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
                // The blur needs Android 12; before it the slider would do nothing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider()
                    SliderRow(
                        title = stringResource(R.string.appearance_wallpaper_blur),
                        value = state.wallpaperBlurDp,
                        range = 0f..40f,
                        onChange = actions.onWallpaperBlurChange,
                    )
                }
            }
        }

        // Defaults at the very end, centred and bare, like the Glass and Keys screens.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = Ui.spaceS),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(onClick = actions.onReset) {
                Text(stringResource(R.string.glass_reset))
            }
        }
        Spacer(Modifier.height(Ui.spaceXl))
    }
}

/** A titled row of chips, one of which is chosen. */
@Composable
private fun ChoiceRow(
    title: String,
    choices: List<Pair<String, Int>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = Ui.spaceL, vertical = Ui.spaceM)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier
                .padding(top = Ui.spaceS)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Ui.spaceS),
        ) {
            choices.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}

/**
 * The colour of buttons and panels: Ayana's own base tone, the phone's accent, one of the basic
 * colours, or any colour at all from the picker.
 */
@Composable
private fun TintChooser(state: AppearanceState, actions: AppearanceActions) {
    var picking by remember { mutableStateOf(false) }
    val system = systemAccent()
    val chosen = state.tintArgb?.let { Color(it) }
    val custom = state.tintMode == TINT_COLOR && state.tintArgb !in BASIC_TINTS
    Column(Modifier.padding(horizontal = Ui.spaceL, vertical = Ui.spaceM)) {
        Text(stringResource(R.string.appearance_tint), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.appearance_tint_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier
                .padding(top = Ui.spaceM)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Ui.spaceS),
        ) {
            // Ayana's own base: the neutral tone the surfaces have without a colour.
            Swatch(
                fill = Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)),
                selected = state.tintMode == TINT_DEFAULT,
            ) { actions.onTintChange(TINT_DEFAULT, null) }
            if (system != null) {
                Swatch(
                    fill = Brush.linearGradient(listOf(system, system)),
                    selected = state.tintMode == TINT_SYSTEM,
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                ) { actions.onTintChange(TINT_SYSTEM, null) }
            }
            BASIC_TINTS.forEach { argb ->
                Swatch(
                    fill = Brush.linearGradient(listOf(Color(argb), Color(argb))),
                    selected = state.tintMode == TINT_COLOR && state.tintArgb == argb,
                ) { actions.onTintChange(TINT_COLOR, argb) }
            }
            // Any colour: the picked one when it is not a basic colour, a rainbow until then.
            Swatch(
                fill = if (custom && chosen != null) {
                    Brush.linearGradient(listOf(chosen, chosen))
                } else {
                    Brush.sweepGradient(RAINBOW)
                },
                selected = custom,
                icon = { Icon(Icons.Default.Colorize, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) },
            ) { picking = true }
        }
        Text(
            text = when (state.tintMode) {
                TINT_SYSTEM -> stringResource(R.string.appearance_tint_system)
                TINT_COLOR -> chosen?.let { "#%06X".format(it.toArgb() and 0xFFFFFF) } ?: ""
                else -> stringResource(R.string.appearance_tint_default)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Ui.spaceS),
        )
    }
    if (picking) {
        ColorPickerDialog(
            initial = chosen ?: Color(BASIC_TINTS.first()),
            onDismiss = { picking = false },
            onPick = { color ->
                picking = false
                actions.onTintChange(TINT_COLOR, color.toArgb().toLong() and 0xFFFFFFFFL)
            },
        )
    }
}

private val RAINBOW = listOf(
    Color(0xFFFF4D4D), Color(0xFFFFC44D), Color(0xFF8DFF4D), Color(0xFF4DFFD2),
    Color(0xFF4D8DFF), Color(0xFFC44DFF), Color(0xFFFF4D4D),
)

@Composable
private fun Swatch(
    fill: Brush,
    selected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(SWATCH)
            .clip(CircleShape)
            .background(fill)
            .border(
                if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon?.invoke()
    }
}

/** Any colour, as hue, saturation and brightness, with the result shown as it is chosen. */
@Composable
private fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val start = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableStateOf(start[0]) }
    var saturation by remember { mutableStateOf(start[1]) }
    var brightness by remember { mutableStateOf(start[2]) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    AyanaDialogContainer(
        title = stringResource(R.string.appearance_tint_pick),
        onDismiss = onDismiss,
        content = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color),
            )
            PickerSlider(stringResource(R.string.appearance_tint_hue), hue, 0f..360f) { hue = it }
            PickerSlider(stringResource(R.string.appearance_tint_saturation), saturation, 0f..1f) { saturation = it }
            PickerSlider(stringResource(R.string.appearance_tint_brightness), brightness, 0f..1f) { brightness = it }
        },
        confirm = {
            TextButton(onClick = { onPick(color) }) { Text(stringResource(R.string.dialog_ok)) }
        },
    )
}

@Composable
private fun PickerSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = Ui.spaceS)) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
