package com.example.graymatter.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DatasetLinked
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.domain.ReferenceType

data class BacklinkUiModel(
    val id: String,  // ID of the source (usually an Opinion or Item)
    val title: String,
    val subtitle: String,
    val sourceType: ReferenceType
)

@Composable
fun BacklinkPanel(
    backlinks: List<BacklinkUiModel>,
    onBacklinkClick: (BacklinkUiModel) -> Unit,
    onViewInGraphClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (backlinks.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DatasetLinked,
                contentDescription = "Backlinks",
                tint = GrayMatterColors.TypeLink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${backlinks.size} Linked Reference${if (backlinks.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = "Expand",
                tint = GrayMatterColors.Neutral500,
                modifier = Modifier.size(24.dp)
            )
        }

        // Expanded Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                backlinks.forEach { backlink ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrayMatterColors.BackgroundDark)
                            .clickable { onBacklinkClick(backlink) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = backlink.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = GrayMatterColors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = backlink.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMatterColors.Neutral500,
                                maxLines = 2
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Go",
                            tint = GrayMatterColors.TypeLink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // View in Graph Button
                Button(
                    onClick = onViewInGraphClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrayMatterColors.TypeLink.copy(alpha = 0.15f),
                        contentColor = GrayMatterColors.TypeLink
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DatasetLinked, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View in Relatrix", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
