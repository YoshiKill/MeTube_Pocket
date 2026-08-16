package com.medialtube.app.data.api

import com.google.gson.JsonObject
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

data class DownloadItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("percent") val percent: Double? = 0.0, 
    @SerializedName("speed") val speed: Double? = 0.0, 
    @SerializedName("error") val error: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("quality") val quality: String? = null
)

data class IdsRequest(
    @SerializedName("ids") val ids: List<String>
)

interface MeTubeApi {
    @POST("add")
    suspend fun addDownload(@Body request: AddRequest): Response<AddResponse>

    // Возвращаем сырой JsonObject для ручного вытаскивания UUID
    @GET("history")
    suspend fun getHistory(): Response<JsonObject>

    // Единственный верный метод удаления в MeTube
    @POST("delete")
    suspend fun deleteDownloads(@Body request: IdsRequest): Response<AddResponse>

    // Метод для повторного запуска
    @POST("start")
    suspend fun retryDownloads(@Body request: IdsRequest): Response<AddResponse>
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
