import re
with open('androidApp/src/main/java/com/example/graymatter/android/ui/navigation/GrayMatterNavigation.kt', 'r') as f:
    content = f.read()

target = """                onDeleteBookmark = { bookmarkId ->
                    viewModel.deleteBookmark(bookmarkId)
                },
                onUpdateBookmark = { id, text, conf, date ->
                    viewModel.updateBookmark(id, text, conf, date)
                },"""

replacement = """                onDeleteBookmark = { bookmarkId ->
                    viewModel.deleteBookmark(bookmarkId)
                },
                onUndoDeleteBookmark = { bookmarkId ->
                    viewModel.undoDeleteBookmark(bookmarkId)
                },
                onUpdateBookmark = { id, text, conf, date ->
                    viewModel.updateBookmark(id, text, conf, date)
                },"""

content = content.replace(target, replacement)

target2 = """                onDeleteOpinion = { opinionId ->
                    viewModel.deleteOpinion(opinionId)
                },
                onUndoDeleteOpinion = { opinionId ->
                    viewModel.undoDeleteOpinion(opinionId)
                },"""

replacement2 = """                onDeleteOpinion = { opinionId ->
                    viewModel.deleteOpinion(opinionId)
                },
                onUndoDeleteOpinion = { opinionId ->
                    viewModel.undoDeleteOpinion(opinionId)
                },"""

# Wait, onUndoDeleteOpinion is already passed in GrayMatterNavigation.kt? Let's check.
with open('androidApp/src/main/java/com/example/graymatter/android/ui/navigation/GrayMatterNavigation.kt', 'w') as f:
    f.write(content)
