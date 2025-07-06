package com.example.bafflingvision.usbDataMonitor

import com.example.bafflingvision.BafangMessage
import com.example.bafflingvision.BafangReadMessage
import kotlinx.coroutines.flow.StateFlow

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

sealed class UsbStatus {
    data object ServiceBound : UsbStatus()
    data object ServiceUnbound : UsbStatus()
    data class Connected(val deviceName: String) : UsbStatus()
    data object Disconnected : UsbStatus()
    data class Error(val message: String) : UsbStatus()
}

/**
 * Interface representing a class that translates between uart serial data and bafang messages
 */
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
     * @param message The [com.example.bafflingvision.BafangReadMessage] to send.
     * @return true if the data was successfully passed to the service for sending, false otherwise.
     *         Note: Successful return here does not guarantee the device received or processed it.
     *         Observe [sentDataLog] and [receivedMessage] for more details.
     */
    fun sendReadRequest(message: BafangReadMessage): Boolean
}