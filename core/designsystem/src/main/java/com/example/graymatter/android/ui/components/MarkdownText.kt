package dev.jeziellago.compose.markdowntext

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = markdown,
        modifier = modifier,
        style = style,
        maxLines = maxLines
    )
}
