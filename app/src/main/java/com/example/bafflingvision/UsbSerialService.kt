package com.example.bafflingvision

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.util.concurrent.Executors

class UsbSerialService : Service() {
    private val binder = UsbBinder()
    private var usbManager: UsbManager? = null
    private var connectedPort: UsbSerialPort? = null
    private var serialInputOutputManager: SerialInputOutputManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var listener: UsbSerialListener? = null

    interface UsbSerialListener {
        fun onUsbDeviceAttached(deviceName: String)
        fun onUsbDeviceDetached()
        fun onUsbDataReceived(message: ByteArray)
        fun onUsbConnectionError(message: String)
        fun onUsbReadError(e: IOException)
        fun onUsbWriteError(e: IOException)
    }

    companion object {
        private const val TAG = "UsbSerialService"
        private const val ACTION_USB_PERMISSION = "com.example.bafflingvision.USB_PERMISSION" // Make sure this is unique
        private const val BAUD_RATE = 1200
        private const val DATA_BITS = 8
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1
        private const val PARITY = UsbSerialPort.PARITY_NONE
    }

    inner class UsbBinder : Binder() {
        fun getService(): UsbSerialService = this@UsbSerialService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        registerUsbReceiver()
        Log.d(TAG, "UsbSerialService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UsbSerialService started")
        // You can handle intents here if needed, for example, to auto-connect to a specific device
        findAndConnectDevice()
        return START_STICKY // Or other appropriate return value
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            Log.d(TAG, "Permission granted for device ${it.deviceName}")
                            connectToDevice(it)
                        } ?: run {
                            Log.e(TAG, "Permission granted but device is null")
                            listener?.onUsbConnectionError("Permission granted but device is null")
                        }
                    } else {
                        Log.e(TAG, "Permission denied for device ${device?.deviceName}")
                        listener?.onUsbConnectionError("USB permission denied for ${device?.deviceName}")
                    }
                }
            }
        }
    }

    private val usbAttachDetachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "USB device attached: ${it.deviceName}")
                    // You might want to automatically try to connect or notify the UI
                    findAndConnectDevice() // Or attempt connection to this specific device
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "USB device detached: ${it.deviceName}")
                    if (connectedPort?.device?.deviceId == it.deviceId) {
                        disconnect()
                        listener?.onUsbDeviceDetached()
                    }
                }
            }
        }
    }

    private fun registerUsbReceiver() {
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbPermissionReceiver, permissionFilter, RECEIVER_NOT_EXPORTED)

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbAttachDetachReceiver, filter)
        Log.d(TAG, "USB broadcast receivers registered")
    }

    fun setListener(listener: UsbSerialListener?) {
        this.listener = listener
    }

    fun findAndConnectDevice() {
        if (connectedPort != null) {
            Log.d(TAG, "Already connected to a device.")
            return
        }

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "No USB serial drivers available.")
            listener?.onUsbConnectionError("No USB serial drivers available.")
            return
        }

        // Find the first available port. You might want more sophisticated logic here.
        for (driver in availableDrivers) {
            val device = driver.device
            if (!usbManager!!.hasPermission(device)) {
                Log.d(TAG, "Requesting permission for device: ${device.deviceName}")
                val permissionIntent = PendingIntent.getBroadcast(
                    this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
                )
                usbManager!!.requestPermission(device, permissionIntent)
                return // Wait for permission broadcast
            } else {
                Log.d(TAG, "Permission already granted for device: ${device.deviceName}")
                connectToDevice(device)
                 if (connectedPort != null) break // Connected to the first available device
            }
        }

        if (connectedPort == null) {
            Log.d(TAG, "No suitable USB serial device found or permission pending.")
            listener?.onUsbConnectionError("No suitable USB serial device found.")
        }
    }


    private fun connectToDevice(device: UsbDevice) {
        Log.d(TAG, "Attempting to connect to device: ${device.deviceName}")
        val connection = usbManager!!.openDevice(device)
        if (connection == null) {
            listener?.onUsbConnectionError("Could not open device: ${device.deviceName}")
            Log.e(TAG, "Could not open device: ${device.deviceName}")
            return
        }

        // Most devices have only one port (port 0)
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = availableDrivers.find { it.device.deviceId == device.deviceId }

        if (driver == null || driver.ports.isEmpty()){
            listener?.onUsbConnectionError("No serial ports found for device: ${device.deviceName}")
            Log.e(TAG, "No serial ports found for device: ${device.deviceName}")
            connection.close()
            return
        }

        val port = driver.ports[0] // Usually the first port

        try {
            port.open(connection)
            port.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)
            connectedPort = port
            startSerialListener()
            mainHandler.post { listener?.onUsbDeviceAttached(device.deviceName ?: "Unknown Device") }
            Log.i(TAG, "Successfully connected to ${device.deviceName}")
        } catch (e: IOException) {
            Log.e(TAG, "Error setting up device: ${e.message}", e)
            mainHandler.post { listener?.onUsbConnectionError("Error setting up device: ${e.message}") }
            try {
                port.close()
            } catch (ignored: IOException) {
            }
            connectedPort = null
        }
    }

    private fun startSerialListener() {
        connectedPort?.let { port ->
            if (serialInputOutputManager == null) {
                serialInputOutputManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) {
                        mainHandler.post { listener?.onUsbDataReceived(data) }
                    }

                    override fun onRunError(e: Exception) {
                        mainHandler.post { listener?.onUsbReadError(e as IOException) }
                        disconnect()
                    }
                })
                Executors.newSingleThreadExecutor().submit(serialInputOutputManager)
                Log.d(TAG, "SerialInputOutputManager started")
            }
        }
    }

    fun sendData(data: ByteArray): Boolean {
        if (connectedPort == null || serialInputOutputManager == null) {
            Log.w(TAG, "Cannot send data, not connected.")
            return false
        }
        try {
            connectedPort?.write(data, 500) // 500ms timeout
            Log.d(TAG, "Sent data: ${data.toHexString()}")
            return true
        } catch (e: IOException) {
            Log.d(TAG, "Failed to send data: ${data.toHexString()}")
            Log.e(TAG, "Error writing data", e)
            mainHandler.post { listener?.onUsbWriteError(e) }
            return false
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from USB device")
        try {
            serialInputOutputManager?.stop()
            serialInputOutputManager = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping IO manager", e)
        }
        try {
            connectedPort?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing port", e)
        }
        connectedPort = null
        mainHandler.post { listener?.onUsbDeviceDetached() }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        unregisterReceiver(usbPermissionReceiver)
        unregisterReceiver(usbAttachDetachReceiver)
        Log.d(TAG, "UsbSerialService destroyed")
    }
}