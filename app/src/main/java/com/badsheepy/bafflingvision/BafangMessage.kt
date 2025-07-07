package com.badsheepy.bafflingvision

import com.badsheepy.bafflingvision.usbDataMonitor.toHexString

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
abstract class BaseUartMessage {
    abstract fun getBytes(): ByteArray

    override fun toString(): String {
        return "Bafang message: ${getBytes().toHexString()}"
    }
}

abstract class BaseUartReceiveMessage();
abstract class BaseUartSendMessage();

abstract class BafangReadMessage(code: Byte) : BafangMessage(MessageType.BAFANG_READ, code)

abstract class BafangWriteMessage(val code: Byte) : BafangIMessage {
    override fun getType(): MessageType {
        return MessageType.BAFANG_WRITE
    }

    override fun getOpcode(): Byte {
        return code
    }
}

class BafangReadBasicDataMessage(val code: Byte): BafangReadMessage(code) {
    override fun getBytes(): ByteArray {
        return byteArrayOf(MessageType.BAFANG_READ.code, code)
    }
}

object BafangReadFirmwareVersionMessage: BafangReadMessage(OSFW_READ) {
    override fun getBytes(): ByteArray {
        return byteArrayOf(0x1, 0x1, 0x2)
    }
}

object BafangReadEventLogStatusMessage: BafangReadMessage(OSFW_READ) {
    override fun getBytes(): ByteArray {
        return byteArrayOf(MessageType.BBSFW_READ.code, OPCODE_READ_EVTLOG_ENABLE, 0x3)
    }
}
//
//object BafangEnableEventLogStatusMessage: BafangWriteMessage(OSFW_WRITE) {
//    override fun getBytes(): ByteArray {
//        return byteArrayOf(MessageType.BAFANG_WRITE.code, 0x2, 0x3)
//    }
//}

object BafangMessage08Bafang: BafangReadMessage(0x08)
object BafangMessage0ABafang: BafangReadMessage(0x0a)
object BafangMessage11Bafang: BafangReadMessage(0x11)
object BafangReadSpeedMessage: BafangReadMessage(0x20)
class BafangReadSpeedMessageReceived(data: ByteArray): BafangReadMessage(0x20) {
    val rangeExtensions = data[0]
    val rawWheelSpeed = data[1]
}
object BafangMessage22Bafang: BafangReadMessage(0x22)

object BafangDisplayCurrentMessage: BafangReadMessage(0x0a)

abstract class ReceiveMessage(messageType: MessageType, code: Byte, val data: ByteArray): BafangMessage(messageType, code) {
    abstract fun getSize():  Int
}

class GetFirmwareVersionResponse(data: ByteArray): ReceiveMessage(MessageType.BAFANG_READ, OSFW_READ, data) {
    companion object {
        const val MESSAGE_SIZE = 8
    }
    override fun getSize(): Int { return MESSAGE_SIZE }

    fun getVersion(): FWVersion {
        if (data.size < MESSAGE_SIZE) return FWVersion(0, 0, 0)
        return FWVersion(data[2], data[3], data[4])
    }

    override fun toString(): String {
        return "Bafang message: ${getVersion()}"
    }
}

object NoOpBafangMessage: BafangMessage(MessageType.BAFANG_READ, 0x00)
class ErrorProcessingMessage(val message: Error): BafangMessage(MessageType.UNKNOWN, 0xf)

abstract class BafangMessage(private val _type: MessageType, private val _opcode: Byte): BafangIMessage {
    override fun getType(): MessageType = _type
    override fun getOpcode(): Byte = _opcode

    companion object {
        /**
         * Shamelessly copied constants from bbs-fw open source firmware tool by
         * Daniel Nilsson https://github.com/danielnilsson9/bbs-fw/wiki/Bafang-Display-Protocol
         */
        // Note that kotlin needs these silly assignments because bytes are signed on the JVM
        val OSFW_READ: Byte = 0x01;
        val OPCODE_READ_EVTLOG_ENABLE: Byte = 0x02;
        val OPCODE_READ_CONFIG: Byte = 0x03;

        val OPCODE_WRITE_EVTLOG_ENABLE: Byte = (0xf0).toByte();
        val OPCODE_WRITE_CONFIG: Byte = (0xf1).toByte();
        val OPCODE_WRITE_RESET_CONFIG: Byte = (0xf2).toByte();
        val OPCODE_WRITE_ADC_VOLTAGE_CALIBRATION: Byte = (0xf3).toByte();
    }

    override fun toString(): String {
        return "Bafang message: ${getBytes().toHexString()}"
    }

    override fun getBytes(): ByteArray {
        val message = byteArrayOf(getType().code, getOpcode());
        if (getType() == MessageType.BAFANG_READ) {
            //val checkSum = computeChecksum(message)
            return message
        }
        return message
    }

    fun computeChecksum(buffer: ByteArray): Byte {
        var checksum = 0
        for (i in 1..<buffer.size) {
            checksum += buffer[i]
        }
        return checksum.toByte();
    }
}

interface BafangIMessage {
    fun getType(): MessageType
    fun getOpcode(): Byte
    fun getBytes(): ByteArray
}
