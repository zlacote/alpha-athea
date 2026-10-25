package iatheiya.ayana.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import iatheiya.ayana.ui.theme.Ui
import kotlinx.coroutines.launch

/** One option of an [AyanaChoiceSheet]. */
data class SheetChoice<T>(val value: T, val label: String, val icon: ImageVector? = null)

private val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val OPTION_SHAPE = RoundedCornerShape(24.dp)

/**
 * The one way a value is chosen from a few options: a sheet that rises from the bottom with the
 * options as large rows, the current one filled and ticked. It is as tall as its options, never the
 * whole screen. A drag down or a tap outside puts it away; picking an option puts it away too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AyanaChoiceSheet(
    title: String,
    choices: List<SheetChoice<T>>,
    selected: T?,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = SHEET_SHAPE,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = Ui.spaceXl, end = Ui.spaceXl, bottom = Ui.spaceL),
        )
        Column(
            Modifier
                .padding(horizontal = Ui.spaceL)
                .padding(bottom = Ui.spaceXl)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Ui.spaceS),
        ) {
            choices.forEach { choice ->
                val current = choice.value == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(OPTION_SHAPE)
                        .background(if (current) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                        .border(
                            BorderStroke(1.dp, if (current) Color.Transparent else MaterialTheme.colorScheme.outline),
                            OPTION_SHAPE,
                        )
                        .clickable {
                            scope.launch { state.hide() }.invokeOnCompletion {
                                onPick(choice.value)
                                onDismiss()
                            }
                        }
                        .padding(horizontal = Ui.spaceXl, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    choice.icon?.let {
                        Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(Ui.spaceL))
                    }
                    Text(
                        choice.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (current) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
