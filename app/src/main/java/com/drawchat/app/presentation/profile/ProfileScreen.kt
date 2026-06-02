package com.drawchat.app.presentation.profile

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    var isEditingName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(it, context) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Профиль",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Аватар
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB2EBB2).copy(alpha = 0.2f))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotEmpty()) {
                    val avatarBitmap = remember(avatarUrl) {
                        try {
                            val bytes = Base64.decode(avatarUrl, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) { null }
                    }
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "Аватар",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFFB2EBB2)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFFB2EBB2)
                    )
                }
            }

            Text(
                "Нажмите чтобы изменить фото",
                color = Color(0xFFB2EBB2),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Имя
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        newName = userName
                        isEditingName = true
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFFB2EBB2))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Имя", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            userName.ifEmpty { "Не задано" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    Icon(Icons.Default.Edit, "Изменить", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Email
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, null, tint = Color(0xFFB2EBB2))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Email", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            userEmail.ifEmpty { "Не задано" },
                            fontSize = 16.sp,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Выход
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Выйти из аккаунта", color = Color.White)
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFB2EBB2),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

    // Диалог редактирования имени
    if (isEditingName) {
        AlertDialog(
            onDismissRequest = { isEditingName = false },
            title = { Text("Изменить имя") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Новое имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateName(newName)
                        isEditingName = false
                    }
                }) { Text("Сохранить", color = Color(0xFFB2EBB2)) }
            },
            dismissButton = {
                TextButton(onClick = { isEditingName = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выход") },
            text = { Text("Вы уверены, что хотите выйти?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    onLogoutClick()
                }) { Text("Выйти", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Отмена") }
            }
        )
    }
}