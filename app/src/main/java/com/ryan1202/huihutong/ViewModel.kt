package com.ryan1202.huihutong

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
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

class HuiHuTongViewModel : ViewModel() {
    var selectedItem = mutableIntStateOf(0)
    val items = listOf("Home", "QRCode")


    private val _latestRelease = MutableStateFlow<GithubRelease?>(null)
    val latestRelease: StateFlow<GithubRelease?> = _latestRelease

    var openID = mutableStateOf("")
        private set

    var qrBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var satoken by mutableStateOf("")
        private set
    var userName by mutableStateOf("")
        private set

    val showUpdateDialog = mutableStateOf(false)

    fun checkForUpdates(versionName: String) {
        viewModelScope.launch {
            val release = fetchLatestRelease()
            if (release != null && checkVersion(versionName, release.tagName)) {
                _latestRelease.value = release
            }
        }
    }

    fun navigate_item(nav_controller: NavController, index: Int) {
        nav_controller.navigate(items[index])
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
                            satoken = json.getString("token")
                        } else {
                            throw JSONException("'data' is null")
                        }
                        if (satoken == "") {
                            throw JSONException("'satoken' is null")
                        }
                        userName = json.getString("name")
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
            isLoading = true

            var data: String? = null
            withContext(Dispatchers.IO) {
                var tmp_data: String? = null
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.215123.cn/pms/welcome/make-qrcode")
                        .addHeader("satoken", satoken)
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

            qrBitmap = data?.let { generateQRCode(it) }
            isLoading = false
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