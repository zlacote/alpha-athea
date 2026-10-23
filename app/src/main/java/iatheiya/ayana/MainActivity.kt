package iatheiya.ayana

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import iatheiya.ayana.ui.MainViewModel
import iatheiya.ayana.ui.main.MainScreen
import iatheiya.ayana.ui.theme.AyanaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark. The default bar style follows the phone's light or dark theme,
        // and in light mode it puts a white scrim under the navigation bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AyanaTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onPause() {
        // Drafts and metadata are cheap to flush here and precious to lose.
        viewModel.persistTransientState()
        super.onPause()
    }
}
