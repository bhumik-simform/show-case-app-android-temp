package com.example.recipeapp.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit

// Spoonacular's free tier caps each key at a daily request quota; once hit, the API
// responds 402. Rather than someone manually swapping SPOONACULAR_API_KEY_INDEX by hand,
// this rotator cycles to the next key on 402 and remembers the last working index so
// later requests don't retry keys already known to be exhausted today.
private object SpoonacularKeyRotator {
    private val keys = arrayOf(
        "c2767743e1f54f828fd0f5f5ce1428be",
        "65c419295e3e43509b01d5a7720f3e43",
        "ed46c147ed734413b3b10e16a8fa0b93",
        "7355913421ea473d9889c7c50442c78a"
    )
    private val currentIndex = AtomicInteger(0)

    val keyCount: Int get() = keys.size

    fun currentKey(): String = keys[currentIndex.get()]

    fun rotateToNext(): String = keys[currentIndex.updateAndGet { (it + 1) % keys.size }]
}

object RetrofitClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()

    // Plain instance with no interceptor/authenticator — used only by TokenAuthenticator
    // to perform the refresh call itself without re-triggering authentication.
    val dummyJsonPlain: Retrofit = Retrofit.Builder()
        .baseUrl("https://dummyjson.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    // Built per-client so the Bearer token interceptor + auto-refresh authenticator
    // (which both need SessionStorage) can be Koin-injected instead of hardcoded here.
    fun dummyJson(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://dummyjson.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private fun requestWithKey(original: Request, apiKey: String): Request {
        val url = original.url.newBuilder()
            .removeAllQueryParameters("apiKey")
            .addQueryParameter("apiKey", apiKey)
            .build()
        return original.newBuilder().url(url).build()
    }

    private val spoonacularClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            var response = chain.proceed(requestWithKey(original, SpoonacularKeyRotator.currentKey()))

            // On quota exhaustion (402), rotate through the remaining keys before giving up.
            // If every key is exhausted, the final 402 propagates up to FallbackRecipeRepository,
            // which swaps in the bundled dummy recipe data.
            var attempt = 1
            while (response.code == 402 && attempt < SpoonacularKeyRotator.keyCount) {
                response.close()
                val nextKey = SpoonacularKeyRotator.rotateToNext()
                response = chain.proceed(requestWithKey(original, nextKey))
                attempt++
            }
            response
        }
        .build()

    val spoonacular: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.spoonacular.com/")
        .client(spoonacularClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
}