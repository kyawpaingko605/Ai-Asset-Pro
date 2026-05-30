package com.ai.asset

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai.asset.model.ChatMessage
import com.ai.asset.viewmodel.AssetViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AssetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(this)
        setContent {
            AIAssetProApp(viewModel = viewModel)
        }
    }
}

@Composable
fun AIAssetProApp(viewModel: AssetViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme(),
        typography = Typography()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
        ) {
            MainChatScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(viewModel: AssetViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }
    
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
    val availableModels = viewModel.availableModels
    
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0088CC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Asset Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0088CC))
                            Text(
                                if (geminiApiKey.isNotBlank()) "● AI Ready" else "⚠️ Set API Key First",
                                fontSize = 11.sp,
                                color = if (geminiApiKey.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showModelSelector = true }) {
                        Icon(Icons.Default.ModelTraining, contentDescription = "Select Model", tint = Color(0xFF0088CC))
                    }
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.VpnKey, contentDescription = "API Key Settings", tint = Color(0xFF0088CC))
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            if (viewModel.isDarkTheme.value) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = Color(0xFF0088CC)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (viewModel.isDarkTheme.value) Color(0xFF1E1E1E) else Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat Area
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item { WelcomeCard() }
                }
                
                items(chatMessages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                if (isAiLoading) {
                    item { TypingIndicator() }
                }
            }
            
            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = if (viewModel.isDarkTheme.value) Color(0xFF1E1E1E) else Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type your message here...", fontSize = 14.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0088CC),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = if (viewModel.isDarkTheme.value) Color(0xFF2D2D2D) else Color.White,
                            unfocusedContainerColor = if (viewModel.isDarkTheme.value) Color(0xFF2D2D2D) else Color.White
                        )
                    )
                    
                    Button(
                        onClick = {
                            if (inputText.isNotBlank() && !isAiLoading) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                scope.launch { listState.animateScrollToItem(chatMessages.size) }
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inputText.isNotBlank()) Color(0xFF0088CC) else Color(0xFFE0E0E0)
                        ),
                        shape = CircleShape,
                        enabled = inputText.isNotBlank() && !isAiLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    
    // Model Selector Dialog
    if (showModelSelector) {
        AlertDialog(
            onDismissRequest = { showModelSelector = false },
            title = { Text("Select AI Model", fontWeight = FontWeight.Bold, color = Color(0xFF0088CC)) },
            text = {
                Column {
                    availableModels.forEach { model ->
                        val displayName = model.replace("models/", "").replace("gemini-", "Gemini ").replace("-pro", " Pro").replace("-flash", " Flash")
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                viewModel.updateModel(model)
                                showModelSelector = false
                            },
                            colors = CardDefaults.cardColors(containerColor = if (currentModel == model) Color(0xFF0088CC).copy(alpha = 0.1f) else Color.Transparent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(displayName, fontSize = 14.sp, fontWeight = if (currentModel == model) FontWeight.Bold else FontWeight.Normal)
                                if (currentModel == model) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF0088CC), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModelSelector = false }) { Text("Close", color = Color(0xFF0088CC)) } },
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        val currentSavedKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
        var keyInput by remember { mutableStateOf(currentSavedKey) }
        var showKey by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key Settings", fontWeight = FontWeight.Bold, color = Color(0xFF0088CC)) },
            text = {
                Column {
                    Text("Get free key from aistudio.google.com", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("Enter AIzaSy Key...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show", fontSize = 11.sp) }
                        },
                        visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveApiKey(context, keyInput.trim())
                        showApiKeyDialog = false
                        Toast.makeText(context, "Key Configuration Updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Settings", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel", color = Color.Gray) } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF0088CC)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Asset Pro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0088CC))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ready to chat with Gemini", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onCopy: () -> Unit) {
    val isUser = message.isUser
    var showMenu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isUser) 18.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 18.dp))
                .background(if (isUser) Color(0xFF0088CC) else Color(0xFFE8F0FE))
                .clickable { showMenu = !showMenu }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.text, fontSize = 14.sp, color = if (isUser) Color.White else Color(0xFF1A1A1A), lineHeight = 20.sp)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Copy") }, onClick = { onCopy(); showMenu = false })
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(modifier = Modifier.padding(start = 12.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFE8F0FE)) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0088CC))) }
            }
        }
    }
}
