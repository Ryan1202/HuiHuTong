package com.ryan1202.huihutong

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

data class QRCode(
    var qrBitmap: Bitmap?,
    var userName: String
)

class HuiHuTongViewModel : ViewModel() {
    private val _latestRelease = MutableStateFlow<GithubRelease?>(null)
    val latestRelease: StateFlow<GithubRelease?> = _latestRelease

    var openID = mutableStateOf("")
        private set

    private var saToken by mutableStateOf("")

    var isLoading = mutableStateOf(false)
    var qrCodeInfo = mutableStateOf(QRCode(null, ""))
        private set

    fun checkForUpdates(versionName: String, enable: Boolean) {
        if (enable) {
            viewModelScope.launch {
                val release = fetchLatestRelease()
                if (release != null && checkVersion(versionName, release.tagName)) {
                    _latestRelease.value = release
                }
            }
        }
    }

    fun setOpenID(openID: String, prefs: SharedPreferences) {
        this.openID.value = openID
        val editor = prefs.edit()
        editor.putString("openid", openID)
        editor.apply()
    }
    suspend fun getSaToken() = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.215123.cn/web-app/auth/certificateLogin?openId=${openID.value}")
                .build()
            val response = client.newCall(request).execute()
            val tmpData = response.body()?.string().orEmpty()

            val json = JSONObject(tmpData).optJSONObject("data")
                ?: throw JSONException("'data' is null")

            val token = json.optString("token")
            if (token.isBlank()) throw JSONException("'sa-token' is blank")

            saToken = token
            qrCodeInfo.value = qrCodeInfo.value.copy(userName = json.optString("name", ""))
        } catch (e: Exception) {
            Log.e("GetSaToken", "Error retrieving sa-token", e)
        }
    }

    fun fetchQRCode() {
        if (saToken.isEmpty()) return
        if (isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true

            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.215123.cn/pms/welcome/make-qrcode")
                    .addHeader("satoken", saToken)
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body()?.string().orEmpty()

                val data = JSONObject(body).optString("data", "")
                if (!data.isNullOrEmpty() && data != "null") {
                    val bitmap = generateQRCode(data)
                    withContext(Dispatchers.Main) {
                        qrCodeInfo.value.qrBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                Log.e("FetchQRCode", "Error fetching QR code", e)
            }
        }
    }
}

fun generateQRCode(text: String): Bitmap? {
    val barcodeEncoder = BarcodeEncoder()
    var bitmap: Bitmap? = null
    try {
        bitmap = barcodeEncoder.encodeBitmap(
            text,
            com.google.zxing.BarcodeFormat.QR_CODE,
            300,
            300)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return bitmap
}