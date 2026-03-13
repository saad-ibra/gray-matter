package com.example.graymatter.notes.utils

actual object UUID {
    actual fun randomUUIDString(): String = java.util.UUID.randomUUID().toString()
}