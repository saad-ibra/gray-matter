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
import com.example.graymatter.android.ui.itemdetail.ItemDetailScreen
import com.example.graymatter.android.ui.library.LibraryScreen
import com.example.graymatter.android.ui.newentry.EntryType
import com.example.graymatter.android.ui.newentry.NewEntryScreen
import com.example.graymatter.android.ui.topicsynthesis.TopicSynthesisScreen
import com.example.graymatter.android.ui.components.MarkdownEditor
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.di.AppModule
import com.example.graymatter.domain.business.ExportService
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.graymatter.android.ui.profile.ProfileScreen
import com.example.graymatter.android.util.FileUtils
import kotlinx.coroutines.launch

/**
 * Main navigation graph for Gray Matter app.
 */
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@androidx.compose.runtime.Composable
fun GrayMatterNavigation(
    appModule: AppModule,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val viewModel: GrayMatterViewModel = viewModel {
        GrayMatterViewModel(
            itemRepository = appModule.itemRepository,
            resourceRepository = appModule.resourceRepository,
            opinionRepository = appModule.opinionRepository,
            topicRepository = appModule.topicRepository,
            referenceLinkRepository = appModule.referenceLinkRepository
        )
    }

    val topics by viewModel.topicsStream.collectAsState(initial = emptyList())
    val items by viewModel.itemsStream.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState()
    var editingResource by remember { mutableStateOf<com.example.graymatter.domain.Resource?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val referenceSelectorViewModel = remember {
        com.example.graymatter.viewmodel.ReferenceSelectorViewModel(
            topicRepository = appModule.topicRepository,
            resourceRepository = appModule.resourceRepository,
            opinionRepository = appModule.opinionRepository,
            itemRepository = appModule.itemRepository,
            coroutineScope = null,
            defaultDispatcher = kotlinx.coroutines.Dispatchers.Default
        )
    }

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
        composable(NavigationDestination.Home.route) {
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
                            val continueReadingItem by viewModel.continueReadingItem.collectAsState()
                            val lastOpenedProgress by viewModel.lastOpenedProgress.collectAsState()

                            HomeScreen(
                                viewModel = viewModel,
                                continueReadingItem = continueReadingItem,
                                continueReadingProgress = lastOpenedProgress,
                                onCreateNewEntryClick = {
                                    navController.navigate(NavigationDestination.NewEntry.route)
                                },
                                onNavigateToLibrary = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                onItemClick = { itemId ->
                                    navController.navigate(NavigationDestination.ItemDetail.buildRoute(itemId))
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
                                    navController.navigate(NavigationDestination.KnowledgeGraph.route)
                                },
                                onDeleteTopics = { ids -> viewModel.deleteTopics(ids) },
                                onRenameTopic = { id, name -> viewModel.renameTopic(id, name) },
                                onUpdateOrder = { ids -> viewModel.updateTopicOrder(ids) }
                            )
                        }
                        2 -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onNavigateToGraph = {
                                    navController.navigate(NavigationDestination.KnowledgeGraph.route)
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
            val topicItems by viewModel.getItemsByTopic(topicId ?: "").collectAsState(initial = emptyList())
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
                    val item = topicItems.find { it.resource.id == resource.id }?.item
                    item?.let {
                        navController.navigate(NavigationDestination.ItemDetail.buildRoute(it.id))
                    }
                },
                onSaveOverallOpinion = { notes, selectedReferences ->
                    topic?.id?.let { viewModel.updateTopicNotes(it, notes, selectedReferences) }
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
                onLoadLinks = { topicId -> viewModel.getLinksForTopic(topicId) }
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
                viewModel = viewModel,
                referenceSelectorViewModel = referenceSelectorViewModel,
                preSelectedTopicId = topicId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Home.route) { inclusive = true }
                    }
                },
                onNavigateToAddToTopic = { itemId ->
                    navController.navigate("add_to_topic/$itemId")
                }
            )
        }

        // Item Detail Screen
        composable(
            route = NavigationDestination.ItemDetail.route,
            arguments = listOf(
                navArgument(NavigationDestination.ItemDetail.ARG_ITEM_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString(NavigationDestination.ItemDetail.ARG_ITEM_ID) ?: return@composable
            val itemDetails by viewModel.getItemDetails(itemId).collectAsState(initial = null)
            val readingProgress by viewModel.getReadingProgressStream(itemDetails?.resource?.id ?: "").collectAsState(initial = null)

            ItemDetailScreen(
                itemDetails = itemDetails,
                readingProgress = readingProgress,
                templates = templates,
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
                onUpdateDescription = { desc ->
                    itemId.let { viewModel.updateItemDescription(it, desc) }
                },
                onAddOpinion = { text, confidence, selectedLinks ->
                    itemId.let { viewModel.addOpinion(it, text, confidence, referenceLinks = selectedLinks) }
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
                onDeleteItem = {
                    viewModel.deleteItem(itemId)
                    navController.popBackStack()
                },
                onEditNote = {
                    editingResource = itemDetails?.resource
                },
                onExport = { filteredOpinions ->
                    itemDetails?.let { details ->
                        val markdown = ExportService.exportItemHistory(details, filteredOpinions)
                        shareText(context, markdown, "Opinion History: ${details.resource.title ?: "Untitled"}")
                    }
                },
                onLoadLinks = { opinionId -> viewModel.getLinksForOpinion(opinionId) },
                onLoadResourceLinks = { resourceId -> viewModel.getLinksForResource(resourceId) },
                onLoadBacklinks = { resourceId -> viewModel.getResolvedBacklinksForTarget(resourceId) },
                onViewInGraphClick = { resourceId -> 
                    navController.navigate(NavigationDestination.KnowledgeGraph.route) 
                }
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

                val coroutineScope = rememberCoroutineScope()
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
                        }
                        editingResource = null
                    },
                    onShowReferenceSelector = { 
                        referenceSelectorViewModel.clearSelection()
                        showEditReferenceSelector = true 
                    },
                    referenceToInsert = editReferenceToInsert,
                    onReferenceInserted = { editReferenceToInsert = null }
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
                navArgument(NavigationDestination.AddToTopic.ARG_ITEM_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString(NavigationDestination.AddToTopic.ARG_ITEM_ID)

            AddToTopicScreen(
                topics = topics,
                onBackClick = { navController.popBackStack() },
                onSelectTopic = { topic ->
                    itemId?.let { viewModel.assignTopicToItem(it, topic.id) }
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Home.route)
                    }
                },
                onCreateNewTopic = { topicName ->
                    coroutineScope.launch {
                        val newTopicId = viewModel.createTopic(topicName)
                        itemId?.let { viewModel.assignTopicToItem(it, newTopicId) }
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
            
            val fileViewerViewModel: com.example.graymatter.android.ui.fileviewer.FileViewerViewModel = viewModel {
                com.example.graymatter.android.ui.fileviewer.FileViewerViewModel(
                    resourceRepository = appModule.resourceRepository,
                    opinionRepository = appModule.opinionRepository,
                    itemRepository = appModule.itemRepository,
                    referenceLinkRepository = appModule.referenceLinkRepository
                )
            }

            com.example.graymatter.android.ui.fileviewer.FileViewerScreen(
                viewModel = fileViewerViewModel,
                referenceSelectorViewModel = referenceSelectorViewModel,
                resourceId = resourceId,
                initialPage = if (initialPage >= 0) initialPage else null,
                onBackClick = { navController.popBackStack() },
                onLoadBacklinks = { resId -> viewModel.getResolvedBacklinksForTarget(resId) },
                onViewInGraph = { resId -> 
                    navController.navigate(NavigationDestination.KnowledgeGraph.route) 
                }
            )
        }

        // Rela-trix (Knowledge Graph) Screen
        composable(NavigationDestination.KnowledgeGraph.route) {
            val graphViewModel: com.example.graymatter.android.ui.graph.KnowledgeGraphViewModel = viewModel {
                com.example.graymatter.android.ui.graph.KnowledgeGraphViewModel(
                    topicRepository = appModule.topicRepository,
                    itemRepository = appModule.itemRepository,
                    resourceRepository = appModule.resourceRepository,
                    opinionRepository = appModule.opinionRepository,
                    referenceLinkRepository = appModule.referenceLinkRepository
                )
            }
            com.example.graymatter.android.ui.graph.KnowledgeGraphScreen(
                viewModel = graphViewModel,
                onBackClick = { navController.popBackStack() },
                onNodeDoubleTap = { node ->
                    when (node.type) {
                        com.example.graymatter.android.ui.graph.NodeType.TOPIC -> {
                            navController.navigate(NavigationDestination.TopicDetail.buildRoute(node.id))
                        }
                        com.example.graymatter.android.ui.graph.NodeType.RESOURCE -> {
                            // Find corresponding item mapped to this resource
                            val item = viewModel.itemsStream.value.find { it.resourceId == node.id }
                            if (item != null) navController.navigate(NavigationDestination.ItemDetail.buildRoute(item.id))
                        }
                        else -> {
                            // Opinions open the ItemDetail for their parent item
                            coroutineScope.launch {
                                val opinion = appModule.opinionRepository.getOpinionById(node.id)
                                if (opinion != null) {
                                    navController.navigate(NavigationDestination.ItemDetail.buildRoute(opinion.itemId))
                                }
                            }
                        }
                    }
                }
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
