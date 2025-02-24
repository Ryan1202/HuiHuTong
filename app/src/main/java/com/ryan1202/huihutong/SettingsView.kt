package com.ryan1202.huihutong

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsView(navController: NavController, settings: SharedPreferences) {
    val detectLatestVersion = settings.getBoolean(SettingConfig.detectLatestVersionKey, true)

    Scaffold(
        topBar = {
            TopBar(navController)
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column (
                modifier = Modifier.padding(8.dp)
            ){
                BooleanSetting("检测最新版本", detectLatestVersion) {
                    settings.edit().putBoolean(SettingConfig.detectLatestVersionKey, it).apply()
                }

                HorizontalDivider(Modifier.padding(8.dp))

                Text("作者：王嘉骏")
                Text("Author：Jiajun Wang" )

                LinkButton("https://github.com/Ryan1202/HuiHuTong",
                    "https://github.com/Ryan1202/HuiHuTong")

                Text("感谢 Thanks for: ")
                LinkButton("https://github.com/PairZhu/HuiHuTong",
                    "https://github.com/PairZhu/HuiHuTong")
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

@Composable
private fun BooleanSetting(title: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    var state by rememberSaveable { mutableStateOf(value) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            textAlign = TextAlign.Start,
            fontSize = MaterialTheme.typography.titleMedium.fontSize
        )
        Switch(
            checked = state,
            onCheckedChange = {
                state = it
                onValueChange(it)
            }
        )
    }
}
