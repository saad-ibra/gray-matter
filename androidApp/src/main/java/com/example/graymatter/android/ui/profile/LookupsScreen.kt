package com.example.graymatter.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.LookupsViewModel
import com.example.graymatter.domain.Opinion

@Composable
fun LookupsScreen(
    viewModel: LookupsViewModel,
    onBackClick: () -> Unit,
    onNavigateToOrigin: (itemId: String, opinionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLookups by viewModel.activeLookups.collectAsState()
    val learntLookups by viewModel.learntLookups.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Learnt

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GrayMatterColors.BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GrayMatterColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Lookups",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GrayMatterColors.TextPrimary
                )
            }
            
            // Segmented Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GrayMatterColors.SurfaceDark)
                    .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(24.dp)),
            ) {
                TabItem(
                    text = "Learning",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabItem(
                    text = "Learnt",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val currentList = if (selectedTab == 0) activeLookups else learntLookups
            
            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTab == 0) "No active lookups." else "No learnt lookups yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrayMatterColors.Neutral500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(currentList, key = { it.id }) { opinion ->
                        LookupCard(
                            opinion = opinion,
                            isLearnt = selectedTab == 1,
                            onToggleLearnt = { viewModel.toggleLearntStatus(opinion) },
                            onJumpToOrigin = { onNavigateToOrigin(opinion.itemId, opinion.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) GrayMatterColors.Primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) GrayMatterColors.Primary else GrayMatterColors.Neutral500
        )
    }
}

@Composable
private fun LookupCard(
    opinion: Opinion,
    isLearnt: Boolean,
    onToggleLearnt: () -> Unit,
    onJumpToOrigin: () -> Unit
) {
    // Strip [DICT:42], [DICT], and #learnt tags
    val regex = Regex("\\[DICT(:\\d+)?\\]\\s*")
    val cleanWord = opinion.text.replace(regex, "").replace(" #learnt", "").trim()
    
    val tint = if (isLearnt) GrayMatterColors.Success else GrayMatterColors.TypeLookupMain

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onJumpToOrigin)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanWord,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = tint
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleLearnt,
                    modifier = Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(if (isLearnt) GrayMatterColors.Neutral800 else tint.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = if (isLearnt) Icons.Default.Restore else Icons.Default.Check,
                        contentDescription = if (isLearnt) "Unmark as Learnt" else "Mark as Learnt",
                        tint = if (isLearnt) GrayMatterColors.Neutral400 else tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onJumpToOrigin,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Go to origin",
                        tint = GrayMatterColors.Neutral600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
