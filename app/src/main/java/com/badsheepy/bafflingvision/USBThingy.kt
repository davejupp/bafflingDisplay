package com.badsheepy.bafflingvision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.badsheepy.bafflingvision.usbDataMonitor.UsbStatus

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

// Update UsbSerialScreen Composable to handle the new UsbStatus type
@Composable
fun UsbSerialScreen(
    usbStatus: UsbStatus,
    sentData: List<BafangMessage>,
    messageReceived: List<BafangMessage>,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSendFirmwareVersionRequest: () -> Unit,
    onSendSomeRequest: () -> Unit,
    onReadBasicDataRequest08: () -> Unit,
    onReadBasicDataRequest11: () -> Unit,
    onReadBasicDataRequest24: () -> Unit,
    onReadBasicDataRequest0a: () -> Unit,
    onReadBasicDataRequest20: () -> Unit,
    onReadBasicDataRequest22: () -> Unit,
    onStartPollingClick: () -> Unit
) {
    val statusText = when (usbStatus) {
        is UsbStatus.Connected -> "Connected to: ${usbStatus.deviceName}"
        is UsbStatus.Error -> "Error: ${usbStatus.message}"
        UsbStatus.Disconnected -> "Disconnected"
        UsbStatus.ServiceBound -> "Service Bound (Ready to connect)"
        UsbStatus.ServiceUnbound -> "Service Unbound"
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("USB Serial Demo", style = MaterialTheme.typography.headlineSmall)
            Text("Status: $statusText") // Use the formatted statusText

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onConnectClick,
                enabled = usbStatus is UsbStatus.ServiceBound || usbStatus is UsbStatus.Disconnected || usbStatus is UsbStatus.Error
            ) {
                Text("Connect/Find Device")
            }

//            Button(
//                onClick = onStartPollin,
//                enabled = usbStatus is UsbStatus.ServiceBound || usbStatus is UsbStatus.Disconnected || usbStatus is UsbStatus.Error
//            ) {
//                Text("Start polling")
//            }

            Button(onClick = onDisconnectClick, enabled = usbStatus is UsbStatus.Connected) {
                Text("Disconnect")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Button for specific command
            Button(
                onClick = onSendFirmwareVersionRequest,
                enabled = usbStatus is UsbStatus.Connected
            ) {
                Text("Request Firmware Version")
            }
//
//            // Button for specific command
//            Button(
//                onClick = onSendSomeRequest,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Request something or other")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest08,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Read 08")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest0a,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Read 0a")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest11,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Read 11")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest20,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Read 20")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest22,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Read 22")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest24,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("Read 24")
//            }
//
//            // Button for specific command
//            Button(
//                onClick = onReadBasicDataRequest08,
//                enabled = usbStatus is UsbStatus.Connected
//            ) {
//                Text("ReadBasic Data request")
//            }

            // Button for specific command
            Button(
                onClick = onStartPollingClick,
                enabled = usbStatus is UsbStatus.Connected
            ) {
                Text("Start polling")
            }

            for (sentDataItem in sentData) {
                Text("Sent list: $sentDataItem")
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Received Data:", style = MaterialTheme.typography.titleMedium)
            Text(
                messageReceived.toString(),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    }
}
