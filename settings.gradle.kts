// Remove conflicting environment variables for AGP
try {
    val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
    
    fun removeEnv(fieldName: String, key: String) {
        try {
            val field = processEnvClass.getDeclaredField(fieldName)
            field.isAccessible = true
            val map = field.get(null) as? MutableMap<String, String>
            map?.remove(key)
        } catch (_: Throwable) {}
    }

    removeEnv("theEnvironment", "ANDROID_PREFS_ROOT")
    removeEnv("theUnmodifiableEnvironment", "ANDROID_PREFS_ROOT")
} catch (_: Throwable) {}

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GrayMatter"
include(":androidApp")
include(":shared")
