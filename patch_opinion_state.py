import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'r') as f:
    content = f.read()

state_def = """    var showTemplateEditor by remember { mutableStateOf(false) }

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
"""

content = content.replace("    var showTemplateEditor by remember { mutableStateOf(false) }", state_def)

# Also there's one more place where the old state variables were used: in the ReferenceSelectorSheet
# At the bottom of the file!
bottom_replace = """    if (showReferenceSelector) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = referenceSelectorViewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                opinionSelectedReferences = (opinionSelectedReferences + items).distinctBy { it.id }
            }
        )
    }"""
new_bottom_replace = """    if (showReferenceSelector) {
        com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
            viewModel = referenceSelectorViewModel,
            onDismissRequest = { showReferenceSelector = false },
            onConfirm = { items ->
                showReferenceSelector = false
                if (opinionBlocks.isNotEmpty()) {
                    val newRefs = (opinionBlocks[0].selectedReferences + items).distinctBy { it.id }
                    opinionBlocks[0] = opinionBlocks[0].copy(selectedReferences = newRefs)
                }
            }
        )
    }"""

content = content.replace(bottom_replace, new_bottom_replace)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/newentry/NewEntryScreen.kt', 'w') as f:
    f.write(content)
