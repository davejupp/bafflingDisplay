package com.example.bafflingvision

enum class MessageType(val code: Byte) {
    READ(0x1),
    WRITE(0x2),
    UNKNOWN(0xf)
}

abstract class ReadMessage(code: Byte) : BafangMessage(MessageType.READ, code)
object ReadFirmwareVersionMessage: ReadMessage(OPCODE_READ_FW_VERSION)

abstract class ReceiveMessage(messageType: MessageType, code: Byte, val data: ByteArray): BafangMessage(messageType, code) {
    abstract fun getSize():  Int
}

class GetFirmwareVersionResponse(data: ByteArray): ReceiveMessage(MessageType.READ, OPCODE_READ_FW_VERSION, data) {
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

object NoOpBafangMessage: BafangMessage(MessageType.READ, 0x00)
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
        val OPCODE_READ_FW_VERSION: Byte = 0x01;
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
        val checkSum = computeChecksum(message)
        return message + checkSum;
    }

    private fun computeChecksum(buffer: ByteArray): Byte {
        return buffer.sumOf { bob ->
            bob.toUInt()
        }.toByte()
    }
}

interface BafangIMessage {
    fun getType(): MessageType
    fun getOpcode(): Byte
    fun getBytes(): ByteArray
}
