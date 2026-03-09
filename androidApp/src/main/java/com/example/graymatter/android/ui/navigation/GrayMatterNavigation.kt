package com.example.graymatter.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.example.graymatter.android.ui.topicsynthesis.OverallOpinionEditor
import com.example.graymatter.android.ui.viewmodel.GrayMatterViewModel
import com.example.graymatter.di.AppModule
import com.example.graymatter.domain.business.ExportService
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Main navigation graph for Gray Matter app.
 */
@Composable
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
            topicRepository = appModule.topicRepository
        )
    }

    val topics by viewModel.topicsStream.collectAsState(initial = emptyList())
    val items by viewModel.itemsStream.collectAsState(initial = emptyList())
    var editingResource by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.graymatter.domain.Resource?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
        // Home Screen
        composable(NavigationDestination.Home.route) {
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
                    navController.navigate(NavigationDestination.Library.route)
                },
                onItemClick = { itemId ->
                    navController.navigate(NavigationDestination.ItemDetail.buildRoute(itemId))
                }
            )
        }

        // Library Screen
        composable(NavigationDestination.Library.route) {
            LibraryScreen(
                topics = topics,
                onTopicClick = { topic ->
                    navController.navigate(NavigationDestination.TopicDetail.buildRoute(topic.id))
                },
                onNavigateToHome = {
                    navController.navigate(NavigationDestination.Home.route) {
                        popUpTo(NavigationDestination.Home.route) { inclusive = true }
                    }
                },
                onCreateClick = {
                    navController.navigate(NavigationDestination.NewEntry.route)
                }
            )
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
            val context = LocalContext.current
            
            // Collect resources for this topic
            val topicItems by viewModel.getItemsByTopic(topicId ?: "").collectAsState(initial = emptyList())
            val resources = topicItems.map { it.resource }

            TopicSynthesisScreen(
                topic = topic,
                resources = resources,
                onBackClick = { navController.popBackStack() },
                onAddResource = {
                    navController.navigate(NavigationDestination.NewEntry.route)
                },
                onResourceClick = { resource ->
                    val item = topicItems.find { it.resource.id == resource.id }?.item
                    item?.let {
                        navController.navigate(NavigationDestination.ItemDetail.buildRoute(it.id))
                    }
                },
                onSaveOverallOpinion = { notes ->
                    topic?.id?.let { viewModel.updateTopicNotes(it, notes) }
                },
                onDeleteTopic = {
                    topicId?.let { 
                        viewModel.deleteTopic(it)
                        navController.popBackStack()
                    }
                },
                onExport = {
                    topic?.let { t ->
                        val markdown = ExportService.exportTopicSummary(t, resources)
                        shareText(context, markdown, "Topic Analysis: ${t.name}")
                    }
                }
            )
        }

        // New Entry Screen
        composable(NavigationDestination.NewEntry.route) {
            NewEntryScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { type, value, opinion, confidence, fileName, description ->
                    coroutineScope.launch {
                        val newItemId = when (type) {
                            EntryType.LINK -> viewModel.createNewItem(value, opinion, confidence, description)
                            EntryType.FILE -> viewModel.createNewItemFromFile(fileName ?: "Unknown", value, opinion, confidence, description)
                            EntryType.NOTE -> viewModel.createNewNote(fileName?.removeSuffix(".md") ?: "Untitled", value, opinion, confidence, description)
                        }
                        
                        navController.navigate(NavigationDestination.AddToTopic.buildRoute(newItemId)) {
                            popUpTo(NavigationDestination.Home.route) { saveState = true }
                        }
                    }
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
            val context = androidx.compose.ui.platform.LocalContext.current

            ItemDetailScreen(
                itemDetails = itemDetails,
                readingProgress = readingProgress,
                onBackClick = { navController.popBackStack() },
                onOpenResource = {
                    itemDetails?.resource?.let { resource ->
                        if (resource.type == com.example.graymatter.domain.ResourceType.WEB_LINK) {
                            if (resource.url != null) {
                                viewModel.openUrlInBrowser(context, resource.url!!)
                            }
                        } else {
                            navController.navigate(NavigationDestination.FileViewer.buildRoute(resource.id))
                        }
                    }
                },
                onOpenBookmark = { bookmark ->
                    navController.navigate(NavigationDestination.FileViewer.buildRoute(bookmark.resourceId, bookmark.page))
                },
                onUpdateDescription = { desc ->
                    itemId.let { viewModel.updateItemDescription(it, desc) }
                },
                onAddOpinion = { text, confidence ->
                    itemId.let { viewModel.addOpinion(it, text, confidence) }
                },
                onUpdateOpinion = { opinionId, text, confidence, date ->
                    viewModel.updateOpinion(opinionId, text, confidence, date)
                },
                onDeleteOpinion = { opinionId ->
                    viewModel.deleteOpinion(opinionId)
                },
                onRenameResource = { newName ->
                    itemDetails?.resource?.let { resource ->
                        viewModel.renameResource(resource.id, newName)
                    }
                },
                onDeleteItem = {
                    viewModel.deleteItem(itemId)
                    navController.popBackStack()
                },
                onEditNote = {
                    editingResource = itemDetails?.resource
                },
                onExport = {
                    itemDetails?.let { details ->
                        val markdown = ExportService.exportItemHistory(details)
                        shareText(context, markdown, "Opinion History: ${details.resource.title ?: "Untitled"}")
                    }
                }
            )

            // Overlay the editor if we are editing a note
            if (editingResource != null) {
                com.example.graymatter.android.ui.topicsynthesis.OverallOpinionEditor(
                    title = "Edit Note: ${editingResource?.title ?: "Untitled"}",
                    initialText = editingResource?.extractedText ?: "",
                    onBackClick = { editingResource = null },
                    onSave = { newText ->
                        editingResource?.let { res ->
                            viewModel.updateResourceText(res.id, newText)
                        }
                        editingResource = null
                    }
                )
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
                    navController.navigate(NavigationDestination.Library.route) {
                        popUpTo(NavigationDestination.Home.route)
                    }
                },
                onCreateNewTopic = { topicName ->
                    coroutineScope.launch {
                        val newTopicId = viewModel.createTopic(topicName)
                        itemId?.let { viewModel.assignTopicToItem(it, newTopicId) }
                        navController.navigate(NavigationDestination.Library.route) {
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
                onNavigateToLibrary = { navController.navigate(NavigationDestination.Library.route) },
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
                    itemRepository = appModule.itemRepository
                )
            }

            com.example.graymatter.android.ui.fileviewer.FileViewerScreen(
                viewModel = fileViewerViewModel,
                resourceId = resourceId,
                initialPage = if (initialPage >= 0) initialPage else null,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun shareText(context: Context, text: String, title: String) {
    // Also copy to clipboard for convenience
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
