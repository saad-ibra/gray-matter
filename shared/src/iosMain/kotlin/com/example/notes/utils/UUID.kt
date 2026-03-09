package com.example.notes.utils

import platform.Foundation.NSUUID

actual object UUID {
    actual fun randomUUIDString(): String = NSUUID().UUIDString
}