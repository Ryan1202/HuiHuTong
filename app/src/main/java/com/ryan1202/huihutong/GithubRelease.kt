package com.ryan1202.huihutong

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class GithubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String
)

suspend fun fetchLatestRelease():GithubRelease? =
    withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/Ryan1202/HuiHuTong/releases/latest"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val jsonString = response.body()?.string() ?: return@withContext null
            return@withContext try {
                val json = JSONObject(jsonString)
                val tagName = json.getString("tag_name")
                val name = json.optString("name")
                val body = json.optString("body")
                val htmlUrl = json.getString("html_url")
                val publishedAt = json.optString("published_at")
                GithubRelease(tagName, name, body, publishedAt, htmlUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

