package com.example.civn26t01.data.models

data class MasterLabelData (
    val wono: String,
    val date: String,
    val qty: Double // Changed from Int to Double to support decimal quantities
)