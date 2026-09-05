import sys
import math

with open('androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt', 'r') as f:
    content = f.read()

bad_recent = '''                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recentColors.take(18).chunked(6).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowColors.forEach { hexColor ->
                                val color = remember(hexColor) {
                                    try { Color(android.graphics.Color.parseColor(if (hexColor.startsWith("#")) hexColor else "#$hexColor")) }
                                    catch (e: Exception) { Color.Gray }
                                }
                                val isSelected = selectedColor?.let {
                                    val diff = kotlin.math.abs((it.toArgb() and 0xFFFFFF) - (color.toArgb() and 0xFFFFFF))
                                    diff < 2
                                } ?: false

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) GrayMatterTheme.colors.textPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            updateHsvFromColor(color)
                                        }
                                )
                            }
                        }
                    }
                }'''

good_recent = '''                val colorsToShow = recentColors.take(18)
                val rowsCount = kotlin.math.ceil(colorsToShow.size / 6.0).toInt()
                val gridHeight = (rowsCount * 42 + (rowsCount - 1) * 10).dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    items(colorsToShow) { hexColor ->
                        val color = remember(hexColor) {
                            try { Color(android.graphics.Color.parseColor(if (hexColor.startsWith("#")) hexColor else "#$hexColor")) }
                            catch (e: Exception) { Color.Gray }
                        }
                        val isSelected = selectedColor?.let {
                            val diff = kotlin.math.abs((it.toArgb() and 0xFFFFFF) - (color.toArgb() and 0xFFFFFF))
                            diff < 2
                        } ?: false

                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) GrayMatterTheme.colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    updateHsvFromColor(color)
                                }
                        )
                    }
                }'''

content = content.replace(bad_recent, good_recent)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/library/TopicColorPickerSheet.kt', 'w') as f:
    f.write(content)

