package com.drawchat.app.presentation.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawchat.app.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

val colorPalette = listOf(
    Color.Black, Color.White, Color.Red, Color(0xFFFF5722),
    Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF4CAF50),
    Color(0xFF2196F3), Color(0xFF3F51B5), Color(0xFF9C27B0),
    Color(0xFF795548), Color(0xFF607D8B), Color(0xFFB2EBB2),
    Color(0xFFFF4081), Color(0xFF00BCD4), Color(0xFF8BC34A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: ChatViewModel,
    chatId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var showComments by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokePicker by remember { mutableStateOf(false) }
    var chatInviteCode by remember { mutableStateOf("") }
    var isEraser by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
        try {
            val doc = FirebaseFirestore.getInstance().collection("chats").document(chatId).get().await()
            chatInviteCode = doc.getString("inviteCode") ?: ""
        } catch (_: Exception) { }
    }

    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Рисунок сохранен в Мои работы!", Toast.LENGTH_SHORT).show()
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Чат художников", fontSize = 18.sp)
                        if (chatInviteCode.isNotEmpty()) {
                            Text("Код: $chatInviteCode", fontSize = 12.sp, color = Color(0xFFB2EBB2))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveImmediately()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (chatInviteCode.isNotEmpty()) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("code", chatInviteCode))
                            Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, "Копировать код", tint = Color(0xFFB2EBB2))
                        }
                    }
                    IconButton(onClick = { isEraser = !isEraser }) {
                        Icon(
                            if (isEraser) Icons.Default.AutoFixHigh else Icons.Default.AutoFixNormal,
                            "Ластик",
                            tint = if (isEraser) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewModel.clearCanvas() }) {
                        Icon(Icons.Default.Delete, "Очистить")
                    }
                    IconButton(onClick = {
                        viewModel.saveDrawing(context)
                    }) {
                        Icon(Icons.Default.Save, "Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB2EBB2).copy(alpha = 0.3f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CanvasView(
                selectedColor = selectedColor,
                strokeWidth = strokeWidth,
                isEraser = isEraser,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
            )

            DrawingToolsPanel(
                selectedColor = selectedColor,
                onColorChange = { selectedColor = it },
                strokeWidth = strokeWidth,
                onStrokeWidthChange = { strokeWidth = it },
                showColorPicker = showColorPicker,
                onToggleColorPicker = { showColorPicker = !showColorPicker },
                showStrokePicker = showStrokePicker,
                onToggleStrokePicker = { showStrokePicker = !showStrokePicker }
            )

            Divider()

            Column(
                modifier = Modifier
                    .weight(if (showComments) 0.35f else 0.05f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showComments = !showComments }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (showComments) Icons.Default.KeyboardArrowDown
                        else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Комментарии (${messages.size})", color = Color.Gray, fontSize = 14.sp)
                }

                if (showComments) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        reverseLayout = true,
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(message = message)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Добавить комментарий...") },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = Color(0xFFB2EBB2))
                }
            }
        }
    }
}

@Composable
fun DrawingToolsPanel(
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    showColorPicker: Boolean,
    onToggleColorPicker: () -> Unit,
    showStrokePicker: Boolean,
    onToggleStrokePicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, Color.Gray, CircleShape)
                    .clickable { onToggleColorPicker() }
            )

            for (i in 0..7) {
                val color = colorPalette[i]
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (color == selectedColor) 2.dp else 0.dp,
                            color = Color.DarkGray,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color.Gray, CircleShape)
                    .clickable { onToggleStrokePicker() },
                contentAlignment = Alignment.Center
            ) {
                val dotSize = when {
                    strokeWidth > 20f -> 20.dp
                    strokeWidth < 2f -> 2.dp
                    else -> strokeWidth.dp
                }
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
            }
        }

        if (showColorPicker) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colorPalette.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (color == selectedColor) 3.dp else 0.dp,
                                color = Color.DarkGray,
                                shape = CircleShape
                            )
                            .clickable {
                                onColorChange(color)
                                onToggleColorPicker()
                            }
                    )
                }
            }
        }

        if (showStrokePicker) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1", fontSize = 12.sp, color = Color.Gray)
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 1f..20f,
                    modifier = Modifier.weight(1f)
                )
                Text("20", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = message.senderName + ": ",
                fontSize = 12.sp,
                color = Color(0xFFB2EBB2)
            )
            Text(text = message.text, fontSize = 12.sp)
        }
    }
}