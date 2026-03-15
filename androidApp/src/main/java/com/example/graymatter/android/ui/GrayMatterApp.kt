package com.example.graymatter.android.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.graymatter.android.ui.navigation.GrayMatterNavigation
import com.example.graymatter.android.ui.navigation. NavigationDestination
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.di.AppModule

@Composable
fun GrayMatterApp(
    appModule: AppModule,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Top-level destinations that show the bottom bar
    val topLevelDestinations = listOf(
        NavigationDestination.Home.route,
        NavigationDestination.Library.route
    )
    val showBottomBar = currentRoute in topLevelDestinations

    Scaffold(
        containerColor = GrayMatterColors.BackgroundDark,
        bottomBar = {
            if (showBottomBar) {
                GrayMatterBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToHome = {
                        navController.navigate(NavigationDestination.Home.route) {
                            popUpTo(NavigationDestination.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToLibrary = {
                        navController.navigate(NavigationDestination.Library.route) {
                            popUpTo(NavigationDestination.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        GrayMatterNavigation(
            appModule = appModule,
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun GrayMatterBottomBar(
    currentRoute: String?,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GrayMatterColors.BackgroundDark.copy(alpha = 0.98f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(GrayMatterColors.Neutral800)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                selected = currentRoute == NavigationDestination.Home.route,
                activeIcon = Icons.Filled.Home,
                inactiveIcon = Icons.Outlined.Home,
                onClick = onNavigateToHome,
                modifier = Modifier.weight(1f)
            )
            
            NavBarItem(
                selected = currentRoute == NavigationDestination.Library.route,
                activeIcon = Icons.Filled.List,
                inactiveIcon = Icons.Outlined.List,
                onClick = onNavigateToLibrary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    selected: Boolean,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) GrayMatterColors.Primary else GrayMatterColors.Neutral500,
        label = "color"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        label = "scale"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 14.dp else 0.dp,
        label = "width"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(26.dp)
                    .scale(scale),
                tint = iconColor
            )
            
            if (selected) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(GrayMatterColors.Primary)
                )
            }
        }
    }
}
