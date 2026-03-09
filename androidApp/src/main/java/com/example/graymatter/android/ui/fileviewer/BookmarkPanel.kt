package com.example.graymatter.android.ui.fileviewer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.domain.Bookmark

/**
 * Bookmark list panel. Displays reflections and allows jumping to pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkPanel(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkDelete: (Bookmark) -> Unit,
    onAddBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 500.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bookmarks (${bookmarks.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                FilledTonalButton(
                    onClick = onAddBookmark,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(Icons.Default.BookmarkAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontSize = 13.sp)
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No bookmarks yet",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(bookmarks) { bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            onClick = { onBookmarkClick(bookmark) },
                            onDelete = { onBookmarkDelete(bookmark) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.Bookmark,
            null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Metadata chips row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Page Chip
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Page ${bookmark.page + 1}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Confidence Chip (if exists)
                if (bookmark.confidenceScore != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${bookmark.confidenceScore}% Certainty",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Opinion / Summary Text
            if (!bookmark.opinion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bookmark.opinion!!,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                "Delete",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
