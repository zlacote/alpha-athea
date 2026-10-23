package iatheiya.ayana.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iatheiya.ayana.R
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.ui.theme.codeStyle

/**
 * Dedicated full-screen text selection surface (long-press to select),
 * mirroring the familiar chat-app pattern. Selection is bounded to the
 * given text by design.
 */
@Composable
fun SelectTextScreen(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AyanaScaffold(onBack = onClose, modifier = modifier) {
        AyanaScrollHeader(stringResource(R.string.select_text_title))
        SelectionContainer(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Text(
                text = text,
                style = codeStyle().copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(Ui.screenPadding),
            )
        }
    }
}
