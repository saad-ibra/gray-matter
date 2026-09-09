import re
import sys

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    content = f.read()

# 1. Define the OpinionState and list right before the content Column
state_def = """
    // Opinion UI State for multiple opinions
    data class OpinionBlockState(
        val id: String = java.util.UUID.randomUUID().toString(),
        var text: String = "",
        var confidence: Float = 0.5f,
        var selectedTemplate: com.example.graymatter.domain.CustomTemplate? = null,
        var templateFieldValues: Map<String, String> = emptyMap(),
        var imagePath: String? = null,
        var selectedTags: List<com.example.graymatter.domain.Tag> = emptyList(),
        var selectedReferences: List<com.example.graymatter.domain.ReferenceSelectorItem> = emptyList(),
        var isConnectionsExpanded: Boolean = false,
        var showTagConsole: Boolean = false
    )
    val opinionBlocks = remember { androidx.compose.runtime.mutableStateListOf(OpinionBlockState()) }

    // Sync first block with VM if it's new (for backwards compatibility with other screens feeding data)
    androidx.compose.runtime.LaunchedEffect(opinionText, confidenceScore, currentImagePath) {
        if (opinionBlocks.size == 1 && opinionBlocks[0].text.isEmpty() && opinionText.isNotEmpty()) {
            opinionBlocks[0] = opinionBlocks[0].copy(
                text = opinionText,
                confidence = confidenceScore,
                imagePath = currentImagePath
            )
        }
    }

    Column(
"""
content = content.replace("    Column(\n        modifier = Modifier\n            .fillMaxSize()", state_def + "        modifier = Modifier\n            .fillMaxSize()")

# Now I'll replace the entire Opinion box logic.
# Wait, replacing hundreds of lines of code with a script is risky if regex fails or indentation changes.
