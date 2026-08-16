package com.medialtube.app.data.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class AddRequest(
    @SerializedName("url") val url: String,
    @SerializedName("quality") val quality: String = "best",
    @SerializedName("format") val format: String = "any"
)

data class AddResponse(
    @SerializedName("status") val status: String?
)

data class HistoryResponse(
    @SerializedName("queue") val queue: Map<String, DownloadItem>? = emptyMap(),
    @SerializedName("done") val done: Map<String, DownloadItem>? = emptyMap()
)

data class DownloadItem(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("progress") val progress: Double?, 
    @SerializedName("speed") val speed: Double?, 
    @SerializedName("error") val error: String?
)

interface MeTubeApi {
    @POST("add")
    suspend fun addDownload(@Body request: AddRequest): Response<AddResponse>

    // Исправлено: MeTube принимает запросы по адресу /history
    @GET("history")
    suspend fun getHistory(): Response<HistoryResponse>
}

object NetworkClient {
    fun createApi(baseUrl: String): MeTubeApi {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MeTubeApi::class.java)
    }
}
