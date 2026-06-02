package com.drawchat.app.presentation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drawchat.app.presentation.chat.ChatListScreen
import com.drawchat.app.presentation.chat.ChatViewModel
import com.drawchat.app.presentation.profile.ProfileScreen
import com.drawchat.app.presentation.profile.ProfileViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Artwork(
    val id: String,
    val title: String,
    val imageBase64: String,
    val authorName: String,
    val createdAt: Long
)

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
    val navItems = listOf(BottomNavItem.Chats, BottomNavItem.Gallery, BottomNavItem.Profile)
    var selectedTab by remember { mutableIntStateOf(0) }
    val chatViewModel: ChatViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
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
                0 -> ChatListScreen(viewModel = chatViewModel, onChatClick = onChatClick)
                1 -> GalleryScreen()
                2 -> ProfileScreen(viewModel = profileViewModel, onBackClick = { selectedTab = 0 }, onLogoutClick = onLogoutClick)
            }
        }
    }
}

@Composable
fun GalleryScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    var artworks by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedArtwork by remember { mutableStateOf<Artwork?>(null) }
    val scope = rememberCoroutineScope()

    fun loadArtworks() {
        scope.launch {
            try {
                val snapshot = db.collection("artworks")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                artworks = snapshot.documents.mapNotNull { doc ->
                    val base64 = doc.getString("imageBase64") ?: return@mapNotNull null
                    Artwork(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        imageBase64 = base64,
                        authorName = doc.getString("authorName") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }
            } catch (e: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadArtworks() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Мои работы", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.padding(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFB2EBB2)) }
            } else if (artworks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎨", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Здесь будут ваши работы", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(artworks, key = { it.id }) { artwork ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column {
                                val bitmap = remember(artwork.imageBase64) {
                                    try { val bytes = Base64.decode(artwork.imageBase64, Base64.DEFAULT); BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } catch (e: Exception) { null }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                    if (bitmap != null) {
                                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Рисунок", modifier = Modifier.fillMaxSize().clickable { selectedArtwork = artwork }, contentScale = ContentScale.Crop)
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) { Text("🎨") }
                                    }
                                }
                                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(artwork.authorName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { scope.launch { try { db.collection("artworks").document(artwork.id).delete().await(); loadArtworks() } catch (e: Exception) { } } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Delete, "Удалить", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedArtwork != null) {
        val artwork = selectedArtwork!!
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { selectedArtwork = null }) {
            val bitmap = remember(artwork.imageBase64) {
                try { val bytes = Base64.decode(artwork.imageBase64, Base64.DEFAULT); BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } catch (e: Exception) { null }
            }
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            IconButton(onClick = { selectedArtwork = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Filled.Close, "Закрыть", tint = Color.White, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = { scope.launch { try { db.collection("artworks").document(artwork.id).delete().await(); selectedArtwork = null; artworks = artworks.filter { it.id != artwork.id } } catch (e: Exception) { } } }, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Icon(Icons.Filled.Delete, "Удалить", tint = Color.White, modifier = Modifier.size(32.dp)) }
            Text("Автор: ${artwork.authorName}", color = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(16.dp), fontSize = 16.sp)
        }
    }
}