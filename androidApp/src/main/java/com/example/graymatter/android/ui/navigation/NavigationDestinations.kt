package com.example.graymatter.android.ui.navigation

/**
 * Navigation destinations for Gray Matter app.
 */
sealed class NavigationDestination(val route: String) {
    
    object Home : NavigationDestination("home")
    
    object Library : NavigationDestination("library")
    
    object Profile : NavigationDestination("profile")
    
    object NewEntry : NavigationDestination("new_entry")
    
    object ResourceDetail : NavigationDestination("resource_detail/{resourceEntryId}?focusOpinionId={focusOpinionId}") {
        const val ARG_RESOURCE_ENTRY_ID = "resourceEntryId"
        const val ARG_FOCUS_OPINION_ID = "focusOpinionId"
        fun buildRoute(resourceEntryId: String, focusOpinionId: String? = null) = 
            if (focusOpinionId != null) "resource_detail/$resourceEntryId?focusOpinionId=$focusOpinionId"
            else "resource_detail/$resourceEntryId"
    }
    
    object AddToTopic : NavigationDestination("add_to_topic/{resourceEntryId}") {
        const val ARG_RESOURCE_ENTRY_ID = "resourceEntryId"
        fun buildRoute(resourceEntryId: String) = "add_to_topic/$resourceEntryId"
    }
    
    object TopicDetail : NavigationDestination("topic_detail/{topicId}") {
        const val ARG_TOPIC_ID = "topicId"
        fun buildRoute(topicId: String) = "topic_detail/$topicId"
    }
    
    object Search : NavigationDestination("search")

    object FileViewer : NavigationDestination("file_viewer/{resourceId}?page={page}") {
        const val ARG_RESOURCE_ID = "resourceId"
        const val ARG_PAGE = "page"
        fun buildRoute(resourceId: String, page: Int? = null) = 
            if (page != null) "file_viewer/$resourceId?page=$page" else "file_viewer/$resourceId"
    }

    object KnowledgeGraph : NavigationDestination("knowledge_graph?nodeId={nodeId}") {
        const val ARG_NODE_ID = "nodeId"
        fun buildRoute(nodeId: String? = null) =
            if (nodeId != null) "knowledge_graph?nodeId=$nodeId" else "knowledge_graph"
    }

    object TemplateManagement : NavigationDestination("template_management")
    
    object RecentlyDeleted : NavigationDestination("recently_deleted")
}
