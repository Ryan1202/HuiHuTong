package com.ryan1202.huihutong

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

fun checkVersion(localVersion: String, remoteVersion: String): Boolean {
    val parts1 = localVersion.trim().removePrefix("v").split(".")
    val parts2 = remoteVersion.trim().removePrefix("v").split(".")
    val maxLength = maxOf(parts1.size, parts2.size)

    var flag = true
    var count = 0
    for (i in 0 until maxLength) {
        val num1 = if (i < parts1.size) parts1[i].toIntOrNull() else 0
        val num2 = if (i < parts2.size) parts2[i].toIntOrNull() else 0
        if (num2!! > num1!!) {
            break
        } else {
            count++
        }
    }
    if (count == maxLength) flag = false
    return flag
}

@Composable
fun UpdatePrompt(info: GithubRelease, viewModel: HuiHuTongViewModel) {
    Column {
        TextButton(
            onClick = {
                viewModel.showUpdateDialog.value = true
            }
        ) {
            Text(text = "新版本可用: ${info.tagName}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAlertDialog(updateInfo: GithubRelease, viewModel: HuiHuTongViewModel) {
    BasicAlertDialog(
        onDismissRequest = {
            viewModel.showUpdateDialog.value = false
        },
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row {
                Text(
                    modifier = Modifier.padding(1.dp),
                    textAlign = TextAlign.Center,
                    text = updateInfo.name,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                )
                Surface(
                    modifier = Modifier.padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            modifier = Modifier.padding(1.dp),
                            text = updateInfo.tagName
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
            Text(
                text = updateInfo.publishedAt,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
            Text(updateInfo.body)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.htmlUrl))
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "没有找到可以打开链接的应用", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        text = "Download",
                    )
                }

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUpdateDialog() {
    UpdateAlertDialog(
        GithubRelease("v1.0.0", "HuiHuTong v1.0.0", "update info", "2025.02.23", "https:/github.com/Ryan1202/HuiHuTong/Release"),
        viewModel = viewModel()
    )
}
