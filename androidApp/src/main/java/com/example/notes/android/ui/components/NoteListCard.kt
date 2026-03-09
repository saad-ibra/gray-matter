package com.example.notes.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notes.domain.Note

@ExperimentalMaterial3Api
@Composable
fun NoteListCard(
    modifier: Modifier = Modifier,
    savedNote: Note,
    onClick: () -> Unit
) {
    OutlinedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = savedNote.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 3
            )
            Text(
                text = savedNote.content,
                maxLines = 3,
                minLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun SwipeableNoteListCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
    savedNote: Note,
) {
    // 1. Use rememberSwipeToDismissBoxState instead of rememberDismissState
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismissed()
                true
            } else {
                false
            }
        }
    )

    // 2. Use SwipeToDismissBox instead of SwipeToDismiss
    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        backgroundContent = { /* Add delete icon/background here if desired */ },
        content = {
            NoteListCard(
                modifier = Modifier.fillMaxWidth(),
                savedNote = savedNote,
                onClick = onClick
            )
        },
        enableDismissFromStartToEnd = false // Only allow swipe from Right to Left
    )
}