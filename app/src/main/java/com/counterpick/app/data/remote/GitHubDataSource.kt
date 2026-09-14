package com.counterpick.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class FetchResult {
    data class Success(val body: String) : FetchResult()
    data class Failure(val message: String) : FetchResult()
}

class GitHubDataSource(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val noCache = CacheControl.Builder().noCache().noStore().build()

    suspend fun fetchDataJson(url: String): FetchResult = fetch(url)

    suspend fun fetchMetaJson(url: String): FetchResult = fetch(url)

    private suspend fun fetch(url: String): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).cacheControl(noCache).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Failure("HTTP ${response.code} for $url")
                }
                val body = response.body?.string()
                    ?: return@withContext FetchResult.Failure("Empty body for $url")
                FetchResult.Success(body)
            }
        } catch (e: Exception) {
            FetchResult.Failure(e.message ?: "Unknown network error fetching $url")
        }
    }
}
