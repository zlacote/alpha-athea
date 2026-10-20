package com.athea.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.athea.app.ui.MainViewModel
import com.athea.app.ui.main.MainScreen
import com.athea.app.ui.theme.AtheaTheme

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
            AtheaTheme {
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
