package com.github.nikp123.racunica.util

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresPermission
import it.skrape.fetcher.NonBlockingFetcher
import it.skrape.fetcher.Request
import it.skrape.fetcher.Result
import okhttp3.OkHttpClient

object OkHttpFetcher : NonBlockingFetcher<Request> {
    override val requestBuilder: Request get() = Request()

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun fetch(request: Request): Result = OkHttpClient().newCall(
        okhttp3.Request.Builder()
            .url(request.url)
            .build()
    ).execute().let {
        val body = it.body!!
        Result(
            responseBody = body.string(),
            responseStatus = Result.Status(it.code, it.message),
            contentType = body.contentType()?.toString()?.replace(" ", ""),
            headers = it.headers.toMap(),
            cookies = emptyList(),
            baseUri = it.request.url.toString()
        )
    }
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.hasInternetConnection(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } else {
        // This is to keep compatibility down to Android SDK 21 (Lollipop)
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        networkInfo != null && networkInfo.isConnected
    }
}