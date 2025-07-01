package com.example.bafflingvision

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

class FWVersion(val major: Byte, val minor: Byte, val patch: Byte) {
    override fun toString(): String {
        return "Version ${major}.${minor}.${patch}"
    }
}

// Update UsbSerialScreen Composable to handle the new UsbStatus type
@Composable
fun UsbSerialScreen(
    usbStatus: UsbStatus,
    sentData: List<BafangMessage>,
    messageReceived: List<BafangMessage>,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSendFirmwareVersionRequest: () -> Unit
) {
    val statusText = when (usbStatus) {
        is UsbStatus.Connected -> "Connected to: ${usbStatus.deviceName}"
        UsbStatus.Disconnected -> "Disconnected"
        is UsbStatus.Error -> "Error: ${usbStatus.message}"
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