import os
import re

root_dir = "/Users/saadibrahimkhan/GrayMatter/androidApp/src/main/java/com/example/graymatter"

skip_files = [
    "GrayMatterColors.kt",
    "GrayMatterTheme.kt",
    "GrayMatterTypography.kt",
    "FileViewerScreen.kt",
    "PdfViewerContent.kt",
    "TextSelectionOverlay.kt",
    "DisplaySettingsSheet.kt"
]

replacements = {
    "GrayMatterColors.BackgroundDark": "GrayMatterTheme.colors.background",
    "GrayMatterColors.SurfaceDark": "GrayMatterTheme.colors.surface",
    "GrayMatterColors.SurfaceInput": "GrayMatterTheme.colors.surfaceInput",
    "GrayMatterColors.SurfaceBorder": "GrayMatterTheme.colors.surfaceBorder",
    "GrayMatterColors.SurfaceCard": "GrayMatterTheme.colors.surfaceCard",
    "GrayMatterColors.TextPrimary": "GrayMatterTheme.colors.textPrimary",
    "GrayMatterColors.TextSecondary": "GrayMatterTheme.colors.textSecondary",
    "GrayMatterColors.TextTertiary": "GrayMatterTheme.colors.textTertiary",
    "GrayMatterColors.TextMuted": "GrayMatterTheme.colors.textMuted",
    "GrayMatterColors.Primary": "GrayMatterTheme.colors.primary",
    "GrayMatterColors.OnPrimary": "GrayMatterTheme.colors.onPrimary",
    "GrayMatterColors.Neutral800": "GrayMatterTheme.colors.neutral800",
    "GrayMatterColors.Neutral700": "GrayMatterTheme.colors.neutral700",
    "GrayMatterColors.Neutral600": "GrayMatterTheme.colors.neutral600",
    "GrayMatterColors.Neutral500": "GrayMatterTheme.colors.neutral500",
    "GrayMatterColors.Error": "GrayMatterTheme.colors.error",
    "Color(0xFF1A1A1E)": "GrayMatterTheme.colors.neutral800",
    "Color(0xFF0C0C0C)": "GrayMatterTheme.colors.surface",
}

def process_file(filepath):
    filename = os.path.basename(filepath)
    if filename in skip_files:
        return

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    
    # Do replacements using word boundaries where applicable to prevent partial matches
    # Wait, GrayMatterColors.Primary should be safe to replace directly because there is no PrimaryContainer etc.
    # Color(0xFF1A1A1E) should also be safe.
    for old, new in replacements.items():
        if "Color" in old:
            # literal string replace
            content = content.replace(old, new)
        else:
            # ensure we don't partially match, e.g. Primary vs PrimaryContainer
            # But we checked, there is no PrimaryContainer. Still, it's safer to use word boundaries or just replace.
            content = re.sub(r'\b' + old.replace('.', r'\.') + r'\b', new, content)
            
    if content != original_content:
        # Check if we need to add the import
        import_stmt = "import com.example.graymatter.android.ui.theme.GrayMatterTheme"
        if "GrayMatterTheme.colors" in content and import_stmt not in content:
            # find first import
            lines = content.split('\n')
            insert_idx = -1
            for i, line in enumerate(lines):
                if line.startswith('import '):
                    insert_idx = i
                    break
            
            if insert_idx != -1:
                lines.insert(insert_idx, import_stmt)
            else:
                # no imports, insert after package
                for i, line in enumerate(lines):
                    if line.startswith('package '):
                        lines.insert(i + 1, '')
                        lines.insert(i + 2, import_stmt)
                        break
            content = '\n'.join(lines)
            
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, _, files in os.walk(root_dir):
    for f in files:
        if f.endswith('.kt'):
            process_file(os.path.join(root, f))
