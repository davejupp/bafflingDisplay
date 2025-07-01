package com.example.bafflingvision

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Sealed class to represent USB status more explicitly (can be kept from previous version)
sealed class UsbStatus {
    data object ServiceBound : UsbStatus()
    data object ServiceUnbound : UsbStatus()
    data class Connected(val deviceName: String) : UsbStatus()
    data object Disconnected : UsbStatus()
    data class Error(val message: String) : UsbStatus()
}

interface UsbMonitor {
    /**
     * A flow emitting the current status of the USB connection.
     */
    val usbStatus: StateFlow<UsbStatus>

    /**
     * A flow emitting received BafangMessage objects.
     * Null if no message is ready or an error occurred during parsing.
     */
    val receivedMessage: StateFlow<BafangMessage>

    /**
     * A flow emitting a log of data sending attempts and their results.
     */
    val sentMessage: StateFlow<BafangMessage>

    /**
     * A flow emitting a log of data sending attempts and their results.
     */
    val errorMessage: StateFlow<BafangMessage>

    /**
     * Initiates the process of finding and connecting to a USB device.
     * The connection status will be updated via [usbStatus].
     */
    fun findAndConnectDevice()

    /**
     * Disconnects from the currently connected USB device.
     * The connection status will be updated via [usbStatus].
     */
    fun disconnectDevice()

    /**
     * Sends a read request message to the connected device.
     * @param message The [ReadMessage] to send.
     * @return true if the data was successfully passed to the service for sending, false otherwise.
     *         Note: Successful return here does not guarantee the device received or processed it.
     *         Observe [sentDataLog] and [receivedMessage] for more details.
     */
    fun sendReadRequest(message: ReadMessage): Boolean

    /**
     * Starts the monitoring process, which includes binding to the UsbSerialService.
     * This must be called before other operations like [findAndConnectDevice].
     */
    fun start()

    /**
     * Stops the monitoring process, which includes unbinding from the UsbSerialService.
     * This should be called when the monitor is no longer needed to release resources.
     */
    fun stop()
}