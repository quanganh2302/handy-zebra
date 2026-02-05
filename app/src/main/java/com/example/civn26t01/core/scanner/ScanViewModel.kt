package com.example.civn26t01.core.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {
    private val _scanEvents = MutableSharedFlow<ScanEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val scanEvents = _scanEvents.asSharedFlow()

    fun emitScanEvent(event: ScanEvent) {
        viewModelScope.launch {
            _scanEvents.emit(event)
        }
    }
}