package com.example.graymatter.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.graymatter.android.ui.addtotopic.AddToTopicScreen
import com.example.graymatter.android.ui.home.HomeScreen
import com.example.graymatter.android.ui.resourcedetail.ResourceDetailScreen
import com.example.graymatter.android.ui.library.LibraryScreen
import com.example.graymatter.android.ui.newentry.EntryType
import com.example.graymatter.android.ui.newentry.NewEntryScreen
import com.example.graymatter.android.ui.topicsynthesis.TopicSynthesisScreen
import com.example.graymatter.android.ui.components.MarkdownEditor
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.domain.business.ExportService
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.graymatter.android.ui.profile.ProfileScreen
import com.example.graymatter.android.ui.template.TemplatesManagementScreen
import com.example.graymatter.android.ui.trash.RecentlyDeletedScreen
import com.example.graymatter.android.util.FileUtils
import kotlinx.coroutines.launch

import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.first

/**
 * Main navigation graph for Gray Matter app.
 */
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@androidx.compose.runtime.Composable
fun GrayMatterNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val viewModel: GrayMatterViewModel = koinViewModel()
    val trashViewModel: com.example.graymatter.android.ui.viewmodel.TrashViewModel = koinViewModel()
    val templateViewModel: com.example.graymatter.android.ui.viewmodel.TemplateViewModel = koinViewModel()
    val homeViewModel: com.example.graymatter.android.ui.viewmodel.HomeViewModel = koinViewModel()
    val draftingViewModel: com.example.graymatter.android.ui.viewmodel.DraftingViewModel = koinViewModel()
    val opinionRepository: com.example.graymatter.data.OpinionRepository = koinInject()

    val topics by viewModel.topicsStream.collectAsState(initial = emptyList())
    val items by viewModel.resourceEntriesStream.collectAsState(initial = emptyList())
    val templates by templateViewModel.templates.collectAsState()
    var editingResource by remember { mutableStateOf<com.example.graymatter.domain.Resource?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val referenceSelectorViewModel: com.example.graymatter.viewmodel.ReferenceSelectorViewModel = koinInject()

    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Home.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // Main Pager (Home, Library, Profile)
        composable(
            route = NavigationDestination.Home.route,
            enterTransition = {
                if (initialState.destination.route?.startsWith("knowledge_graph") == true) {
                    // Special case: Slide in from left (towards right/end) from Relatrix
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    )
                } else {
                    // Default forward behavior: Slide in from right (towards start)
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    )
                }
            },
            popEnterTransition = {
                // Default back behavior: Slide in from left (towards right/end)
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        ) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 3 })
            
            androidx.compose.material3.Scaffold(
                bottomBar = {
                    com.example.graymatter.android.ui.GrayMatterBottomBar(
                        currentRoute = when (pagerState.currentPage) {
                            0 -> NavigationDestination.Home.route
                            1 -> NavigationDestination.Library.route
                            2 -> NavigationDestination.Profile.route
                            else -> NavigationDestination.Home.route
                        },
                        onNavigateToHome = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        onNavigateToLibrary = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        onNavigateToProfile = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                    )
                }
            ) { paddingValues ->
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.padding(paddingValues).fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            val continueReadingItem by homeViewModel.continueReadingResourceEntry.collectAsState()
                            val lastOpenedProgress by homeViewModel.lastOpenedProgress.collectAsState()

                            HomeScreen(
                                viewModel = viewModel,
                                homeViewModel = homeViewModel,
                                continueReadingItem = continueReadingItem,
                                continueReadingProgress = lastOpenedProgress,
                                onCreateNewEntryClick = {
                                    navController.navigate(NavigationDestination.NewEntry.route)
                                },
                                onNavigateToLibrary = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                onItemClick = { resourceEntryId ->
                                    navController.navigate(NavigationDestination.ResourceDetail.buildRoute(resourceEntryId))
                                }
                            )
                        }
                        1 -> {
                            LibraryScreen(
                                topics = topics,
                                onTopicClick = { topic ->
                                    navController.navigate(NavigationDestination.TopicDetail.buildRoute(topic.id))
                                },
                                onNavigateToHome = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                },
                                onCreateClick = {
                                    navController.navigate(NavigationDestination.NewEntry.route)
                                },
                                onNavigateToGraph = {
                                    navController.navigate(NavigationDestination.KnowledgeGraph.buildRoute())
                                },
                                onDeleteTopics = { ids -> viewModel.deleteTopics(ids) },
                                onUndoDeleteTopics = { ids -> viewModel.undoDeleteTopics(ids) },
                                onRenameTopic = { id, name -> viewModel.renameTopic(id, name) },
                                onExportTopic = { topic ->
                                    coroutineScope.launch {
                                        val topicItems = viewModel.getResourceEntriesByTopic(topic.id).first()
                                        val markdown = ExportService.exportTopicSummary(topic, topicItems)
                                        shareText(context, markdown, "Topic Analysis: ${topic.name}")
                                    }
                                },
                                onViewTopicInRelatrix = { topicId ->
                                    navController.navigate(NavigationDestination.KnowledgeGraph.buildRoute(topicId))
                                },
                                onUpdateOrder = { ids -> viewModel.updateTopicOrder(ids) }
                            )
                        }
                        2 -> {
                            ProfileScreen(
                                onNavigateToGraph = {
                                    navController.navigate(NavigationDestination.KnowledgeGraph.buildRoute())
                                },
                                onNavigateToTemplates = {
                                    navController.navigate(NavigationDestination.TemplateManagement.route)
                                },
                                onNavigateToRecentlyDeleted = {
                                    navController.navigate(NavigationDestination.RecentlyDeleted.route)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Topic Detail Screen (Synthesis)
        composable(
            route = NavigationDestination.TopicDetail.route,
            arguments = listOf(
                navArgument(NavigationDestination.TopicDetail.ARG_TOPIC_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString(NavigationDestination.TopicDetail.ARG_TOPIC_ID)
            val topic = topics.find { it.id == topicId }
            
            // Collect resources for this topic
            val topicItems by viewModel.getResourceEntriesByTopic(topicId ?: "").collectAsState(initial = emptyList())
            val resources = topicItems.map { it.resource }

            TopicSynthesisScreen(
                topic = topic,
                resources = resources,
                referenceSelectorViewModel = referenceSelectorViewModel,
                onBackClick = { navController.popBackStack() },
                onAddResource = {
                    navController.navigate(NavigationDestination.NewEntry.route + "?topicId=$topicId")
                },
                onResourceClick = { resource ->
                    val item = topicItems.find { it.resource.id == resource.id }?.resourceEntry
                    item?.let {
                        navController.navigate(NavigationDestination.ResourceDetail.buildRoute(it.id))
                    }
                },
                onSaveTopicOverview = { content, links ->
                    topicId?.let { viewModel.updateTopicNotes(it, content, links) }
                },
                onDeleteTopic = {
                    topicId?.let { 
                        viewModel.deleteTopic(it)
                        navController.popBackStack()
                    }
                },
                onRenameTopic = { newName ->
                    topicId?.let { viewModel.renameTopic(it, newName) }
                },
                onExport = {
                    topic?.let { t ->
                        val markdown = ExportService.exportTopicSummary(t, topicItems)
                        shareText(context, markdown, "Topic Analysis: ${t.name}")
                    }
                },
                onViewInGraph = { id -> 
                    navController.navigate(NavigationDestination.KnowledgeGraph.buildRoute(id))
                },
                onLoadLinks = { id -> viewModel.getLinksForTopic(id) }
            )
        }

        // New Entry Screen
        composable(
            route = NavigationDestination.NewEntry.route + "?topicId={topicId}",
            arguments = listOf(
                navArgument("topicId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId")
            NewEntryScreen(
                templateViewModel = templateViewModel,
                draftingViewModel = draftingViewModel,
                referenceSelectorViewModel = referenceSelectorViewModel,
                preSelectedTopicId = topicId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Home.route) { inclusive = true }
                    }
                },
                onNavigateToAddToTopic = { resourceEntryId ->
                    navController.navigate("add_to_topic/$resourceEntryId")
                }
            )
        }

        // Resource Detail Screen
        composable(
            route = NavigationDestination.ResourceDetail.route,
            arguments = listOf(
                navArgument(NavigationDestination.ResourceDetail.ARG_RESOURCE_ENTRY_ID) { type = NavType.StringType },
                navArgument(NavigationDestination.ResourceDetail.ARG_FOCUS_OPINION_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val resourceEntryId = backStackEntry.arguments?.getString(NavigationDestination.ResourceDetail.ARG_RESOURCE_ENTRY_ID) ?: return@composable
            val focusOpinionId = backStackEntry.arguments?.getString(NavigationDestination.ResourceDetail.ARG_FOCUS_OPINION_ID)
            val itemDetails by viewModel.getResourceEntryDetails(resourceEntryId).collectAsState(initial = null)
            val readingProgress by viewModel.getReadingProgressStream(itemDetails?.resource?.id ?: "").collectAsState(initial = null)

            ResourceDetailScreen(
                resourceEntryDetails = itemDetails,
                readingProgress = readingProgress,
                referenceSelectorViewModel = referenceSelectorViewModel,
                onBackClick = { navController.popBackStack() },
                onOpenResource = {
                    itemDetails?.resource?.let { resource ->
                        when (resource.type) {
                            com.example.graymatter.domain.ResourceType.WEB_LINK -> {
                                if (resource.url != null) {
                                    viewModel.openUrlInBrowser(context, resource.url!!)
                                }
                            }
                            com.example.graymatter.domain.ResourceType.PDF,
                            com.example.graymatter.domain.ResourceType.MARKDOWN -> {
                                // Check integrity before opening
                                if (FileUtils.verifyFileExists(resource.filePath)) {
                                    navController.navigate(NavigationDestination.FileViewer.buildRoute(resource.id))
                                } else {
                                    android.widget.Toast.makeText(context, "File missing from storage", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                            else -> {
                                resource.filePath?.let { path ->
                                    FileUtils.openFileWithIntent(context, path)
                                }
                            }
                        }
                    }
                },
                onOpenBookmark = { bookmark ->
                    navController.navigate(NavigationDestination.FileViewer.buildRoute(bookmark.resourceId, bookmark.page))
                },
                onUpdateDescription = { desc, links ->
                    viewModel.updateResourceEntryDescription(resourceEntryId, desc, links)
                },
                onAddOpinion = { text, confidence, selectedLinks ->
                    viewModel.addOpinion(resourceEntryId, text, confidence, referenceLinks = selectedLinks)
                },
                onUpdateOpinion = { opinionId, text, confidence, date, selectedLinks ->
                    viewModel.updateOpinion(opinionId, text, confidence, date, selectedLinks)
                },
                onDeleteOpinion = { opinionId ->
                    viewModel.deleteOpinion(opinionId)
                },
                onRenameResource = { newName ->
                    itemDetails?.resource?.let { resource ->
                        // Maintain extension when renaming if it's a file
                        val finalName = if (resource.type != com.example.graymatter.domain.ResourceType.WEB_LINK && !newName.contains(".")) {
                            val oldExt = resource.title?.substringAfterLast('.', "") ?: ""
                            if (oldExt.isNotEmpty()) "$newName.$oldExt" else newName
                        } else newName
                        
                        viewModel.renameResource(resource.id, finalName)
                    }
                },
                onDeleteResourceEntry = {
                    viewModel.deleteResourceEntry(resourceEntryId)
                },
                onUndoDeleteResourceEntry = {
                    viewModel.undoDeleteResourceEntry(resourceEntryId)
                },
                onUndoDeleteOpinion = { opinionId -> 
                    viewModel.undoDeleteOpinion(opinionId)
                },
                onEditNote = {
                    editingResource = itemDetails?.resource
                },
                onExport = { filteredOpinions ->
                    itemDetails?.let { details ->
                        val markdown = ExportService.exportResourceHistory(details, filteredOpinions)
                        shareText(context, markdown, "Opinion History: ${details.resource.title ?: "Untitled"}")
                    }
                },
                onLoadLinks = { opinionId -> viewModel.getLinksForOpinion(opinionId) },
                onLoadResourceLinks = { resourceId -> viewModel.getLinksForResource(resourceId) },
                onViewInGraphClick = { resourceId -> 
                    navController.navigate(NavigationDestination.KnowledgeGraph.buildRoute(resourceId)) 
                },
                onNavigateToKnowledgeLink = { link ->
                    when (link) {
                        is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> {
                            navController.navigate(NavigationDestination.TopicDetail.buildRoute(link.id))
                        }
                        is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> {
                            val targetItem = items.find { it.resourceId == link.id }
                            if (targetItem != null) {
                                navController.navigate(NavigationDestination.ResourceDetail.buildRoute(targetItem.id))
                            }
                        }
                        is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> {
                            val targetItem = items.find { it.resourceId == link.resourceId }
                            if (targetItem != null) {
                                navController.navigate(NavigationDestination.ResourceDetail.buildRoute(targetItem.id, link.id))
                            }
                        }
                    }
                },
                onSaveTemplate = { templateViewModel.saveTemplate(it) },
                generateUuid = { viewModel.generateUuid() }
            )

            // Overlay the editor if we are editing a note
            if (editingResource != null) {
                var showEditReferenceSelector by remember { mutableStateOf(false) }
                var editReferenceToInsert by remember { mutableStateOf<String?>(null) }
                var editSelectedReferences by remember { mutableStateOf(emptyList<com.example.graymatter.domain.ReferenceSelectorItem>()) }
                var currentEditorText by remember { mutableStateOf(editingResource?.extractedText ?: "") }

                // Load existing references on startup
                LaunchedEffect(editingResource?.id) {
                    editingResource?.id?.let { resId ->
                        viewModel.getLinksForResource(resId).collect { links ->
                            editSelectedReferences = links
                        }
                    }
                }

                // Robust reference synchronizer (matches NewEntryScreen)
                LaunchedEffect(currentEditorText) {
                    val regex = Regex("\\[\\[(.*?)\\]\\]")
                    val foundTexts = regex.findAll(currentEditorText).map { it.groupValues[1] }.toSet()
                    
                    editSelectedReferences = editSelectedReferences.filter { ref ->
                        val refText = when (ref) {
                            is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> ref.name
                            is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> ref.title
                            is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> ref.snippet
                        }
                        foundTexts.contains(refText) || foundTexts.any { it.endsWith(refText) }
                    }.distinctBy { it.id }
                }

                MarkdownEditor(
                    title = editingResource?.title ?: "Untitled",
                    initialText = editingResource?.extractedText ?: "",
                    onBackClick = { editingResource = null },
                    onTextChange = { currentEditorText = it },
                    onTitleChange = { newTitle -> 
                        editingResource?.let { res ->
                            viewModel.renameResource(res.id, newTitle)
                        }
                    },
                    onSave = { newText: String ->
                        editingResource?.let { res ->
                            viewModel.updateResourceText(res.id, newText, editSelectedReferences)
                            // Update the local snapshot so MarkdownEditor tracks saved state correctly
                            editingResource = res.copy(extractedText = newText)
                        }
                    },
                    onShowReferenceSelector = { 
                        referenceSelectorViewModel.clearSelection()
                        showEditReferenceSelector = true 
                    },
                    referenceToInsert = editReferenceToInsert,
                    onReferenceInserted = { editReferenceToInsert = null },
                    onReferenceTap = { refText ->
                        // Resolve the reference text to a navigation target
                        // Strip prefixes like "Topic: ", "Resource: ", "Knowledge: "
                        val cleanText = refText
                            .removePrefix("Topic: ")
                            .removePrefix("Resource: ")
                            .removePrefix("Knowledge: ")
                            .removeSuffix("...")
                            .trim()

                        // Try to find a matching topic (synchronous — already in memory)
                        val matchingTopic = topics.firstOrNull { 
                            it.name.equals(cleanText, ignoreCase = true) 
                        }
                        if (matchingTopic != null) {
                            editingResource = null
                            navController.navigate(NavigationDestination.TopicDetail.buildRoute(matchingTopic.id))
                            return@MarkdownEditor
                        }

                        // Try to find a matching resource (async lookup)
                        coroutineScope.launch {
                            for (entry in items) {
                                val resource = viewModel.getResourceForResourceEntry(entry.resourceId)
                                if (resource?.title?.contains(cleanText, ignoreCase = true) == true) {
                                    editingResource = null
                                    navController.navigate(NavigationDestination.ResourceDetail.buildRoute(entry.id))
                                    return@launch
                                }
                            }
                        }
                    }
                )
                
                if (showEditReferenceSelector) {
                    com.example.graymatter.android.ui.components.ReferenceSelectorSheet(
                        viewModel = referenceSelectorViewModel,
                        onDismissRequest = { showEditReferenceSelector = false },
                        onConfirm = { items ->
                            showEditReferenceSelector = false
                            editSelectedReferences = (editSelectedReferences + items).distinctBy { it.id }
                            
                            if (items.isNotEmpty()) {
                                val item = items.first()
                                val refText = when (item) {
                                    is com.example.graymatter.domain.ReferenceSelectorItem.TopicItem -> item.name
                                    is com.example.graymatter.domain.ReferenceSelectorItem.ResourceItem -> item.title
                                    is com.example.graymatter.domain.ReferenceSelectorItem.DetailItem -> item.snippet
                                }
                                editReferenceToInsert = "[[$refText]]"
                            }
                        }
                    )
                }
            }
        }

        // Add to Topic Screen
        composable(
            route = NavigationDestination.AddToTopic.route,
            arguments = listOf(
                navArgument(NavigationDestination.AddToTopic.ARG_RESOURCE_ENTRY_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val resourceEntryId = backStackEntry.arguments?.getString(NavigationDestination.AddToTopic.ARG_RESOURCE_ENTRY_ID)

            AddToTopicScreen(
                topics = topics,
                onSelectTopic = { topic ->
                    resourceEntryId?.let { homeViewModel.assignTopicToResourceEntry(it, topic.id) }
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Home.route)
                    }
                },
                onCreateNewTopic = { topicName ->
                    coroutineScope.launch {
                        val newTopicId = viewModel.createTopic(topicName)
                        resourceEntryId?.let { homeViewModel.assignTopicToResourceEntry(it, newTopicId) }
                        navController.navigate(NavigationDestination.Home.route) {
                            popUpTo(NavigationDestination.Home.route)
                        }
                    }
                }
            )
        }

        // Search Screen (placeholder)
        composable(NavigationDestination.Search.route) {
            HomeScreen(
                viewModel = viewModel,
                homeViewModel = homeViewModel,
                continueReadingItem = null,
                continueReadingProgress = null,
                onCreateNewEntryClick = {},
                onNavigateToLibrary = { navController.navigate(NavigationDestination.Home.route) },
                onItemClick = {}
            )
        }

        // File Viewer
        composable(
            route = NavigationDestination.FileViewer.route,
            arguments = listOf(
                navArgument(NavigationDestination.FileViewer.ARG_RESOURCE_ID) { type = NavType.StringType },
                navArgument(NavigationDestination.FileViewer.ARG_PAGE) { 
                    type = NavType.IntType
                    defaultValue = -1 
                }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString(NavigationDestination.FileViewer.ARG_RESOURCE_ID) ?: return@composable
            val initialPage = backStackEntry.arguments?.getInt(NavigationDestination.FileViewer.ARG_PAGE) ?: -1
            
            val fileViewerViewModel: com.example.graymatter.android.ui.fileviewer.FileViewerViewModel = koinViewModel()

            com.example.graymatter.android.ui.fileviewer.FileViewerScreen(
                viewModel = fileViewerViewModel,
                referenceSelectorViewModel = referenceSelectorViewModel,
                resourceId = resourceId,
                initialPage = if (initialPage >= 0) initialPage else null,
                onBackClick = { navController.popBackStack() },
                onViewInGraph = { resId -> 
                    navController.navigate(NavigationDestination.KnowledgeGraph.buildRoute(resId)) 
                },
                onNavigateToDictionaryOrigin = { opinionId, itemId ->
                    // Navigate to the resource detail page and focus the dictionary opinion
                    navController.navigate(NavigationDestination.ResourceDetail.buildRoute(itemId, opinionId))
                }
            )
        }

        // Relatrix (Knowledge Graph) Screen
        composable(
            route = NavigationDestination.KnowledgeGraph.route,
            arguments = listOf(
                navArgument(NavigationDestination.KnowledgeGraph.ARG_NODE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val nodeId = backStackEntry.arguments?.getString(NavigationDestination.KnowledgeGraph.ARG_NODE_ID)
            
            val graphViewModel: com.example.graymatter.android.ui.graph.KnowledgeGraphViewModel = koinViewModel()

            // Force fresh data on every navigation — ViewModel is cached so init only runs once
            LaunchedEffect(Unit) {
                graphViewModel.loadGraphData()
            }

            com.example.graymatter.android.ui.graph.KnowledgeGraphScreen(
                viewModel = graphViewModel,
                initialSelectedNodeId = nodeId,
                onBackClick = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Home.route) { inclusive = true }
                    }
                },
                onNodeDoubleTap = { node ->
                    when (node.type) {
                        com.example.graymatter.android.ui.graph.NodeType.TOPIC -> {
                            navController.navigate(NavigationDestination.TopicDetail.buildRoute(node.id))
                        }
                        com.example.graymatter.android.ui.graph.NodeType.RESOURCE -> {
                            // Find corresponding item mapped to this resource
                            val item = viewModel.resourceEntriesStream.value.find { it.resourceId == node.id }
                            if (item != null) navController.navigate(NavigationDestination.ResourceDetail.buildRoute(item.id))
                        }
                        else -> {
                            // Opinions open the ItemDetail for their parent item
                            coroutineScope.launch {
                                val opinion = opinionRepository.getOpinionById(node.id)
                                if (opinion != null) {
                                    navController.navigate(NavigationDestination.ResourceDetail.buildRoute(opinion.itemId, opinion.id))
                                }
                            }
                        }
                    }
                }
            )
        }

        // Template Management Screen
        composable(
            route = NavigationDestination.TemplateManagement.route
        ) {
            TemplatesManagementScreen(
                templateViewModel = templateViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Recently Deleted Screen
        composable(
            route = NavigationDestination.RecentlyDeleted.route
        ) {
            RecentlyDeletedScreen(
                viewModel = viewModel,
                trashViewModel = trashViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun shareText(context: Context, text: String, title: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Gray Matter Export", text)
    clipboard.setPrimaryClip(clip)
    
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
