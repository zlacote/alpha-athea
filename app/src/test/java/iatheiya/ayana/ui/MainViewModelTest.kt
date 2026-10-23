package iatheiya.ayana.ui

import iatheiya.ayana.core.model.PREVIEW_LINES
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {

    @Test
    fun `previewLines defaults to PREVIEW_LINES`() {
        // UiState is pure data, can be tested without Android runtime
        assertEquals(PREVIEW_LINES, UiState().workspace.settings.previewLines)
    }
}
