package com.ryan1202.huihutong

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun LinkButton(text: String, url: String) {
    val context = LocalContext.current
    TextButton(onClick = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "没有找到可以打开链接的应用", Toast.LENGTH_SHORT).show()
        }
    }) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary
        )
    }
}