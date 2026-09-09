import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    content = f.read()

print("File size:", len(content))
