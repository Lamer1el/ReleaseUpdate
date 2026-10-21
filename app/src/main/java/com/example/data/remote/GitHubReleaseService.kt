package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class GitHubAsset(
    val id: Long = 0,
    val name: String = "",
    val size: Long = 0,
    @Json(name = "browser_download_url")
    val browserDownloadUrl: String = "",
    @Json(name = "content_type")
    val contentType: String? = null
)

data class GitHubRelease(
    val id: Long = 0,
    @Json(name = "tag_name")
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @Json(name = "published_at")
    val publishedAt: String? = null,
    @Json(name = "html_url")
    val htmlUrl: String = "",
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

interface GitHubReleaseApi {

    @GET("repos/Lamer1el/ReleaseUpdate/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>

    @GET("repos/Lamer1el/ReleaseUpdate/releases")
    suspend fun getAllReleases(): Response<List<GitHubRelease>>

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): GitHubReleaseApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "Skitefi-Android-Benchmark")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubReleaseApi::class.java)
        }
    }
}
