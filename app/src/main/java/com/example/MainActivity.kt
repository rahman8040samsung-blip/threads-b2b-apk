package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import com.example.ui.B2bAppContent
import com.example.ui.B2bViewModel
import com.example.ui.theme.MyTheme
import com.example.update.AppUpdateManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for app updates
        AppUpdateManager.checkForUpdate(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            MyTheme {
                val viewModel: B2bViewModel = viewModel()
                B2bAppContent(viewModel = viewModel)
            }
        }
    }
}
