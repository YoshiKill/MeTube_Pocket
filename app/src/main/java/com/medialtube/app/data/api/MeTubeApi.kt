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
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("format") val format: String? = null
)

data class AddResponse(
    @SerializedName("status") val status: String?
)

data class HistoryResponse(
    @SerializedName("queue") val queue: List<DownloadItem>? = emptyList(),
    @SerializedName("done") val done: List<DownloadItem>? = emptyList(),
    @SerializedName("history") val history: List<DownloadItem>? = emptyList()
)

data class DownloadItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("status") val status: String? = null,
    // ИСПРАВЛЕНО: MeTube использует 'percent' вместо 'progress'
    @SerializedName("percent") val percent: Double? = 0.0, 
    @SerializedName("speed") val speed: Double? = 0.0, 
    @SerializedName("error") val error: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("quality") val quality: String? = null
)

// Запрос для управления загрузками (Отменить/Удалить)
data class ActionRequest(
    @SerializedName("id") val id: String
)

interface MeTubeApi {
    @POST("add")
    suspend fun addDownload(@Body request: AddRequest): Response<AddResponse>

    @GET("history")
    suspend fun getHistory(): Response<HistoryResponse>

    // Новые методы для взаимодействия с загрузками из всплывающего меню
    @POST("cancel")
    suspend fun cancelDownload(@Body request: ActionRequest): Response<AddResponse>

    @POST("clear")
    suspend fun clearDownload(@Body request: ActionRequest): Response<AddResponse>
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
