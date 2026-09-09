import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if "modifier = Modifier" in lines[i] and ".fillMaxWidth()" in lines[i+1] and ".navigationBarsPadding()" in lines[i+2]:
        # we found the SaveButton modifier. Let's find the `}` after the `)`
        for j in range(i, i+15):
            if lines[j].strip() == "}":
                # delete this first `}`
                del lines[j]
                break
        break

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'w') as f:
    f.writelines(lines)
