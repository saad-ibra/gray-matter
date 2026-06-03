package com.example.graymatter.android.ui.navigation

/**
 * Navigation destinations for Gray Matter app.
 */
sealed class NavigationDestination(val route: String) {
    
    object Home : NavigationDestination("home")
    
    object Library : NavigationDestination("library")
    
    object Profile : NavigationDestination("profile")
    
    object Lookups : NavigationDestination("lookups")
    
    object NewEntry : NavigationDestination("new_entry")
    
    object ResourceDetail : NavigationDestination("resource_detail/{resourceEntryId}?focusOpinionId={focusOpinionId}&searchQuery={searchQuery}") {
        const val ARG_RESOURCE_ENTRY_ID = "resourceEntryId"
        const val ARG_FOCUS_OPINION_ID = "focusOpinionId"
        const val ARG_SEARCH_QUERY = "searchQuery"
        fun buildRoute(resourceEntryId: String, focusOpinionId: String? = null, searchQuery: String? = null) = 
            buildString {
                append("resource_detail/$resourceEntryId")
                val params = mutableListOf<String>()
                if (focusOpinionId != null) params.add("focusOpinionId=$focusOpinionId")
                if (searchQuery != null) params.add("searchQuery=$searchQuery")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
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

    object FileViewer : NavigationDestination("file_viewer/{resourceId}?page={page}&searchQuery={searchQuery}") {
        const val ARG_RESOURCE_ID = "resourceId"
        const val ARG_PAGE = "page"
        const val ARG_SEARCH_QUERY = "searchQuery"
        fun buildRoute(resourceId: String, page: Int? = null, searchQuery: String? = null) = 
            buildString {
                append("file_viewer/$resourceId")
                val params = mutableListOf<String>()
                if (page != null) params.add("page=$page")
                if (searchQuery != null) params.add("searchQuery=$searchQuery")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
    }

    object KnowledgeGraph : NavigationDestination("knowledge_graph?nodeId={nodeId}") {
        const val ARG_NODE_ID = "nodeId"
        fun buildRoute(nodeId: String? = null) =
            if (nodeId != null) "knowledge_graph?nodeId=$nodeId" else "knowledge_graph"
    }

    object TemplateManagement : NavigationDestination("template_management")
    
    object RecentlyDeleted : NavigationDestination("recently_deleted")

    object RecentResources : NavigationDestination("recent_resources")
    
    object ImageEditor : NavigationDestination("image_editor")

    object BackupSettings : NavigationDestination("backup_settings")

    object SecuritySettings : NavigationDestination("security_settings")

    object AppearanceSettings : NavigationDestination("appearance_settings")
}
