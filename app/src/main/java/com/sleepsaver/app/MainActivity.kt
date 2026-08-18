package com.sleepsaver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.sleepsaver.app.ui.AppViewModel
import com.sleepsaver.app.ui.SleepSaverApp
import com.sleepsaver.app.ui.theme.SleepSaverTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SleepSaverTheme {
                SleepSaverApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUsagePermission()
    }
}

