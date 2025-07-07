@file:JvmName("SerialToDisplayMessageConvertorInterfaceKt")

package com.badsheepy.bafflingvision.usbDataMonitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.badsheepy.bafflingvision.BafangMessage
import com.badsheepy.bafflingvision.BafangMessage.Companion.OSFW_READ
import com.badsheepy.bafflingvision.ErrorProcessingMessage
import com.badsheepy.bafflingvision.GetFirmwareVersionResponse
import com.badsheepy.bafflingvision.MessageType
import com.badsheepy.bafflingvision.NoOpBafangMessage
import com.badsheepy.bafflingvision.BafangReadMessage
import com.badsheepy.bafflingvision.BafangReadSpeedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

/**
 * This class connects to, or starts and then connects to the USB subsystem service. It acts as a
 * bridge between the raw data transfer and the actual display/config data we want.
 * I have them separated like this
 *
 * Because serial port
 * comms (and especially bafang uart) is somewhat terrible we'll handle all the bad data/incomplete
 * messages/other nonsense here and just not propogate non legitimate messages to our subscribers.
 */
class SerialToDisplayMessageConvertor(private val context: Context): SerialToDisplayMessageConvertorInterface
{
    private inner class USBListener: UsbSerialService.UsbSerialListener {
        override fun onUsbDeviceAttached(deviceName: String) {
            _usbStatus.value = UsbStatus.Connected(deviceName)
            Log.i(TAG, "USB Device Attached: $deviceName")
        }

        override fun onUsbDeviceDetached() {
            _usbStatus.value = UsbStatus.Disconnected
            Log.i(TAG, "USB Device Detached")
            stopPolling() // Stop polling if device detaches
        }

        override fun onUsbDataReceived(data: ByteArray) {
            // just append the data. work out what to do afterwards. we literally don't know how long the message is until we get these bytes
            inputBuffer = inputBuffer + data

            val outputConsumed = if (inputBuffer.size < 2) {
                Log.d(TAG, "Not enough data to process yet: ${inputBuffer.toHexString()}")
                return
            } else if (inputBuffer[0] == MessageType.BBSFW_READ.code) {
                processReadMessageReceived(inputBuffer)
            } else if (inputBuffer[0] == MessageType.BAFANG_READ.code) {
                processBafangReadMessageReceived(inputBuffer)
            } else if (inputBuffer[0] == MessageType.BBSFW_WRITE.code) {
                processWriteMessageReceived(inputBuffer)
            } else if (inputBuffer[0] == 0x20.toByte()) {
                processBafangSpeedMessage(inputBuffer)
            } else {
                Log.w(TAG, "Unknown starting byte, discarding: 0x%01X".format(inputBuffer[0]))
                1 // Consume the unknown byte to prevent getting stuck
            }

            if (outputConsumed > 0) {
                inputBuffer = inputBuffer.drop(outputConsumed).toByteArray()
            }
        }

        override fun onUsbConnectionError(data: String) {
            _usbStatus.value = UsbStatus.Error(data)
            Log.e(TAG, "USB Connection Error: $data")
            stopPolling() // Stop polling on connection error
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

    /**
     * The actual long-running service interacting with the USB serial and
     * calling our callback methods.
     **/
    private lateinit var usbSerialService: UsbSerialService

    /**
     * Is the USB device currently bound
     */
    private var isBound = false

    /**
     * State flow of the USB connection (active, disconnected etc)
     */
    private val _usbStatus = MutableStateFlow<UsbStatus>(UsbStatus.Disconnected)
    override val usbStatus: StateFlow<UsbStatus> = _usbStatus.asStateFlow()

    /**
     * State flow of received, correctly parsed, checksummed Bafang messages
     * These will mostly just be used to update the overall UI state
     */
    private val _receivedMessage = MutableStateFlow<BafangMessage>(NoOpBafangMessage)
    override val receivedMessage: StateFlow<BafangMessage> = _receivedMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<BafangMessage>(NoOpBafangMessage)
    override val errorMessage: StateFlow<BafangMessage> = _errorMessage.asStateFlow()

    // Coroutine scope for background tasks like polling
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var pollingJob: Job? = null

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
            stopPolling() // Stop polling if service disconnects

            if (_usbStatus.value is UsbStatus.Connected) {
                _usbStatus.value = UsbStatus.Disconnected
            }
        }
    }

    override fun startMonitoring() {
        Log.d(TAG, "Attempting to bind UsbSerialService from Monitor")
        Intent(context, UsbSerialService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun stopMonitoring() {
        Log.d(TAG, "Stopping monitoring and unbinding service")
        stopPolling() // Ensure polling is stopped
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
        if (isBound && ::usbSerialService.isInitialized) {
            usbSerialService.disconnect()
        }
        stopPolling() // Stop polling on manual disconnect
    }

    private fun sendData(data: ByteArray): Boolean {
        if (!isBound || !::usbSerialService.isInitialized) {
            Log.w(TAG, "Cannot send data: Service not available or not initialized.")
            return false
        }
        return usbSerialService.sendData(data)
    }

    override fun sendReadRequest(message: BafangReadMessage): Boolean {
        return sendData(message.getBytes())
    }

    /**
     * Starts polling for a specific BafangReadMessage at a given interval.
     *
     * @param messageToPoll The BafangReadMessage to send periodically.
     * @param intervalMillis The interval in milliseconds between polling attempts.
     */
    override fun startPollingWithRequests(): Boolean {
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Polling is already active. Stop it first to start a new one.")
            return true
        }
        var retries = 0;
        while (!isBound || !::usbSerialService.isInitialized || _usbStatus.value !is UsbStatus.Connected) {
            Log.w(TAG, "Cannot start polling: USB service not bound, not initialized, or device not connected.")
//            _errorMessage.value = ErrorProcessingMessage(Error("Cannot start polling: Device not connected or service unavailable."))
//            return false
            Thread.sleep(100)
            if (retries++ == 90) return false;
        }

        val bob = BafangReadSpeedMessage

        pollingJob = coroutineScope.launch {
            while (isActive) { // Loop while the coroutine is active
                if (_usbStatus.value is UsbStatus.Connected) { // Check connection status before sending
                    val success = sendReadRequest(bob)
                    if (success) {
                        Log.d(TAG, "Polling: Sent message")
                    } else {
                        Log.e(TAG, "Polling: Failed to send")
//                        Log.e(TAG, "Polling: Failed to send ${messageToPoll::class.java.simpleName}")
                        // Optionally, you could add logic here to stop polling after too many failures
                        // or signal an error through the _errorMessage StateFlow.
                    }
                } else {
                    Log.w(TAG, "Polling: Skipped sending, device not connected.")
                    // Optionally, stop polling if disconnected for too long or signal this state.
                }
                delay(100)
            }
            Log.i(TAG, "Polling stopped")
        }
        return true
    }

    /**
     * Stops any active polling.
     */
    fun stopPolling() {
        if (pollingJob?.isActive == true) {
            Log.i(TAG, "Stopping polling.")
            pollingJob?.cancel() // Cancel the coroutine
        }
        pollingJob = null
    }


    private fun processReadMessageReceived(bytes: ByteArray): Int {
        if(bytes.isNotEmpty() && bytes[0] == MessageType.BBSFW_READ.code && bytes.size > 1) { // Added null/empty and size check
            if(bytes[1] == OSFW_READ) {
                val messageSize = GetFirmwareVersionResponse.MESSAGE_SIZE
                if (messageSize <= bytes.size) {
                    val receiveMessage = GetFirmwareVersionResponse(bytes.take(messageSize).toByteArray()) // Take only needed bytes
                    _receivedMessage.value = receiveMessage
                    Log.d(TAG, "Processed GetFirmwareVersionResponse")
                    return messageSize
                } else {
                    Log.d(TAG, "OSFW_READ: Not enough data for GetFirmwareVersionResponse. Have ${bytes.size}, need $messageSize")
                }
            } else {
                Log.d(TAG, "BBSFW_READ: Unknown subcommand 0x%01X".format(bytes[1]))
            }
        } else if (bytes.isNotEmpty() && bytes[0] == MessageType.BBSFW_READ.code) {
            Log.d(TAG, "BBSFW_READ: Message too short to determine subcommand. Size: ${bytes.size}")
        }
        return 0 // Indicate 0 bytes consumed if message not fully processed or unknown
    }

    private fun processBafangReadMessageReceived(bytes: ByteArray): Int {
        Log.d(TAG, "Received BAFANG_READ type: ${bytes.toHexString()}")
        // TODO: Implement parsing for specific BafangReadMessage responses
        // You'll need to know the structure of the expected response to parse it
        // and determine its length to return the number of consumed bytes.
        // For example:
        // if (bytes.size >= SomeBafangResponseMessage.EXPECTED_SIZE) {
        //     val parsedMessage = SomeBafangResponseMessage(bytes.take(SomeBafangResponseMessage.EXPECTED_SIZE).toByteArray())
        //     _receivedMessage.value = parsedMessage
        //     return SomeBafangResponseMessage.EXPECTED_SIZE
        // }
        return 0; // Placeholder: return 0 if not processed or full message not yet received
    }

    private fun processBafangSpeedMessage(bytes: ByteArray): Int {
        _receivedMessage.value = BafangReadSpeedMessage
        return 3;
    }

    private fun processWriteMessageReceived(bytes: ByteArray): Int {
        Log.d(TAG, "Received BBSFW_WRITE type (likely an ACK/NACK): ${bytes.toHexString()}")
        // TODO: Implement parsing for write acknowledgements if needed.
        // Often, write operations might just be fire-and-forget, or you might expect a simple ACK.
        // If an ACK has a fixed size, parse it and return that size.
        return 0 // Placeholder
    }

    // Call this when the class instance is no longer needed to clean up coroutines
    fun cleanup() {
        Log.d(TAG, "Cleaning up UsbDataMonitor resources.")
        stopPolling()
        pollingJob?.cancel() // Ensure job is cancelled
        // If coroutineScope uses a Job passed from outside, that external Job should be cancelled.
        // If the Job is created internally like `Job()`, it's good practice to cancel it here if
        // the scope itself isn't being cancelled by a ViewModel's onCleared() for example.
        // However, since `pollingJob` is cancelled, and `coroutineScope` uses `Dispatchers.IO`,
        // individual jobs being cancelled is usually sufficient.
    }
}

// Helper extension function for logging byte arrays
fun ByteArray.toHexString(): String =
    joinToString(separator = " ") { eachByte -> "0x%01X".format(eachByte) }
