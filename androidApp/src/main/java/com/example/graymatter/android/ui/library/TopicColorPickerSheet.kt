package com.example.graymatter.android.ui.library

import android.graphics.ComposePathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterTheme
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicColorPickerSheet(
    initialColor: String?,
    recentColors: List<String>,
    onColorSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val suggestedColors = listOf(
        // Row 1: Reds → Purples
        "#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6",
        // Row 2: Blues → Greens
        "#4FC3F7", "#4DD0E1", "#4DB6AC", "#26A69A", "#66BB6A", "#81C784",
        // Row 3: Light Greens → Yellows
        "#AED581", "#C5E1A5", "#DCE775", "#FFF176", "#FFD54F", "#FFCA28",
        // Row 4: Oranges → Neutrals
        "#FFB74D", "#FF8A65", "#A1887F", "#BCAAA4", "#90A4AE", "#78909C",
        // Row 5: Saturated accents
        "#EF5350", "#EC407A", "#AB47BC", "#7E57C2", "#5C6BC0", "#42A5F5",
        // Row 6: Deep / rich tones
        "#29B6F6", "#26C6DA", "#009688", "#2E7D32", "#F9A825", "#E65100"
    )

    // Current selected color state
    var selectedColor by remember {
        mutableStateOf(
            initialColor?.let {
                try { Color(android.graphics.Color.parseColor(it)) }
                catch (e: Exception) { null }
            }
        )
    }

    // HSV state for the wheel
    var hue by remember {
        mutableFloatStateOf(
            selectedColor?.let {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(it.toArgb(), hsv)
                hsv[0]
            } ?: 0f
        )
    }
    var saturation by remember {
        mutableFloatStateOf(
            selectedColor?.let {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(it.toArgb(), hsv)
                hsv[1]
            } ?: 1f
        )
    }
    var brightness by remember {
        mutableFloatStateOf(
            selectedColor?.let {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(it.toArgb(), hsv)
                hsv[2]
            } ?: 1f
        )
    }

    // Hex text field state
    var hexText by remember {
        mutableStateOf(
            initialColor?.removePrefix("#")?.uppercase() ?: "FFFFFF"
        )
    }
    var hexError by remember { mutableStateOf(false) }

    // Sync color from HSV
    fun colorFromHsv(): Color {
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }

    // Sync hex from current HSV
    fun updateHexFromHsv() {
        val c = colorFromHsv()
        val argb = c.toArgb()
        hexText = String.format("%06X", argb and 0xFFFFFF)
        hexError = false
        selectedColor = c
    }

    // Sync HSV from a Color
    fun updateHsvFromColor(color: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        selectedColor = color
        hexText = String.format("%06X", color.toArgb() and 0xFFFFFF)
        hexError = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GrayMatterTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Topic Color",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GrayMatterTheme.colors.textPrimary
            )

            // ── Recently Used Colors ──
            if (recentColors.isNotEmpty()) {
                Text(
                    text = "RECENTLY USED",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                    color = GrayMatterTheme.colors.neutral500
                )
                Column(
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
                }
                Divider(color = GrayMatterTheme.colors.neutral800, thickness = 1.dp)
            }

            // ── Suggested Colors Grid (6×6) ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Clear color option
                item {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GrayMatterTheme.colors.background)
                            .border(1.dp, GrayMatterTheme.colors.neutral700, CircleShape)
                            .clickable {
                                onColorSelected(null)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = GrayMatterTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(suggestedColors) { hexColor ->
                    val color = remember(hexColor) {
                        try { Color(android.graphics.Color.parseColor(hexColor)) }
                        catch (e: Exception) { Color.Gray }
                    }
                    val isSelected = selectedColor?.let {
                        val diff = abs((it.toArgb() and 0xFFFFFF) - (color.toArgb() and 0xFFFFFF))
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

            // ── Divider ──
            Divider(color = GrayMatterTheme.colors.neutral800, thickness = 1.dp)

            // ── Custom Color Section ──
            Text(
                text = "CUSTOM COLOR",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = GrayMatterTheme.colors.neutral500
            )

            // Color wheel + brightness slider side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Color Wheel ──
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val wheelSize = 160.dp
                    val selectorRingColor = GrayMatterTheme.colors.textPrimary
                    Canvas(
                        modifier = Modifier
                            .size(wheelSize)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dist <= radius) {
                                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        hue = ((angle + 360) % 360)
                                        saturation = (dist / radius).coerceIn(0f, 1f)
                                        updateHexFromHsv()
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    val dx = change.position.x - center.x
                                    val dy = change.position.y - center.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dist <= radius) {
                                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        hue = ((angle + 360) % 360)
                                        saturation = (dist / radius).coerceIn(0f, 1f)
                                        updateHexFromHsv()
                                    }
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        // Draw color wheel using concentric rings
                        val steps = 60
                        val ringSteps = 20
                        for (r in ringSteps downTo 1) {
                            val ringRadius = radius * r / ringSteps
                            val ringBrightness = brightness
                            for (i in 0 until steps) {
                                val startAngle = (360f / steps) * i
                                val sweepAngle = (360f / steps) + 1f
                                val ringHue = startAngle
                                val ringSat = r.toFloat() / ringSteps
                                val ringColor = Color(
                                    android.graphics.Color.HSVToColor(
                                        floatArrayOf(ringHue, ringSat, ringBrightness)
                                    )
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2)
                                )
                            }
                        }

                        // Draw selector indicator
                        val selectorAngle = Math.toRadians(hue.toDouble())
                        val selectorDist = saturation * radius
                        val selectorX = center.x + (selectorDist * cos(selectorAngle)).toFloat()
                        val selectorY = center.y + (selectorDist * sin(selectorAngle)).toFloat()

                        drawCircle(
                            color = selectorRingColor,
                            radius = 10f,
                            center = Offset(selectorX, selectorY),
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 8f,
                            center = Offset(selectorX, selectorY),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                // ── Brightness Slider ──
                Column(
                    modifier = Modifier.width(32.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))),
                                        Color.Black
                                    )
                                )
                            )
                            .pointerInput(hue, saturation) {
                                detectTapGestures { offset ->
                                    brightness = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                    updateHexFromHsv()
                                }
                            }
                            .pointerInput(hue, saturation) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    brightness = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                    updateHexFromHsv()
                                }
                            }
                    ) {
                        // Brightness indicator
                        val indicatorY = (1f - brightness)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = with(LocalDensity.current) { (indicatorY * 140).dp })
                                .height(4.dp)
                                .background(GrayMatterTheme.colors.textPrimary, RoundedCornerShape(2.dp))
                        )
                    }

                }
            }

            // ── Preview + HEX Input Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color preview circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(selectedColor ?: GrayMatterTheme.colors.background)
                        .border(
                            width = 2.dp,
                            color = if (selectedColor != null) Color.White.copy(alpha = 0.3f) else GrayMatterTheme.colors.neutral700,
                            shape = CircleShape
                        )
                )

                // HEX input
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GrayMatterTheme.colors.surfaceInput)
                        .border(
                            1.dp,
                            if (hexError) Color(0xFFCF6679) else GrayMatterTheme.colors.neutral800,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = GrayMatterTheme.colors.neutral500
                    )
                    BasicTextField(
                        value = hexText,
                        onValueChange = { newValue ->
                            val filtered = newValue.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            hexText = filtered
                            if (filtered.length == 6) {
                                try {
                                    val parsedColor = Color(android.graphics.Color.parseColor("#$filtered"))
                                    updateHsvFromColor(parsedColor)
                                    hexError = false
                                } catch (e: Exception) {
                                    hexError = true
                                }
                            } else {
                                hexError = filtered.isNotEmpty()
                            }
                        },
                        textStyle = TextStyle(
                            color = GrayMatterTheme.colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(GrayMatterTheme.colors.primary),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Select button
                Button(
                    onClick = {
                        selectedColor?.let { color ->
                            val hex = String.format("#%06X", color.toArgb() and 0xFFFFFF)
                            onColorSelected(hex)
                        } ?: onColorSelected(null)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterTheme.colors.primary,
                        contentColor = GrayMatterTheme.colors.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
