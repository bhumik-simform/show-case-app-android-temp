package com.example.recipeapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Lets ApiErrorHandler check for a live internet connection *before* attempting a network
// call, so the user sees "No internet connection" immediately instead of waiting out a
// connect-timeout. Initialized once from App.onCreate rather than injected everywhere, since
// safeApiCall() is a plain top-level function called from many repositories.
object ConnectivityChecker {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isConnected(): Boolean {
        if (!::appContext.isInitialized) return true
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
