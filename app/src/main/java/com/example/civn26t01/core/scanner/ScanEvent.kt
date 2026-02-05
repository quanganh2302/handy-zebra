package com.example.civn26t01.core.scanner

sealed class ScanEvent {
    data class Success(val data: String, val codeType: String) : ScanEvent()
    object Timeout : ScanEvent()
    object Canceled : ScanEvent()
    object Alert : ScanEvent()
    data class Failed(val reason: String) : ScanEvent()
}