package com.drawchat.app.presentation.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawchat.app.data.model.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onChatClick: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var newChatName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadChats() }

    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Чаты", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.padding(16.dp))
            Text("Рисуйте вместе с другими!", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFB2EBB2))
                }
            } else if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Нет чатов", color = Color.Gray, fontSize = 16.sp)
                        Text("Создайте новый или введите код", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chats, key = { it.id }) { chat ->
                        ChatItem(chat = chat, onClick = { onChatClick(chat.id) }, onDelete = {
                            scope.launch {
                                try {
                                    db.collection("chats").document(chat.id).collection("messages").get().await()
                                        .documents.forEach { it.reference.delete().await() }
                                    db.collection("chats").document(chat.id).delete().await()
                                    viewModel.loadChats()
                                } catch (e: Exception) { }
                            }
                        })
                    }
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FloatingActionButton(onClick = { showJoinDialog = true }, containerColor = Color(0xFF2196F3)) {
                Icon(Icons.Default.VpnKey, "Войти по коду", tint = Color.White)
            }
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = Color(0xFFB2EBB2)) {
                Icon(Icons.Default.Add, "Создать чат", tint = Color.White)
            }
        }
    }

    // Диалог создания чата
    if (showCreateDialog) {
        var generatedCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; generatedCode = "" },
            title = { Text("Создать чат") },
            text = {
                Column {
                    Text("Создайте чат и получите код!", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = newChatName, onValueChange = { newChatName = it }, label = { Text("Название чата") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (generatedCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Код чата:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(generatedCode, fontSize = 28.sp, color = Color(0xFFB2EBB2), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newChatName.isNotBlank()) {
                        scope.launch {
                            val code = (100000..999999).random().toString()
                            generatedCode = code
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
                            Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            db.collection("chats").add(hashMapOf(
                                "name" to newChatName, "inviteCode" to code,
                                "participantsCount" to 1, "participants" to listOf(userId),
                                "canvasImage" to "", "createdAt" to System.currentTimeMillis()
                            )).await()
                            viewModel.loadChats()
                            showCreateDialog = false; newChatName = ""; generatedCode = ""
                        }
                    }
                }) { Text("Создать", color = Color(0xFFB2EBB2)) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") } }
        )
    }

    // Диалог входа по коду
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false; inviteCode = "" },
            title = { Text("Войти в чат по коду") },
            text = {
                Column {
                    Text("Введите код от друга", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it }, label = { Text("Код чата") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inviteCode.isNotBlank()) {
                        scope.launch {
                            try {
                                val snapshot = db.collection("chats").whereEqualTo("inviteCode", inviteCode).get().await()
                                if (snapshot.documents.isNotEmpty()) {
                                    val doc = snapshot.documents[0]
                                    val chatId = doc.id
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    val participants = (doc.get("participants") as? List<*>)?.toMutableList() ?: mutableListOf()
                                    if (!participants.contains(userId)) {
                                        participants.add(userId)
                                        db.collection("chats").document(chatId).update("participants", participants, "participantsCount", participants.size).await()
                                    }
                                    showJoinDialog = false; inviteCode = ""
                                    onChatClick(chatId)
                                } else {
                                    Toast.makeText(context, "Чат не найден", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }) { Text("Войти", color = Color(0xFFB2EBB2)) }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFB2EBB2).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Chat, null, tint = Color(0xFFB2EBB2), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333))
                Text("${chat.participantsCount} участников", color = Color.Gray, fontSize = 13.sp)
            }
            IconButton(onClick = {
                showCodeDialog = true
                MainScope().launch {
                    try { inviteCode = db.collection("chats").document(chat.id).get().await().getString("inviteCode") ?: "—" }
                    catch (_: Exception) { inviteCode = "—" }
                }
            }) { Icon(Icons.Default.VpnKey, "Код", tint = Color(0xFFB2EBB2)) }
            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Удалить", tint = Color.Red.copy(alpha = 0.6f)) }
        }
    }

    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text("Код чата") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Поделитесь кодом:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (inviteCode.isEmpty()) CircularProgressIndicator(color = Color(0xFFB2EBB2))
                    else Text(inviteCode, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB2EBB2))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("code", inviteCode))
                    Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                    showCodeDialog = false
                }) { Text("Копировать", color = Color(0xFFB2EBB2)) }
            },
            dismissButton = { TextButton(onClick = { showCodeDialog = false }) { Text("Закрыть") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чат?") },
            text = { Text("Чат \"${chat.name}\" будет удален") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Удалить", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } }
        )
    }
}