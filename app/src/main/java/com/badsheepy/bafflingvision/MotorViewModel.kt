// In a new file, e.g., UsbMonitorViewModel.kt
package com.badsheepy.bafflingvision // Or your appropriate package

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.badsheepy.bafflingvision.usbDataMonitor.SerialToDisplayMessageConvertor
import com.badsheepy.bafflingvision.usbDataMonitor.SerialToDisplayMessageConvertorInterface

class MotorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(MotorUiState())
    val uiState: LiveData<MotorUiState> = _uiState

    private var usbDataMonitor: SerialToDisplayMessageConvertorInterface =
        SerialToDisplayMessageConvertor(application.applicationContext)
}