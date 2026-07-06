package com.fittrack.app.data.remote

import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val size: String? = null
)

@Serializable
data class DriveFileList(
    val files: List<DriveFile> = emptyList()
)

/** Subconjunto da API do Google Drive v3 usado para backups no appDataFolder. */
interface DriveApiService {

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authorization: String,
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("fields") fields: String = "files(id,name,modifiedTime,size)",
        @Query("orderBy") orderBy: String = "modifiedTime desc",
        @Query("pageSize") pageSize: Int = 50
    ): DriveFileList

    /** Upload multipart/related: parte 1 = metadados JSON, parte 2 = conteúdo. */
    @POST("upload/drive/v3/files")
    suspend fun uploadFile(
        @Header("Authorization") authorization: String,
        @Body body: RequestBody,
        @Query("uploadType") uploadType: String = "multipart"
    ): DriveFile

    @Streaming
    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): ResponseBody

    @DELETE("drive/v3/files/{fileId}")
    suspend fun deleteFile(
        @Header("Authorization") authorization: String,
        @Path("fileId") fileId: String
    ): Response<Unit>
}
