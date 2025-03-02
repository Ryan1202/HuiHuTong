package com.ryan1202.huihutong

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class MainTab(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    HOME(
        route = "home",
        icon = Icons.Default.Home,
        label = "首页"
    ),
    QR_CODE(
        route = "qrcode",
        icon = Icons.AutoMirrored.Filled.List,
        label = "二维码"
    );

    companion object {
        // 获取所有选项卡的便捷方法
        fun getAllTabs() = entries.toList()
    }
}

@Composable
fun MainView(viewModel: HuiHuTongViewModel, onSettingButton: () -> Unit, prefs: SharedPreferences) {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopBar(onSettingButton) },
        bottomBar = {
            BottomBar(selectedItem){ route ->
                navController.navigate(route)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination =
                if (viewModel.openID.value == "" )
                    { MainTab.HOME.route }
                else { MainTab.QR_CODE.route },
            enterTransition = {
                slideInHorizontally(tween(300)) +
                        fadeIn(tween(300))
            },
            exitTransition = {
                slideOutHorizontally(tween(300)) +
                        fadeOut(tween(300))
            },
            modifier = Modifier
                .padding(innerPadding)

        ) {
            composable(MainTab.HOME.route) {
                selectedItem = MainTab.HOME.route
                HomeView(
                    viewModel.latestRelease,
                    viewModel.openID,
                    {
                        viewModel.setOpenID(it, prefs)
                        navController.navigate(MainTab.QR_CODE.route)
                    }
                )
            }
            composable(MainTab.QR_CODE.route) {
                selectedItem = MainTab.QR_CODE.route
                QRCodeView(
                    viewModel.latestRelease,
                    viewModel.isLoading,
                    viewModel.qrCodeInfo,
                    viewModel.openID,
                    { viewModel.getSaToken() },
                    { viewModel.fetchQRCode() },
                    { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun HomeView(latestRelease: StateFlow<GithubRelease?>,
                     openId: MutableState<String>,
                     onOpenIdChanged: (String) -> Unit) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val updateInfo by latestRelease.collectAsState()
        updateInfo?.let { info ->
            if (showUpdateDialog) {
                UpdateAlertDialog(info) {
                    showUpdateDialog = false
                }
            }
        }
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            updateInfo?.let { info ->
                UpdatePrompt(info) {
                    showUpdateDialog = true
                }
            }
            LinkButton("如何获取OpenID?（可能需要梯子）",
                "https://github.com/PairZhu/HuiHuTong/blob/main/README.md")
            var text by remember { mutableStateOf(openId.value) }
            OutlinedTextField(
                modifier = Modifier.padding(8.dp),
                value = text,
                onValueChange = { text = it },
                label = { Text("OpenID") }
            )
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.width(150.dp),
                onClick = {
                    onOpenIdChanged(text)
                }
            ) {
                Text("确认/修改")
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
private fun QRCodeView(
    latestRelease: StateFlow<GithubRelease?>,
    isLoading: MutableState<Boolean>,
    qrCodeInfo: MutableState<QRCode>,
    openId: MutableState<String>,
    getSaToken: () -> Unit,
    fetchQRCode: () -> Unit,
    navBack: () -> Unit) {

    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(openId) {
        if (openId.value != "") {
            getSaToken()
        } else {
            Toast.makeText(context, "请先填入OpenID", Toast.LENGTH_SHORT).show()
            delay(300)
            navBack()
        }
        while (true) {
            // 10秒刷新一次
            delay(10000)
            fetchQRCode()
        }
    }

    val window = context.findActivity().window
    DisposableEffect(Unit) {
        val originalBrightness = window?.attributes?.screenBrightness
        window.attributes.apply {
            screenBrightness = 1f
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

    val updateInfo by latestRelease.collectAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        updateInfo?.let { info ->
            if (showUpdateDialog) {
                UpdateAlertDialog(info) {
                    showUpdateDialog = false
                }
            }
        }
        val info by qrCodeInfo
        if (isLoading.value && info.qrBitmap == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                updateInfo?.let { info ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        UpdatePrompt(info) {
                            showUpdateDialog = true
                        }
                    }
                }
                CircularProgressIndicator()
            }
        } else {
            info.qrBitmap?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    updateInfo?.let { info ->
                        UpdatePrompt(info) {
                            showUpdateDialog = true
                        }
                    }
                    Text(info.userName,
                        fontSize = MaterialTheme.typography.displaySmall.fontSize)
                    Spacer(Modifier.height(8.dp))
                    Image(
                        it.asImageBitmap(),
                        contentDescription = "二维码",
                        modifier = Modifier.size(300.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    currentRoute: String,
    onClick: (String) -> Unit
) {
    BottomAppBar {
        val tabs = MainTab.getAllTabs()
        tabs.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    onClick(item.route)
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

@Preview(showBackground = true)
@Composable
fun PreviewQRCodeView() {
    val release: StateFlow<GithubRelease?> = MutableStateFlow(GithubRelease("", "", "", "", ""))
    val qrCode = remember { mutableStateOf(QRCode(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), "null")) }
    QRCodeView(
        release,
        remember { mutableStateOf(false) },
        qrCode,
        remember { mutableStateOf("123") },
        { },
        { },
    ) { }
}
