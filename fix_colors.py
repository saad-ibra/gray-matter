import sys

def replace_in_file(filepath, old_text, new_text):
    with open(filepath, 'r') as f:
        content = f.read()
    content = content.replace(old_text, new_text)
    with open(filepath, 'w') as f:
        f.write(content)

# 1. NewEntryScreen (Vision button)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt',
    'tint = Color.White,',
    'tint = com.example.graymatter.android.ui.theme.GrayMatterTheme.colors.textPrimary,'
)

# 2. TemplateComponents (Template button)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/components/TemplateComponents.kt',
    'tint = Color.White,',
    'tint = GrayMatterTheme.colors.textPrimary,'
)

# 3. ResourceDetailScreen (Vision button, links, tags)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/resourcedetail/ResourceDetailScreen.kt',
    'tint = Color.White,',
    'tint = com.example.graymatter.android.ui.theme.GrayMatterTheme.colors.textPrimary,'
)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/resourcedetail/ResourceDetailScreen.kt',
    'leadingIconContentColor = Color.White',
    'leadingIconContentColor = GrayMatterTheme.colors.textPrimary'
)

# 4. TopicColorPickerSheet (borders and indicators)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt',
    'color = if (isSelected) Color.White else Color.Transparent,',
    'color = if (isSelected) GrayMatterTheme.colors.textPrimary else Color.Transparent,'
)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt',
    'color = Color.White,',
    'color = GrayMatterTheme.colors.textPrimary,'
)
replace_in_file(
    'androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt',
    'background(Color.White, RoundedCornerShape(2.dp))',
    'background(GrayMatterTheme.colors.textPrimary, RoundedCornerShape(2.dp))'
)
