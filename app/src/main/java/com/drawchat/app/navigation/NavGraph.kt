package com.drawchat.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.drawchat.app.presentation.MainScreen
import com.drawchat.app.presentation.auth.AuthViewModel
import com.drawchat.app.presentation.auth.LoginScreen
import com.drawchat.app.presentation.auth.RegisterScreen
import com.drawchat.app.presentation.chat.ChatRoomScreen
import com.drawchat.app.presentation.chat.ChatViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val CHAT_ROOM = "chat_room/{chatId}"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToChats = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToResetPassword = {}
            )
        }

        composable(Routes.REGISTER) {
            val authViewModel: AuthViewModel = viewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onChatClick = { chatId ->
                    navController.navigate("chat_room/$chatId")
                },
                onLogoutClick = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHAT_ROOM) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatViewModel: ChatViewModel = viewModel()
            ChatRoomScreen(
                viewModel = chatViewModel,
                chatId = chatId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}