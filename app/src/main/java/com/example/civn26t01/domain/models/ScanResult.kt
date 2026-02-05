package com.example.civn26t01.domain.models

data class ScanResult (
    val source: String?,
    val data: String?,
    val labelType: String?,
    val timestamp: Long = System.currentTimeMillis()
)

