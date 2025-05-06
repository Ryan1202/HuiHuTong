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
import androidx.compose.ui.res.stringResource
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
    val labelId: Int
) {
    HOME(
        route = "home",
        icon = Icons.Default.Home,
        labelId = R.string.HomePage
    ),
    QR_CODE(
        route = "qrcode",
        icon = Icons.AutoMirrored.Filled.List,
        labelId = R.string.QRCode
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
                    viewModel.openID
                ) {
                    viewModel.setOpenID(it, prefs)
                    navController.navigate(MainTab.QR_CODE.route)
                }
            }
            composable(MainTab.QR_CODE.route) {
                selectedItem = MainTab.QR_CODE.route
                QRCodeView(
                    LocalContext.current,
                    viewModel.latestRelease,
                    viewModel.isLoading,
                    viewModel.qrCodeInfo,
                    viewModel.openID,
                    { viewModel.getSaToken() },
                    { viewModel.fetchQRCode(it) },
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
            LinkButton(
                stringResource(R.string.HowToGetOpenID),
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
                Text(stringResource(R.string.Confirm))
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
    context: Context,
    latestRelease: StateFlow<GithubRelease?>,
    isLoading: MutableState<Boolean>,
    qrCodeInfo: MutableState<QRCode>,
    openId: MutableState<String>,
    getSaToken: suspend () -> Unit,
    fetchQRCode: (Boolean) -> Unit,
    navBack: () -> Unit) {

    val showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(openId) {
        var flag = false
        if (openId.value.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.FillOpenIDPrompt), Toast.LENGTH_SHORT).show()
            delay(300)
            navBack()
        }
        getSaToken()

        // 初次加载时显示进度条
        if (qrCodeInfo.value.qrBitmap == null) flag = true
        fetchQRCode(flag)

        while (true) {
            // 每隔10秒刷新一次
            delay(10_000)
            fetchQRCode(false)
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

    QRCodeViewContent(updateInfo, showUpdateDialog, qrCodeInfo, isLoading) {
        // 手动刷新时显示进度条
        fetchQRCode(true)
    }
}

@Composable
private fun QRCodeViewContent(
    updateInfo: GithubRelease?,
    showUpdateDialog: Boolean,
    qrCodeInfo: MutableState<QRCode>,
    isLoading: MutableState<Boolean>,
    onRefresh: () -> Unit
) {
    var showUpdateDialog1 = showUpdateDialog
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        updateInfo?.let { info ->
            if (showUpdateDialog1) {
                UpdateAlertDialog(info) {
                    showUpdateDialog1 = false
                }
            }
        }
        val info = qrCodeInfo.value
        if (isLoading.value) {
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
                            showUpdateDialog1 = true
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
                            showUpdateDialog1 = true
                        }
                    }
                    Text(
                        info.userName,
                        fontSize = MaterialTheme.typography.displaySmall.fontSize
                    )
                    Spacer(Modifier.height(8.dp))
                    Image(
                        it.asImageBitmap(),
                        contentDescription = stringResource(R.string.QRCode),
                        modifier = Modifier.size(300.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onRefresh() }
                    ) {
                        Text(stringResource(R.string.Refresh))
                    }
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
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelId)) },
                label = { Text(stringResource(item.labelId)) },
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
            Text(stringResource(R.string.app_name))
        },
        actions = {
            IconButton(
                onClick = {
                    settingsOnClick()
                }
            ) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQRCodeView() {
    MaterialTheme {
        val release by MutableStateFlow(GithubRelease("1.0.0", "1.0.0", "", "", "")).collectAsState()
        val qrCode = remember { mutableStateOf(QRCode(Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888), "Test User")) }
        val isLoading = remember { mutableStateOf(false) }
        QRCodeViewContent(
            release,
            false,
            qrCode,
            isLoading
        ) {}
    }
}
