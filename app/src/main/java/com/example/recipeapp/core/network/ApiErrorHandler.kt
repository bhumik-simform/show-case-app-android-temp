package com.example.recipeapp.core.network

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
object ApiErrorHandler {
    suspend fun <T> safeApiCall(apiCall: suspend () -> T): NetworkResult<T> {
        // Checked before ever touching the network so an offline device fails fast with a
        // clear message instead of waiting out a connect-timeout first.
        if (!ConnectivityChecker.isConnected()) {
            return NetworkResult.Error("No internet connection", type = NetworkErrorType.NO_INTERNET)
        }
        return try {
            NetworkResult.Success(apiCall())
        } catch (e: CancellationException) {
            // Not a real error: the coroutine was cancelled because a newer request
            // superseded it (e.g. collectLatest on fast typing). Must rethrow so
            // structured concurrency can actually cancel the coroutine.
            throw e
        } catch (e: HttpException) {
            Log.e("ApiError",e.toString())
            NetworkResult.Error(e.localizedMessage ?: "Server error", e.code(), NetworkErrorType.HTTP)
        } catch (e: IOException) {
            Log.e("ApiError",e.toString())
            NetworkResult.Error("No internet connection", type = NetworkErrorType.NO_INTERNET)
        } catch (e: SerializationException) {
            Log.e("ApiError",e.toString())
            NetworkResult.Error("Unexpected response format", type = NetworkErrorType.PARSING)
        } catch (e: Exception) {
            Log.e("ApiError",e.toString())
            NetworkResult.Error(e.localizedMessage ?: "Unknown error", type = NetworkErrorType.UNKNOWN)
        }
    }
}