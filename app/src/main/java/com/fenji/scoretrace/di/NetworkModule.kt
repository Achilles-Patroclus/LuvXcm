package com.fenji.scoretrace.di

import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.data.remote.ApiService
import com.fenji.scoretrace.data.remote.deepseek.DeepSeekApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 占位地址，接入真实后端时替换
    private const val BASE_URL = "https://api.example.com/v1/"

    private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // 整次调用（含重定向/重试）的总时限，避免个别接口长时间挂起
            .callTimeout(12, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    // ── DeepSeek 独立网络栈 ──────────────────────────────────────────
    // baseUrl 与占位 ApiService 不同，且需固定 Authorization 头，故单独一套实例。
    // 用 @Named("deepseek") 限定，避免与上面的 OkHttpClient / Retrofit 产生重复绑定。

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // 流式响应日志用 BASIC，避免 BODY 级把 SSE 全量打印出来
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.DEEPSEEK_API_KEY}")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            // 流式输出可能持续较久，读超时放宽到 120s
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(
        @Named("deepseek") client: OkHttpClient,
        gson: Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(DEEPSEEK_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideDeepSeekApiService(
        @Named("deepseek") retrofit: Retrofit,
    ): DeepSeekApiService = retrofit.create(DeepSeekApiService::class.java)
}
