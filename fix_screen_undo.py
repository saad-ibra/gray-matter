import re
with open('feature/notes/src/main/java/com/example/graymatter/feature/notes/resourcedetail/ResourceDetailScreen.kt', 'r') as f:
    content = f.read()

target = """    onDeleteBookmark: (String) -> Unit = {},
    onUpdateBookmark: (String, String, Int, Long) -> Unit = { _, _, _, _ -> },"""

replacement = """    onDeleteBookmark: (String) -> Unit = {},
    onUndoDeleteBookmark: (String) -> Unit = {},
    onUpdateBookmark: (String, String, Int, Long) -> Unit = { _, _, _, _ -> },"""

content = content.replace(target, replacement)

target2 = """    var deletedOpinionInfo by remember { mutableStateOf<String?>(null) }
    var deletedResourceInfo by remember { mutableStateOf<Pair<String, String>?>(null) }"""

replacement2 = """    var deletedOpinionInfo by remember { mutableStateOf<String?>(null) }
    var deletedBookmarkInfo by remember { mutableStateOf<String?>(null) }
    var deletedResourceInfo by remember { mutableStateOf<Pair<String, String>?>(null) }"""

content = content.replace(target2, replacement2)

target3 = """        } else if (deletedOpinionInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "Opinion deleted",
                onUndo = {
                    onUndoDeleteOpinion(deletedOpinionInfo!!)
                    deletedOpinionInfo = null
                },
                onDismissRequest = {
                    deletedOpinionInfo = null
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }"""

replacement3 = """        } else if (deletedOpinionInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "Opinion deleted",
                onUndo = {
                    onUndoDeleteOpinion(deletedOpinionInfo!!)
                    deletedOpinionInfo = null
                },
                onDismissRequest = {
                    deletedOpinionInfo = null
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else if (deletedBookmarkInfo != null) {
            com.example.graymatter.android.ui.components.UndoSnackbar(
                message = "Bookmark deleted",
                onUndo = {
                    onUndoDeleteBookmark(deletedBookmarkInfo!!)
                    deletedBookmarkInfo = null
                },
                onDismissRequest = {
                    deletedBookmarkInfo = null
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }"""

content = content.replace(target3, replacement3)

target4 = """                        onDeleteBookmark = onDeleteBookmark,"""

replacement4 = """                        onDeleteBookmark = { bookmarkId ->
                            deletedBookmarkInfo = bookmarkId
                            onDeleteBookmark(bookmarkId)
                        },"""

content = content.replace(target4, replacement4)


with open('feature/notes/src/main/java/com/example/graymatter/feature/notes/resourcedetail/ResourceDetailScreen.kt', 'w') as f:
    f.write(content)
