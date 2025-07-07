package com.badsheepy.bafflingvision

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.badsheepy.bafflingvision.AppScreens.Home
import com.badsheepy.bafflingvision.AppScreens.None
import com.badsheepy.bafflingvision.ui.theme.BafflingVisionTheme
import com.badsheepy.bafflingvision.usbDataMonitor.SerialToDisplayMessageConvertor
import com.badsheepy.bafflingvision.usbDataMonitor.SerialToDisplayMessageConvertorInterface
import com.badsheepy.bafflingvision.usbDataMonitor.UsbStatus
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

class MainNavigationActivity : ComponentActivity() {

    private lateinit var messageSource: SerialToDisplayMessageConvertorInterface

    private val usbStatusUi = mutableStateOf<UsbStatus>(UsbStatus.ServiceUnbound)
    private val sentDataUi = mutableStateOf<List<BafangMessage>>(emptyList())
    private val messageReceivedUi = mutableStateOf<List<BafangMessage>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        messageSource = SerialToDisplayMessageConvertor(applicationContext)

        // Collect flows from UsbDataMonitor to update UI state
        lifecycleScope.launch {
            messageSource.usbStatus.collect { status ->
                Log.d("MainActivity", "Monitor Status Updated: $status")
                usbStatusUi.value = status

                // Clear data on disconnect or error from UI perspective
                if (status is UsbStatus.Disconnected || status is UsbStatus.Error) {
                    messageReceivedUi.value = emptyList()
                }

                if (status is UsbStatus.Connected) {
                    sentDataUi.value = emptyList()
                }
            }
        }

        lifecycleScope.launch {
            messageSource.receivedMessage.collect { data ->
                data.let {
                    Log.d("MainActivity", "Monitor Received Data UI Update: $data")
                }
            }
        }

        setContent {
            BafflingVisionTheme {
                AppNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        messageSource.startMonitoring()
        Log.d("MainActivity", "UsbDataMonitor.startMonitoring() called")
    }

    override fun onStop() {
        super.onStop()
        messageSource.stopMonitoring()
        Log.d("MainActivity", "UsbDataMonitor.stopMonitoring() called")
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun AppNavigation() {
        val topLevelBackStack = remember { TopLevelBackStack<AppScreens>(Home) }

        Scaffold(
            bottomBar = {
                NavigationBar {  // Material 3 Bottom Navigation Bar
                    TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                        val isSelected = topLevelRoute == topLevelBackStack.topLevelKey
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                topLevelBackStack.addTopLevel(topLevelRoute)
                            },
                            icon = {
                                Icon(
                                    imageVector = topLevelRoute.icon,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        ) {
            NavDisplay(
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryProvider = { key ->
                    when (key) {
                        is Home -> NavEntry(key) {
                            UsbSerialScreen(
                                usbStatus = usbStatusUi.value, // Pass the UsbStatus sealed class instance
                                sentData = sentDataUi.value,
                                messageReceived = messageReceivedUi.value,
                                onConnectClick = { messageSource.findAndConnectDevice() },
                                onDisconnectClick = { messageSource.disconnectDevice() },
                                onSendFirmwareVersionRequest = {
                                    messageSource.sendReadRequest(BafangReadFirmwareVersionMessage) // ReadFirmwareVersionMessage)
                                },
                                onSendSomeRequest = {
                                    messageSource.sendReadRequest(BafangReadEventLogStatusMessage) // ReadFirmwareVersionMessage)
                                },
                                onReadBasicDataRequest08 = {
                                    messageSource.sendReadRequest(BafangMessage08Bafang)
                                },
                                onReadBasicDataRequest11 = {
                                    messageSource.sendReadRequest(BafangMessage11Bafang)
                                },
                                onReadBasicDataRequest24 = {
                                    messageSource.sendReadRequest(BafangDisplayCurrentMessage)
                                },
                                onReadBasicDataRequest0a = {
                                    messageSource.sendReadRequest(BafangMessage0ABafang)
                                },
                                onReadBasicDataRequest20 = {
                                    messageSource.sendReadRequest(BafangReadSpeedMessage)
                                },
                                onReadBasicDataRequest22 = {
                                    messageSource.sendReadRequest(BafangMessage22Bafang)
                                },
                                onStartPollingClick = {
                                    messageSource.startPollingWithRequests()
                                }
                            )
                        }

                        else -> NavEntry(None) { Text("Unknown Route") }
                    }

                })
        }
    }

    @Preview(showBackground = true, device = "id:pixel_5")
    @Composable
    fun DefaultPreviewMainNavigationActivity() {
        BafflingVisionTheme {
            AppNavigation()
        }
    }
}