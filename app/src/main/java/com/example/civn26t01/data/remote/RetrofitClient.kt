package com.example.civn26t01.data.remote

import android.content.Context
import com.example.civn26t01.core.settings.SettingsManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    fun getInstance(context: Context): Retrofit {
        val baseUrl = SettingsManager.getBaseUrl(context)

        // Recreate nếu base URL thay đổi
        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            retrofit = createRetrofit(baseUrl)
        }

        return retrofit!!
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        // Logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttp client
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Gson
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // Retrofit
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApiService(context: Context): RpaApiService {
        return getInstance(context).create(RpaApiService::class.java)
    }

    // Force recreate (gọi khi settings thay đổi)
    fun reset() {
        retrofit = null
        currentBaseUrl = null
    }
}