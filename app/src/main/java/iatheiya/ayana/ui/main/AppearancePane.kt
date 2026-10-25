package iatheiya.ayana.ui.main

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import iatheiya.ayana.ui.common.AyanaChoiceSheet
import iatheiya.ayana.ui.common.SheetChoice
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    val tintSoft: Boolean,
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
    val onTintSoftChange: (Boolean) -> Unit,
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
)

private val SWATCH = 42.dp
private const val SWATCHES_PER_ROW = 6

/** The font chooser's last row: it opens the file picker instead of choosing a font. */
private const val PICK_FONT_FILE = "pick"

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
            val awakeChoices = listOf(
                SheetChoice(AWAKE_OFF, stringResource(R.string.appearance_screen_awake_off)),
                SheetChoice(AWAKE_RUNNING, stringResource(R.string.appearance_screen_awake_running)),
                SheetChoice(AWAKE_ALWAYS, stringResource(R.string.appearance_screen_awake_always)),
            )
            var choosingAwake by remember { mutableStateOf(false) }
            ChoiceSettingRow(
                title = stringResource(R.string.appearance_screen_awake),
                value = awakeChoices.firstOrNull { it.value == state.screenAwake }?.label.orEmpty(),
                onClick = { choosingAwake = true },
            )
            if (choosingAwake) {
                AyanaChoiceSheet(
                    title = stringResource(R.string.appearance_screen_awake),
                    choices = awakeChoices,
                    selected = state.screenAwake,
                    onPick = actions.onScreenAwakeChange,
                    onDismiss = { choosingAwake = false },
                )
            }
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
            val fontChoices = buildList {
                add(SheetChoice(FONT_SYSTEM, stringResource(R.string.appearance_font_system)))
                add(SheetChoice(FONT_MONO, stringResource(R.string.appearance_font_mono)))
                add(SheetChoice(FONT_SERIF, stringResource(R.string.appearance_font_serif)))
                if (state.hasCustomFont) add(SheetChoice(FONT_CUSTOM, stringResource(R.string.appearance_font_custom)))
                add(SheetChoice(PICK_FONT_FILE, stringResource(R.string.appearance_font_change), Icons.Default.FileOpen))
            }
            var choosingFont by remember { mutableStateOf(false) }
            ChoiceSettingRow(
                title = stringResource(R.string.appearance_font),
                value = fontChoices.firstOrNull { it.value == state.font }?.label.orEmpty(),
                onClick = { choosingFont = true },
            )
            if (choosingFont) {
                AyanaChoiceSheet(
                    title = stringResource(R.string.appearance_font),
                    choices = fontChoices,
                    selected = state.font,
                    onPick = { picked ->
                        if (picked == PICK_FONT_FILE) pickFont.launch(arrayOf("*/*")) else actions.onFontChange(picked)
                    },
                    onDismiss = { choosingFont = false },
                )
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

        Spacer(Modifier.height(Ui.spaceXl))
    }
}

/**
 * The colour of buttons and panels. Off (the default), the surfaces carry no colour at all. On,
 * the colour is the phone's own accent, black, grey or white, a basic colour, or any colour from the
 * picker, laid out as one grid of swatches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TintChooser(state: AppearanceState, actions: AppearanceActions) {
    var picking by remember { mutableStateOf(false) }
    val system = systemAccent()
    val on = state.tintMode != TINT_DEFAULT
    val custom = state.tintMode == TINT_COLOR && state.tintArgb !in PRESET_TINTS
    SwitchRow(
        title = stringResource(R.string.appearance_tint),
        subtitle = stringResource(R.string.appearance_tint_desc),
        checked = on,
        onChange = { enable ->
            when {
                !enable -> actions.onTintChange(TINT_DEFAULT, null)
                state.tintArgb != null -> actions.onTintChange(TINT_COLOR, state.tintArgb)
                system != null -> actions.onTintChange(TINT_SYSTEM, null)
                else -> actions.onTintChange(TINT_COLOR, BASIC_TINTS.first())
            }
        },
    )
    if (on) {
        SettingsDivider()
        // Two even rows of six: the accent and the neutrals, then the colours and the picker.
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Ui.spaceL, vertical = Ui.spaceM),
            maxItemsInEachRow = SWATCHES_PER_ROW,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Ui.spaceS),
        ) {
            if (system != null) {
                // The phone's accent: its colour, marked with an A for automatic.
                Swatch(fill = SolidColor(system), selected = state.tintMode == TINT_SYSTEM, mark = { AutoMark() }) {
                    actions.onTintChange(TINT_SYSTEM, null)
                }
            }
            PRESET_TINTS.forEach { argb ->
                Swatch(
                    fill = SolidColor(Color(argb)),
                    selected = state.tintMode == TINT_COLOR && state.tintArgb == argb,
                ) { actions.onTintChange(TINT_COLOR, argb) }
            }
            // Any colour: the picked one once chosen, a colour wheel until then, marked with a +.
            val chosen = state.tintArgb?.let { Color(it) }
            Swatch(
                fill = if (custom && chosen != null) SolidColor(chosen) else Brush.sweepGradient(RAINBOW),
                selected = custom,
                mark = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp)) },
            ) { picking = true }
        }
    }
    if (on) {
        SettingsDivider()
        // The chosen colour exactly, by default; or softened toward the dark surfaces.
        SwitchRow(
            title = stringResource(R.string.appearance_tint_soft),
            subtitle = stringResource(R.string.appearance_tint_soft_desc),
            checked = state.tintSoft,
            onChange = actions.onTintSoftChange,
        )
    }
    if (picking) {
        ColorPickerDialog(
            initial = state.tintArgb?.let { Color(it) } ?: Color(BASIC_TINTS.first()),
            onDismiss = { picking = false },
            onPick = { color ->
                picking = false
                actions.onTintChange(TINT_COLOR, color.toArgb().toLong() and 0xFFFFFFFFL)
            },
        )
    }
}

/** "A" for automatic, in black so it reads on any accent. */
@Composable
private fun AutoMark() {
    Text("A", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.Black)
}

/** The neutral tones first, then the basic colours. */
private val PRESET_TINTS = listOf(0xFF000000L, 0xFF8A8A8AL, 0xFFFFFFFFL) + BASIC_TINTS

private val RAINBOW = listOf(
    Color(0xFFFF4D4D), Color(0xFFFFC44D), Color(0xFF8DFF4D), Color(0xFF4DFFD2),
    Color(0xFF4D8DFF), Color(0xFFC44DFF), Color(0xFFFF4D4D),
)

/** One colour to pick: a disc, with a ring around it when chosen, and an optional mark inside. */
@Composable
private fun Swatch(
    fill: Brush,
    selected: Boolean,
    mark: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(SWATCH)
            .border(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .padding(SWATCH_RING)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        mark?.invoke()
    }
}

private val SWATCH_RING = 4.dp

/**
 * Any colour: typed as a hex code, or set by hue, saturation and brightness, each with its number.
 * The two stay in step, and the result shows as it is chosen.
 */
@Composable
private fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val start = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableStateOf(start[0]) }
    var saturation by remember { mutableStateOf(start[1]) }
    var brightness by remember { mutableStateOf(start[2]) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    var hex by remember { mutableStateOf(hexOf(color)) }
    // The sliders write the code; a complete code typed by hand moves the sliders.
    fun fromSliders() {
        hex = hexOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))))
    }
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
            OutlinedTextField(
                value = hex,
                onValueChange = { typed ->
                    hex = typed.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }.take(7)
                    parseHex(hex)?.let { parsed ->
                        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(parsed.toArgb(), it) }
                        hue = hsv[0]
                        saturation = hsv[1]
                        brightness = hsv[2]
                    }
                },
                label = { Text(stringResource(R.string.appearance_tint_hex)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Ui.spaceM),
            )
            PickerSlider(stringResource(R.string.appearance_tint_hue), hue, 0f..360f, "${hue.toInt()}°") {
                hue = it
                fromSliders()
            }
            PickerSlider(stringResource(R.string.appearance_tint_saturation), saturation, 0f..1f, "${(saturation * 100).toInt()}%") {
                saturation = it
                fromSliders()
            }
            PickerSlider(stringResource(R.string.appearance_tint_brightness), brightness, 0f..1f, "${(brightness * 100).toInt()}%") {
                brightness = it
                fromSliders()
            }
        },
        confirm = {
            TextButton(onClick = { onPick(color) }) { Text(stringResource(R.string.dialog_ok)) }
        },
    )
}

/** "#RRGGBB" for [color]. */
private fun hexOf(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

/** A colour from "#RRGGBB" or "RRGGBB" (also the short "#RGB"); null until the code is complete. */
private fun parseHex(text: String): Color? {
    val digits = text.removePrefix("#")
    val full = when (digits.length) {
        3 -> digits.map { "$it$it" }.joinToString("")
        6 -> digits
        else -> return null
    }
    return full.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

@Composable
private fun PickerSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    shown: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = Ui.spaceS)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(shown, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
