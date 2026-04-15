package com.example.graymatter.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ResourceType

@Composable
fun RecentItemCard(
    title: String,
    time: String,
    type: ResourceType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        ResourceType.WEB_LINK -> Icons.Default.Language
        ResourceType.MARKDOWN -> Icons.Default.EditNote
        ResourceType.PDF -> Icons.Default.PictureAsPdf
        ResourceType.IMAGE -> Icons.Default.Image
        else -> Icons.Default.Description
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GrayMatterColors.BackgroundDark)
                        .border(1.dp, GrayMatterColors.Neutral700, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (type == ResourceType.MARKDOWN) GrayMatterColors.Primary else GrayMatterColors.Neutral500,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = GrayMatterColors.TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMatterColors.Neutral600
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = GrayMatterColors.Neutral700, modifier = Modifier.size(20.dp))
        }
    }
}
