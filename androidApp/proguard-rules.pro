# Add project specific ProGuard rules here.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable

# Keep SQLCipher native handles and JNI methods
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
