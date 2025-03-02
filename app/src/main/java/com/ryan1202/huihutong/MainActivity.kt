package com.ryan1202.huihutong

import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ryan1202.huihutong.ui.theme.HuihutongTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            HuihutongTheme {
                App()
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Main: Screen("main")
    object Settings: Screen("settings")
}

@Composable
private fun App() {
    val navController = rememberNavController()
    val viewModel = HuiHuTongViewModel()

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
    val settings = context.getSharedPreferences("settings", MODE_PRIVATE)
    // 获取保存的 openid
    val openId = prefs.getString("openid", null)
    if (!settings.contains(SettingConfig.detectLatestVersionKey)) {
        settings.edit().putBoolean(SettingConfig.detectLatestVersionKey, true).apply()
    }
    val detectLatestVersion = settings.getBoolean(SettingConfig.detectLatestVersionKey, true)

    LaunchedEffect(openId) {
        if (openId != null) {
            viewModel.openID.value = openId
        }
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        versionName?.let {
            viewModel.checkForUpdates(it, detectLatestVersion)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        enterTransition = {
            slideInHorizontally(tween(200), initialOffsetX = { it / 2 }) +
                    fadeIn(tween(200))
        },
        exitTransition = {
            slideOutHorizontally(tween(200), targetOffsetX = { it / 2 }) +
                    fadeOut(tween(200))
        }
    ) {
        composable(
            Screen.Main.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            MainView(viewModel,
                onSettingButton = { navController.navigate(Screen.Settings.route) },
                prefs = prefs)
        }
        composable(Screen.Settings.route) {
            SettingsView(navController, settings)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HuihutongTheme {
        App()
    }
}