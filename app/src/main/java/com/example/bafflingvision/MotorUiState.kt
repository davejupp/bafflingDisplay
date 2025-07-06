package com.example.bafflingvision

class MotorUiState(
    val isDeviceAttached: Boolean = false,
    val connectedDeviceName: String? = null,

    // Speed in KM/H
    val speed: Int = 0,
    // Current in amps
    val current: Int = 0,
    // Voltage
    val volts: Int = 0,

    val batteryVoltage: Int = 0,
    val batteryCurrent: Int = 0,

    //    val connectionError: String? = null,
//    val readError: Boolean? = null,
//    val writeError: Boolean? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MotorUiState

        if (isDeviceAttached != other.isDeviceAttached) return false
        if (speed != other.speed) return false
        if (current != other.current) return false
        if (volts != other.volts) return false
        if (batteryVoltage != other.batteryVoltage) return false
        if (batteryCurrent != other.batteryCurrent) return false
        if (connectedDeviceName != other.connectedDeviceName) return false
//        if (readError != other.readError) return false
//        if (writeError != other.writeError) return false
//        if (!lastReceivedData.contentEquals(other.lastReceivedData)) return false
//        if (connectionError != other.connectionError) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isDeviceAttached.hashCode()
        result = 31 * result + speed
        result = 31 * result + current
        result = 31 * result + volts
        result = 31 * result + batteryVoltage
        result = 31 * result + batteryCurrent
        result = 31 * result + (connectedDeviceName?.hashCode() ?: 0)
//        result = 31 * result + (readError?.hashCode() ?: 0)
//        result = 31 * result + (writeError?.hashCode() ?: 0)
//        result = 31 * result + (lastReceivedData?.contentHashCode() ?: 0)
//        result = 31 * result + (connectionError?.hashCode() ?: 0)
        return result
    }
}