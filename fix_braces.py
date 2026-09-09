import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if "modifier = Modifier" in lines[i] and ".fillMaxWidth()" in lines[i+1] and ".navigationBarsPadding()" in lines[i+2]:
        # found the end of SaveButton
        # delete the `    }\n` that follows it
        if lines[i+5].strip() == "}":
            del lines[i+5]
            break

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'w') as f:
    f.writelines(lines)
