package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class OnlineRateFetchResult {
    data class Success(val ratesPerUsd: Map<String, Double>, val fetchedAtMillis: Long) : OnlineRateFetchResult()
    data class Failure(val reason: String) : OnlineRateFetchResult()
}

class OnlineRateFetcher(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun fetchLatestUsdRates(): OnlineRateFetchResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext OnlineRateFetchResult.Failure("No internet connection. Using offline reference rates.")
        }

        try {
            val request = Request.Builder()
                .url("https://open.er-api.com/v6/latest/USD")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext OnlineRateFetchResult.Failure("Rate service returned HTTP ${response.code}. Offline rates kept.")
                }
                val body = response.body?.string()
                    ?: return@withContext OnlineRateFetchResult.Failure("Empty response. Offline rates kept.")

                val json = JSONObject(body)
                if (json.optString("result") != "success") {
                    return@withContext OnlineRateFetchResult.Failure("Unable to parse online rates. Offline rates kept.")
                }

                val ratesObj = json.getJSONObject("rates")
                val map = mutableMapOf<String, Double>()
                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val rate = ratesObj.optDouble(code, -1.0)
                    if (rate > 0.0) {
                        map[code] = rate
                    }
                }
                OnlineRateFetchResult.Success(map, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            OnlineRateFetchResult.Failure("Network unavailable. Offline reference rates active.")
        }
    }
}
