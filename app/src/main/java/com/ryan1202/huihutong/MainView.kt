package com.ryan1202.huihutong

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

@Composable
fun MainView(viewModel: HuiHuTongViewModel, upperNavController: NavController) {
    val selectedItem = viewModel.selectedItem
    val selectedItemValue by remember {
        selectedItem
    }
    val items = listOf("Home", "QRCode")
    val navController = rememberNavController()
    val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List)

    val context = LocalContext.current
    // 获取保存的 openid
    val prefs = context.getSharedPreferences("config", MODE_PRIVATE)
    val openId = prefs.getString("openid", null)

    if (openId != null) {
        viewModel.openID.value = openId
    }

    Scaffold(
        topBar = {
            TopBar() {
                upperNavController.navigate("settings")
            }
        },
        bottomBar = {
            BottomBar(items, icons, selectedItemValue){ index ->
                viewModel.selectedItem.intValue = index
                navController.navigate(items[index])
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "Home",
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(
                        durationMillis = 300
                    ),
                    initialOffsetX = { fullWidth -> -fullWidth }
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(
                        durationMillis = 300
                    ),
                    targetOffsetX = { fullWidth -> -fullWidth }
                )
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()

        ) {
            composable("Home") {
                HomeView(viewModel, navController, prefs)
            }
            composable("QRCode") {
                QRCodeView(viewModel, navController)
            }
        }
    }
}

@Composable
private fun HomeView(viewModel: HuiHuTongViewModel, navController: NavController, prefs: SharedPreferences) {
    var text by remember { mutableStateOf(viewModel.openID.value) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val context = LocalContext.current
                TextButton(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PairZhu/HuiHuTong/blob/main/README.md"))
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "没有找到可以打开链接的应用", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(
                        text = "如何获取OpenID?（可能需要梯子）",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    modifier = Modifier.padding(8.dp),
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("OpenID") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier.width(150.dp),
                    onClick = {
                        viewModel.setOpenID(text, prefs)
                        viewModel.selectedItem.intValue = 1
                        navController.navigate("QRCode")
                    }
                ) {
                    Text("确认/修改")
                }
            }
        }
    }
}

internal fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}

@Composable
private fun QRCodeView(viewModel: HuiHuTongViewModel, navController: NavController) {
    val openId = viewModel.openID.value
    val context = LocalContext.current
    LaunchedEffect(openId) {
        if (openId != "") {
            viewModel.getSaToken()
        } else {
            Toast.makeText(context, "请先填入OpenID", Toast.LENGTH_SHORT).show()
            delay(300)
            viewModel.selectedItem.intValue = 0
            navController.popBackStack()
        }
        while (true) {
            // 10秒刷新一次
            delay(10000)
            viewModel.fetchQRCode()
        }
    }

    val window = context.findActivity().window
    DisposableEffect(Unit) {
        val originalBrightness = window?.attributes?.screenBrightness
        window.attributes.apply {
            screenBrightness = 1F
            window.attributes = this
        }
        onDispose {
            originalBrightness?.let {
                window.attributes.apply {
                    screenBrightness = originalBrightness
                    window.attributes = this
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (viewModel.isLoading && viewModel.qrBitmap == null) {
            CircularProgressIndicator()
        } else {
            viewModel.qrBitmap?.let {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(viewModel.userName)
                        Image(
                            it.asImageBitmap(),
                            contentDescription = "二维码",
                            modifier = Modifier.size(300.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    items: List<String>,
    icons: List<ImageVector>,
    selectedItem: Int,
    onClick: (Int) -> Unit
) {
    BottomAppBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = {
                    onClick(index)
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(settingsOnClick: () -> Unit) {
    TopAppBar(
        title = {
            Text("HuiHuTong")
        },
        actions = {
            IconButton(
                onClick = {
                    settingsOnClick()
                }
            ) {
                Icon(Icons.Default.Settings, contentDescription = "设置")
            }
        }
    )
}
