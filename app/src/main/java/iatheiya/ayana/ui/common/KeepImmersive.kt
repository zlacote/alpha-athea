package iatheiya.ayana.ui.common

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Full screen is on (Appearance): the status bar stays hidden. */
val LocalImmersive = staticCompositionLocalOf { false }

/**
 * Called inside a dialog's content. A dialog is a window of its own, and a new window shows the
 * status bar again even when the app hides it; this hides it there too while full screen is on.
 */
@Composable
fun KeepImmersive() {
    if (!LocalImmersive.current) return
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val view = LocalView.current
    SideEffect { setStatusBarHidden(window, view, hidden = true) }
}

/** Hides the status bar of [window] (a swipe from the top brings it back for a moment), or shows it. */
fun setStatusBarHidden(window: Window, view: View, hidden: Boolean) {
    WindowCompat.getInsetsController(window, view).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) hide(WindowInsetsCompat.Type.statusBars()) else show(WindowInsetsCompat.Type.statusBars())
    }
}
