import re
with open('androidApp/src/main/java/com/example/graymatter/android/ui/viewmodel/GrayMatterViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.softDeleteBookmark(bookmarkId)
        }
    }"""

replacement = """    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.softDeleteBookmark(bookmarkId)
        }
    }

    fun undoDeleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            resourceRepository.undoDeleteBookmark(bookmarkId)
        }
    }"""

content = content.replace(target, replacement)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/viewmodel/GrayMatterViewModel.kt', 'w') as f:
    f.write(content)
