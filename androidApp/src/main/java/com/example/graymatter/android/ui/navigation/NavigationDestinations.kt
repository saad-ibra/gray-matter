package com.example.graymatter.android.ui.navigation

/**
 * Navigation destinations for Gray Matter app.
 */
sealed class NavigationDestination(val route: String) {
    
    object Home : NavigationDestination("home")
    
    object Library : NavigationDestination("library")
    
    object Profile : NavigationDestination("profile")
    
    object NewEntry : NavigationDestination("new_entry")
    
    object ItemDetail : NavigationDestination("item_detail/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        fun buildRoute(itemId: String) = "item_detail/$itemId"
    }
    
    object AddToTopic : NavigationDestination("add_to_topic/{itemId}") {
        const val ARG_ITEM_ID = "itemId"
        fun buildRoute(itemId: String) = "add_to_topic/$itemId"
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
}
