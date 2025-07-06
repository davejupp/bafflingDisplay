package com.example.bafflingvision

data class FWVersion(val major: Byte, val minor: Byte, val patch: Byte) {
    override fun toString(): String {
        return "Version ${major}.${minor}.${patch}"
    }
}