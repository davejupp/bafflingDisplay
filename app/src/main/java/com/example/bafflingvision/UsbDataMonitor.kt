package com.example.bafflingvision

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.bafflingvision.BafangMessage.Companion.OPCODE_READ_FW_VERSION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class UsbDataMonitor(private val context: Context): UsbMonitor, UsbSerialService.UsbSerialListener
{
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
            usbSerialService.setListener(this@UsbDataMonitor)
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

    override fun sendReadRequest(message: ReadMessage): Boolean {
        _sentMessage.value = message
        return sendData(message.getBytes())
    }

    override fun start() {
        startMonitoring()
    }

    override fun stop() {
        stopMonitoring()
    }

    override fun onUsbDeviceAttached(deviceName: String) {
        _usbStatus.value = UsbStatus.Connected(deviceName)
        Log.i(TAG, "USB Device Attached: $deviceName")
    }

    override fun onUsbDeviceDetached() {
        _usbStatus.value = UsbStatus.Disconnected
        Log.i(TAG, "USB Device Detached")
    }

    private fun processReadMessageReceived(bytes: ByteArray): Int {
        if(bytes[1] == OPCODE_READ_FW_VERSION) {
            val messageSize = GetFirmwareVersionResponse.MESSAGE_SIZE
            if (messageSize <= bytes.size) {
                val receiveMessage = GetFirmwareVersionResponse(bytes)
                _receivedMessage.value = receiveMessage
                return messageSize
            }
        }
        return 0
    }

    private fun processWriteMessageReceived(bytes: ByteArray): Int {
        return 0
    }


    override fun onUsbDataReceived(data: ByteArray) {
        // just append the data. work out what to do afterwards. we literally don't know how long the message is until we get these bytes
        inputBuffer = inputBuffer + data
        var outputConsumed = 0
        // collect more bytes if we need
        if (inputBuffer.size < 2) {
            return
        } else if (inputBuffer[0] == MessageType.READ.code) {
            outputConsumed = processReadMessageReceived(inputBuffer)
        } else if (inputBuffer[0] == MessageType.WRITE.code) {
            outputConsumed = processWriteMessageReceived(inputBuffer)
        } else {
            // swallow a byte and try again?
            outputConsumed = 1
        }
        if (outputConsumed > 0) {
            inputBuffer = inputBuffer.drop(outputConsumed).toByteArray()
        }
    }

    override fun onUsbConnectionError(message: String) {
        _usbStatus.value = UsbStatus.Error(message)
        Log.e(TAG, "USB Connection Error: $message")
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

// Helper extension function for logging byte arrays
fun ByteArray.toHexString(): String =
    joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }
