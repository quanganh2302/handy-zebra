package com.example.civn26t01.data.models

import com.google.gson.annotations.SerializedName

data class ProcessResultDto(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("jobId")
    val jobId: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)