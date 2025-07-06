package com.example.bafflingvision

enum class MessageType(val code: Byte) {
    BAFANG_READ(0x11),
    BAFANG_WRITE(0x16),
    BBSFW_READ(0x1),
    BBSFW_WRITE(0x2),
    UNKNOWN(0xf)
}