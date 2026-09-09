import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/viewmodel/DraftingViewModel.kt', 'r') as f:
    content = f.read()

# Add SavedStateHandle to constructor
content = content.replace(
    "private val tagRepository: com.example.graymatter.data.TagRepository\n) : ViewModel() {",
    "private val tagRepository: com.example.graymatter.data.TagRepository,\n    private val savedStateHandle: androidx.lifecycle.SavedStateHandle\n) : ViewModel() {"
)

# Replace MutableStateFlow with SavedStateHandle
old_state_block = """    private val _entryType = MutableStateFlow(EntryType.LINK)
    val entryType: StateFlow<EntryType> = _entryType.asStateFlow()

    private val _draftTitle = MutableStateFlow("")
    val draftTitle: StateFlow<String> = _draftTitle.asStateFlow()

    private val _draftUrl = MutableStateFlow("")
    val draftUrl: StateFlow<String> = _draftUrl.asStateFlow()

    private val _draftOpinion = MutableStateFlow("")
    val draftOpinion: StateFlow<String> = _draftOpinion.asStateFlow()

    private val _draftNoteContent = MutableStateFlow("")
    val draftNoteContent: StateFlow<String> = _draftNoteContent.asStateFlow()

    private val _draftDescription = MutableStateFlow("")
    val draftDescription: StateFlow<String> = _draftDescription.asStateFlow()

    private val _draftConfidence = MutableStateFlow(0.5f)
    val draftConfidence: StateFlow<Float> = _draftConfidence.asStateFlow()

    private val _draftImagePath = MutableStateFlow<String?>(null)
    val draftImagePath: StateFlow<String?> = _draftImagePath.asStateFlow()"""

# We can't use enums easily with getStateFlow without custom parcelers in some old versions, but EntryType is an enum, it should work in recent androidx.lifecycle
# Wait, EntryType is just a standard enum. But let's use strings or primitives if possible. Wait, `savedStateHandle.getStateFlow<EntryType>` works fine for enums in Compose.
new_state_block = """    val entryType = savedStateHandle.getStateFlow("entryType", EntryType.LINK)
    val draftTitle = savedStateHandle.getStateFlow("draftTitle", "")
    val draftUrl = savedStateHandle.getStateFlow("draftUrl", "")
    val draftOpinion = savedStateHandle.getStateFlow("draftOpinion", "")
    val draftNoteContent = savedStateHandle.getStateFlow("draftNoteContent", "")
    val draftDescription = savedStateHandle.getStateFlow("draftDescription", "")
    val draftConfidence = savedStateHandle.getStateFlow("draftConfidence", 0.5f)
    val draftImagePath = savedStateHandle.getStateFlow<String?>("draftImagePath", null)"""

content = content.replace(old_state_block, new_state_block)

# Now update the mutators
content = content.replace("_entryType.value = type", 'savedStateHandle["entryType"] = type')
content = content.replace("_draftTitle.value = title", 'savedStateHandle["draftTitle"] = title')
content = content.replace("_draftUrl.value = url", 'savedStateHandle["draftUrl"] = url')
content = content.replace("_draftOpinion.value = opinion", 'savedStateHandle["draftOpinion"] = opinion')
content = content.replace("_draftNoteContent.value = content", 'savedStateHandle["draftNoteContent"] = content')
content = content.replace("_draftDescription.value = desc", 'savedStateHandle["draftDescription"] = desc')
content = content.replace("_draftConfidence.value = conf", 'savedStateHandle["draftConfidence"] = conf')
content = content.replace("_draftImagePath.value = path", 'savedStateHandle["draftImagePath"] = path')

with open('androidApp/src/main/java/com/example/graymatter/android/ui/viewmodel/DraftingViewModel.kt', 'w') as f:
    f.write(content)
