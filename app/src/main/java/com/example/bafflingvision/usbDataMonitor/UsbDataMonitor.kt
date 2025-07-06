package com.example.bafflingvision.usbDataMonitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.bafflingvision.BafangMessage
import com.example.bafflingvision.BafangMessage.Companion.OSFW_READ
import com.example.bafflingvision.ErrorProcessingMessage
import com.example.bafflingvision.GetFirmwareVersionResponse
import com.example.bafflingvision.MessageType
import com.example.bafflingvision.NoOpBafangMessage
import com.example.bafflingvision.BafangReadMessage
import com.example.bafflingvision.UsbSerialService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
/**
 *     BafflingDisplay android app
 *
 *     Copyright (C) 2025 Dave.J
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

class UsbDataMonitor(private val context: Context): UsbMonitor
{
    private inner class USBListener: UsbSerialService.UsbSerialListener {
        override fun onUsbDeviceAttached(deviceName: String) {
            _usbStatus.value = UsbStatus.Connected(deviceName)
            Log.i(TAG, "USB Device Attached: $deviceName")
        }

        override fun onUsbDeviceDetached() {
            _usbStatus.value = UsbStatus.Disconnected
            Log.i(TAG, "USB Device Detached")
        }

        override fun onUsbDataReceived(data: ByteArray) {
            // just append the data. work out what to do afterwards. we literally don't know how long the message is until we get these bytes
            inputBuffer = inputBuffer + data
            var outputConsumed = 0
            // collect more bytes if we need
            Log.e("UsbDataMonitor", "Received ${data.toHexString()}")
            outputConsumed = if (inputBuffer.size < 2) {
                return
            } else if (inputBuffer[0] == MessageType.BBSFW_READ.code) {
                processReadMessageReceived(inputBuffer)
            } else if (inputBuffer[0] == MessageType.BAFANG_READ.code) {
                processBafangReadMessageReceived(inputBuffer)
            } else if (inputBuffer[0] == MessageType.BBSFW_WRITE.code) {
                processWriteMessageReceived(inputBuffer)
            } else {
               0 // we have no idea what this byte is so ignore it
            }
            if (outputConsumed > 0) {
                inputBuffer = inputBuffer.drop(outputConsumed).toByteArray()
            }
        }

        override fun onUsbConnectionError(data: String) {
            _usbStatus.value = UsbStatus.Error(data)
            Log.e(TAG, "USB Connection Error: $data")
        }

        override fun onUsbReadError(e: IOException) {
            _usbStatus.value = UsbStatus.Error("Read Error: ${e.message}")
            _errorMessage.value = ErrorProcessingMessage(Error(e.message))
            Log.e(TAG, "USB Read Error", e)
        }

        override fun onUsbWriteError(e: IOException) {
            // This can update a specific write error state if needed,
            // or just be logged. The sendData method's return provides immediate feedback.
            Log.e(TAG, "USB Write Error", e)
        }

    }

    // Not every read is a complete message, so we need to store the received data up until this point
    private var inputBuffer: ByteArray = ByteArray(0);

    private lateinit var usbSerialService: UsbSerialService
    private var isBound = false

    private val _usbStatus = MutableStateFlow<UsbStatus>(UsbStatus.Disconnected)
    override val usbStatus: StateFlow<UsbStatus> = _usbStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow<BafangMessage>(NoOpBafangMessage)
    override val receivedMessage: StateFlow<BafangMessage> = _receivedMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<BafangMessage>(NoOpBafangMessage)
    override val errorMessage: StateFlow<BafangMessage> = _errorMessage.asStateFlow()

    private val _sentMessage = MutableStateFlow<BafangMessage>(NoOpBafangMessage)
    override val sentMessage: StateFlow<BafangMessage> = _sentMessage.asStateFlow()

    companion object {
        private const val TAG = "UsbDataMonitor"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as UsbSerialService.UsbBinder
            usbSerialService = binder.getService()
            usbSerialService.setListener(USBListener())
            isBound = true
            Log.d(TAG, "UsbSerialService connected via Monitor")
            _usbStatus.value = UsbStatus.ServiceBound
            findAndConnectDevice()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbSerialService.setListener(null)
            isBound = false
            Log.d(TAG, "UsbSerialService disconnected via Monitor")
            _usbStatus.value = UsbStatus.ServiceUnbound

            if (_usbStatus.value is UsbStatus.Connected) {
                _usbStatus.value = UsbStatus.Disconnected
            }
        }
    }

    fun startMonitoring() {
        Log.d(TAG, "Attempting to bind UsbSerialService from Monitor")
        Intent(context, UsbSerialService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping monitoring and unbinding service")
        if (isBound) {
            usbSerialService.setListener(null)
            context.unbindService(serviceConnection)
            isBound = false
            _usbStatus.value = UsbStatus.ServiceUnbound
            if (_usbStatus.value is UsbStatus.Connected) {
                _usbStatus.value = UsbStatus.Disconnected
            }
        }
    }

    override fun findAndConnectDevice() {
        if (!isBound) {
            _usbStatus.value = UsbStatus.Error("Service not bound. Call startMonitoring() first.")
            Log.w(TAG, "Cannot connect: Service not bound.")
            return
        }
        usbSerialService.findAndConnectDevice()
    }

    override fun disconnectDevice() {
        usbSerialService.disconnect()
    }

    private fun sendData(data: ByteArray): Boolean {
        if (!isBound) {
            Log.w(TAG, "Cannot send data: Service not available.")
            return false
        }
        return usbSerialService.sendData(data)
    }

    override fun sendReadRequest(message: BafangReadMessage): Boolean {
        _sentMessage.value = message
        return sendData(message.getBytes())
    }

    private fun processReadMessageReceived(bytes: ByteArray): Int {
        if(bytes[1] == OSFW_READ) {
            val messageSize = GetFirmwareVersionResponse.Companion.MESSAGE_SIZE
            if (messageSize <= bytes.size) {
                val receiveMessage = GetFirmwareVersionResponse(bytes)
                _receivedMessage.value = receiveMessage
                return messageSize
            }
        }
        return 0
    }

    private fun processBafangReadMessageReceived(bytes: ByteArray): Int {
       print("Received ${bytes.toHexString()}")
        return 0;
    }

    private fun processWriteMessageReceived(bytes: ByteArray): Int {
        return 0
    }
}

// Helper extension function for logging byte arrays
fun ByteArray.toHexString(): String =
    joinToString(separator = " ") { eachByte -> "0x%01X".format(eachByte) }
