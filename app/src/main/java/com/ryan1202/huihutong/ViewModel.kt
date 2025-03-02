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
    fun getSaToken() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // 认证登录
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.215123.cn/web-app/auth/certificateLogin?openId=${openID.value}")
                        .build()
                    val call = client.newCall(request)
                    val response = call.execute()
                    val tmp_data = response.body()?.string()
                    if (tmp_data != null) {
                        Log.e("getSync", tmp_data)
                    }

                    try {
                        val json = tmp_data?.let { JSONObject(it).getJSONObject("data") }
                        if (json != null) {
                            saToken = json.getString("token")
                        } else {
                            throw JSONException("'data' is null")
                        }
                        if (saToken == "") {
                            throw JSONException("'satoken' is null")
                        }
                        qrCodeInfo.value.userName = json.getString("name")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // 获取完satoken后自动获取二维码
                fetchQRCode()
            }
        }
    }

    fun fetchQRCode() {
        viewModelScope.launch {
            isLoading.value = true

            var data: String? = null
            withContext(Dispatchers.IO) {
                var tmp_data: String? = null
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.215123.cn/pms/welcome/make-qrcode")
                        .addHeader("satoken", saToken)
                        .build()
                    val response = client.newCall(request).execute()
                    tmp_data = response.body()?.string()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    data = tmp_data?.let { JSONObject(it).getString("data") }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            qrCodeInfo.value.qrBitmap = data?.let { generateQRCode(it) }
            isLoading.value = false
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