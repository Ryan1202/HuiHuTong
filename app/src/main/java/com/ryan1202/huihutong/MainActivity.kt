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
        setContent {
            HuihutongTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val navController = rememberNavController()
    val viewModel = HuiHuTongViewModel()

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
    // 获取保存的 openid
    val openId = prefs.getString("openid", null)
    LaunchedEffect(openId) {
        if (openId != null) {
            viewModel.openID.value = openId
        }
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        versionName?.let {
            viewModel.checkForUpdates(it)
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(
                    durationMillis = 300
                ),
                initialOffsetX = { fullWidth -> fullWidth }
            )
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 300
                ),
                targetOffsetX = { fullWidth -> fullWidth }
            )
        },
    ) {
        composable(
            "main",
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            }
        ) {
            MainView(viewModel, navController, prefs)
        }
        composable("settings") {
            SettingsView(viewModel, navController)
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