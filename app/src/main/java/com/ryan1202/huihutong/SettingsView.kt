package com.ryan1202.huihutong

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsView(viewModel: HuiHuTongViewModel, navController: NavController) {
    Scaffold(
        topBar = {
            TopBar(navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(8.dp)
            )
            Box(
                modifier = Modifier.padding(8.dp)
            ) {
                Column {
                    Text(
                        "作者：王嘉骏"
                    )
                    Text(
                        "Author：Jiajun Wang"
                    )
                    val context = LocalContext.current
                    Text(
                        modifier = Modifier.clickable() {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ryan1202/HuiHuTong"))
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "没有找到可以打开链接的应用", Toast.LENGTH_SHORT).show()
                            }
                        },
                        text = "https://github.com/Ryan1202/HuiHuTong",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("感谢 Thanks for: ")
                    Text(
                        modifier = Modifier.clickable() {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PairZhu/HuiHuTong"))
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "没有找到可以打开链接的应用", Toast.LENGTH_SHORT).show()
                            }
                        },
                    text = "https://github.com/PairZhu/HuiHuTong",
                    color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavController) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        title = {
            Text("Settings")
        },
    )
}