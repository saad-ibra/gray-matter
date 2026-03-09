package com.example.notes.android.ui.notedetail

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    modifier: Modifier = Modifier,
    noteTitle: String,
    noteContent: String,
    onNoteTitleChange: (String) -> Unit,
    onNoteContentChange: (String) -> Unit,
    onBackButtonClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    // without adding scrollState modifier, the "note content" TextField inside this composable
    // will scroll independent of the screen. This behavior is undesirable, hence use scroll state.
    Column(modifier = modifier.verticalScroll(scrollState)) {
        IconButton(
            modifier = Modifier.statusBarsPadding(),
            onClick = onBackButtonClick,
            content = { Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null) }
        )
        NoteDetailScreenBasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = noteTitle,
            textStyle = MaterialTheme.typography.displaySmall,
            onValueChange = onNoteTitleChange,
            placeholderText = "Title"
        )
        NoteDetailScreenBasicTextField(
            modifier = Modifier.fillMaxSize(),
            value = noteContent,
            onValueChange = onNoteContentChange,
            textStyle = MaterialTheme.typography.titleLarge,
            placeholderText = "Note"
        )
    }
}

@ExperimentalMaterial3Api
@Composable
private fun NoteDetailScreenBasicTextField(
    value: String,
    modifier: Modifier = Modifier,
    placeholderText: String = "",
    textStyle: TextStyle = LocalTextStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onValueChange: (String) -> Unit,
) {
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
    BasicTextField(
        modifier = modifier,
        interactionSource = interactionSource,
        value = value,
        textStyle = mergedTextStyle,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(textColor),
        decorationBox = {
            TextFieldDefaults.DecorationBox(
                interactionSource = interactionSource,
                value = value,
                innerTextField = it,
                enabled = true,
                singleLine = false,
                visualTransformation = VisualTransformation.None,
                placeholder = {
                    Text(
                        modifier = Modifier.alpha(0.7f),
                        text = placeholderText,
                        style = textStyle
                    )
                },
                container = {}
            )
        }
    )
}