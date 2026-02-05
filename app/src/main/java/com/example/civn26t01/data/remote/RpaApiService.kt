package com.example.civn26t01.data.remote

import com.example.civn26t01.data.models.ChiyodaInfo
import com.example.civn26t01.data.models.ProcessResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RpaApiService {

    @POST("/api/rpa/execute")
    suspend fun executeChiyoda(
        @Body chiyodaInfo: ChiyodaInfo
    ): Response<ProcessResultDto>

    @GET("/api/rpa/status")
    suspend fun getRpaStatus(): Response<RpaStatusDto>
}

// RPA Status Response
data class RpaStatusDto(
    val ready: Boolean,
    val bufferedCount: Int,
    val timestamp: String
)