package com.example.graymatter.notes.utils

import platform.Foundation.NSUUID

actual object UUID {
    actual fun randomUUIDString(): String = NSUUID().UUIDString
}