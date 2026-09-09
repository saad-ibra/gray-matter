import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1

for i, line in enumerate(lines):
    if "// Grouped Opinion and Confidence Container" in line:
        start_idx = i
    if "// Save Button logic" in line:
        end_idx = i

if start_idx != -1 and end_idx != -1:
    print(f"Found block from {start_idx} to {end_idx}")
else:
    print("Block not found!")
