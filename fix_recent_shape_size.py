import sys

with open('androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt', 'r') as f:
    content = f.read()

content = content.replace('.height(42.dp)\n                                .clip(CircleShape)', '.size(42.dp)\n                                .clip(CircleShape)')

with open('androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt', 'w') as f:
    f.write(content)

