package com.ryan1202.huihutong

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
fun UpdatePrompt(info: GithubRelease, showUpdateDialog: () -> Unit) {
    TextButton(onClick = showUpdateDialog) {
        Text(text = stringResource(R.string.NewVersionAvailable, info.tagName))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAlertDialog(updateInfo: GithubRelease, onDismissRequest: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.Center) {
                Text(
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
                LinkButton("Download",
                    updateInfo.htmlUrl)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUpdateDialog() {
    UpdateAlertDialog(
        GithubRelease("v1.0.0", "HuiHuTong v1.0.0", "update info", "2025.02.23", "https:/github.com/Ryan1202/HuiHuTong/Release"),
        {  }
    )
}
