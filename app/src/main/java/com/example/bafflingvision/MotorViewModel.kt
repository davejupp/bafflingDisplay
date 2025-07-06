// In a new file, e.g., UsbMonitorViewModel.kt
package com.example.bafflingvision // Or your appropriate package

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bafflingvision.usbDataMonitor.UsbDataMonitor
import java.io.IOException

class MotorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(MotorUiState())
    val uiState: LiveData<MotorUiState> = _uiState

    private var usbDataMonitor: UsbDataMonitor = UsbDataMonitor(application.applicationContext)
    private var isServiceBound = false

//    private val usbSerialListener = object : UsbSerialService.UsbSerialListener {
//        override fun onUsbDeviceAttached(deviceName: String) {
//            _uiState.postValue(
//                _uiState.value?.copy(
//                    isDeviceAttached = true,
//                    connectedDeviceName = deviceName,
//                    connectionError = null // Clear previous connection errors
//                )
//            )
//        }

//        override fun onUsbDeviceDetached() {
//            _uiState.postValue(
//                MotorUiState() // Reset to initial state or a specific detached state
//            )
//        }

//        override fun onUsbDataReceived(message: ByteArray) {
//            _uiState.postValue(
//                _uiState.value?.copy(
//                    lastReceivedData = message,
//                    // Optionally clear read errors if data is now coming through
//                    readError = null
//                )
//            )
//        }

}