import sys

with open('androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt', 'r') as f:
    content = f.read()

# Fix the drawCircle error by extracting the theme color before the Canvas
canvas_start = '''                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val wheelSize = 160.dp
                    Canvas('''

canvas_fixed = '''                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val wheelSize = 160.dp
                    val selectorRingColor = GrayMatterTheme.colors.textPrimary
                    Canvas('''

content = content.replace(canvas_start, canvas_fixed)

draw_circle = '''                        drawCircle(
                            color = GrayMatterTheme.colors.textPrimary,
                            radius = 10f,
                            center = Offset(selectorX, selectorY),
                            style = Stroke(width = 3f)
                        )'''

draw_circle_fixed = '''                        drawCircle(
                            color = selectorRingColor,
                            radius = 10f,
                            center = Offset(selectorX, selectorY),
                            style = Stroke(width = 3f)
                        )'''

content = content.replace(draw_circle, draw_circle_fixed)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt', 'w') as f:
    f.write(content)
