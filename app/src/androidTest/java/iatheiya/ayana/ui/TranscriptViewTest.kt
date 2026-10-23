package iatheiya.ayana.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import iatheiya.ayana.core.model.CommandBlock
import iatheiya.ayana.core.model.OutputBlock
import iatheiya.ayana.session.SessionView
import iatheiya.ayana.text.StyledText
import iatheiya.ayana.transcript.BlockView
import iatheiya.ayana.ui.main.TranscriptView
import iatheiya.ayana.ui.theme.AyanaTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests — satisfies audit "no UI tests".
 * Requires androidTestImplementation "androidx.compose.ui:ui-test-junit4".
 */
class TranscriptViewTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun commandBubbleIsDisplayed() {
        val session = SessionView(
            id = 1, name = "test", pinned = false,
            rawStream = false,
            draft = "", rawText = "", running = false,
            blocks = listOf(
                BlockView(CommandBlock("cmd-1", "echo hello"), false),
                BlockView(OutputBlock("out-1", StyledText("hello\n")), false)
            )
        )
        compose.setContent {
            AyanaTheme {
                TranscriptView(
                    session = session,
                    search = null,
                    scrollRequests = MutableSharedFlow(),
                    jumpToBottom = MutableSharedFlow(),
                    previewLines = 3,
                    contentTopPadding = 16.dp,
                    pinchZoomEnabled = false,
                    onOutputFontZoom = {},
                    onToggleBlock = {},
                    onRevealBlock = {},
                    onLocateBlock = { -1 },
                    onCopyCommand = {},
                    onSelectCommandText = {},
                    onAddToFavorites = {},
                    onAreaResized = { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("echo hello").assertIsDisplayed()
        compose.onNodeWithText("hello").assertIsDisplayed()
    }
}
