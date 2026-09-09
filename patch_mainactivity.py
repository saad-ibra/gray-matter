import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/MainActivity.kt', 'r') as f:
    content = f.read()

old_block = """                if (!isAppLockEnabled || isUnlocked) {
                    GrayMatterApp(initialSharedUri = initialSharedUri)
                } else {
                    BiometricLockScreen(
                        onAuthenticate = { biometricAuthManager.authenticate(this@MainActivity) }
                    )
                }"""

new_block = """                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    // Always keep the app in the composition tree to preserve navigation state and saved instance state
                    GrayMatterApp(initialSharedUri = initialSharedUri)
                    
                    if (isAppLockEnabled && !isUnlocked) {
                        BiometricLockScreen(
                            onAuthenticate = { biometricAuthManager.authenticate(this@MainActivity) },
                            modifier = androidx.compose.ui.Modifier.zIndex(100f)
                        )
                    }
                }"""

content = content.replace(old_block, new_block)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/MainActivity.kt', 'w') as f:
    f.write(content)
