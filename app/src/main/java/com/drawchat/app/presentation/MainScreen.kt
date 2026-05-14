package com.drawchat.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drawchat.app.presentation.chat.ChatListScreen
import com.drawchat.app.presentation.chat.ChatViewModel
import com.drawchat.app.presentation.profile.ProfileScreen
import com.drawchat.app.presentation.profile.ProfileViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Chats : BottomNavItem("chats", "Чаты", Icons.AutoMirrored.Filled.Chat)
    object Gallery : BottomNavItem("gallery", "Работы", Icons.Filled.Dashboard)
    object Profile : BottomNavItem("profile", "Профиль", Icons.Filled.Person)
}

@Composable
fun MainScreen(
    onChatClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val navItems = listOf(
        BottomNavItem.Chats,
        BottomNavItem.Gallery,
        BottomNavItem.Profile
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val chatViewModel: ChatViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFB2EBB2),
                            selectedTextColor = Color(0xFFB2EBB2),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFFB2EBB2).copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> ChatListScreen(
                    viewModel = chatViewModel,
                    onChatClick = onChatClick,
                    onProfileClick = { selectedTab = 2 },
                    onLogoutClick = onLogoutClick
                )
                1 -> GalleryScreen()
                2 -> ProfileScreen(
                    viewModel = profileViewModel,
                    onBackClick = { selectedTab = 0 },
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}

@Composable
fun GalleryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎨\nЗдесь будут ваши работы",
            color = Color.Gray
        )
    }
}