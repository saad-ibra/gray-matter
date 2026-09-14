import re
with open('feature/notes/src/main/java/com/example/graymatter/feature/notes/resourcedetail/ResourceDetailScreen.kt', 'r') as f:
    content = f.read()

target = """                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
            )
        }
    }"""

replacement = """                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .imePadding()
            )
        }
    }"""

content = content.replace(target, replacement)

with open('feature/notes/src/main/java/com/example/graymatter/feature/notes/resourcedetail/ResourceDetailScreen.kt', 'w') as f:
    f.write(content)
