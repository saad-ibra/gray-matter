import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("modifier = androidx.compose.ui.Modifier.zIndex(100f)", "modifier = androidx.compose.ui.Modifier")

with open('androidApp/src/main/java/com/example/graymatter/android/ui/MainActivity.kt', 'w') as f:
    f.write(content)
