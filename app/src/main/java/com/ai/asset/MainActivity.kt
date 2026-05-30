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
    val hasValidApiKey by viewModel.hasValidApiKey.collectAsStateWithLifecycle()
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
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "App Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "AI Asset Pro",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0088CC)
                            )
                            if (!hasValidApiKey) {
                                Text(
                                    "API Key required",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showModelSelector = true }) {
                        Icon(
                            Icons.Default.ModelTraining,
                            contentDescription = "Select Model",
                            tint = Color(0xFF0088CC)
                        )
                    }
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = "API Key",
                            tint = Color(0xFF0088CC)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            if (viewModel.isDarkTheme.value) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
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
            // Chat Messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        WelcomeCard()
                    }
                }
                
                items(
                    items = chatMessages,
                    key = { it.id }
                ) { message ->
                    ChatBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                if (isAiLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }
            
            // Input Area
            InputArea(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && hasValidApiKey && !isAiLoading) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        scope.launch {
                            listState.animateScrollToItem(chatMessages.size)
                        }
                    } else if (!hasValidApiKey) {
                        Toast.makeText(context, "Please add API Key first", Toast.LENGTH_SHORT).show()
                        showApiKeyDialog = true
                    }
                },
                isEnabled = hasValidApiKey && !isAiLoading,
                showKeyWarning = !hasValidApiKey,
                onWarningClick = { showApiKeyDialog = true }
            )
        }
    }
    
    // Model Selector Dialog
    if (showModelSelector) {
        ModelSelectorDialog(
            currentModel = currentModel,
            models = availableModels,
            onSelect = { model ->
                viewModel.updateModel(model)
                showModelSelector = false
            },
            onDismiss = { showModelSelector = false }
        )
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = viewModel.geminiApiKey.value,
            onSave = { key ->
                viewModel.saveApiKey(context, key)
                showApiKeyDialog = false
                Toast.makeText(context, "API Key Saved Successfully", Toast.LENGTH_LONG).show()
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F0FE)
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0088CC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Welcome",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "AI Asset Pro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0088CC)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Powered by Google Gemini AI",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "• Multiple AI Models Available\n• Chat History Saved Locally\n• Markdown Support\n• Dark/Light Mode",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onCopy: () -> Unit) {
    val isUser = message.isUser
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) Color(0xFF0088CC) else Color(0xFFE8F0FE))
                .clickable { showMenu = !showMenu }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF0088CC)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gemini AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0088CC))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isUser) Color.White else Color(0xFF1A1A1A),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.formattedTime,
                    fontSize = 9.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copy Message", fontSize = 13.sp) },
                onClick = {
                    onCopy()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = Color(0xFFE8F0FE),
            modifier = Modifier.widthIn(min = 60.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0088CC))
                    )
                }
            }
        }
    }
}

@Composable
fun InputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean,
    showKeyWarning: Boolean,
    onWarningClick: () -> Unit
) {
    Column {
        if (showKeyWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onWarningClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = "Key",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Gemini API Key Required",
                            fontSize = 13.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Settings",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            tonalElevation = 3.dp,
            color = if (isEnabled) Color.White else Color(0xFFF5F5F5)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            if (isEnabled) "Type a message..." else "Enter API Key to start",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    enabled = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0088CC),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color(0xFFF5F5F5)
                    )
                )
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && isEnabled) Color(0xFF0088CC) 
                            else Color(0xFFE0E0E0)
                        )
                        .clickable(enabled = inputText.isNotBlank() && isEnabled) {
                            onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModelSelectorDialog(
    currentModel: String,
    models: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Select AI Model", 
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0088CC)
            ) 
        },
        text = {
            Column {
                models.forEach { model ->
                    val displayName = model.replace("models/", "")
                        .replace("gemini-", "")
                        .replace("-pro", " Pro")
                        .replace("-flash", " Flash")
                        .replace("-vision", " Vision")
                        .replace("1.5", "1.5")
                        .trim()
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(model) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentModel == model) 
                                Color(0xFF0088CC).copy(alpha = 0.1f) 
                            else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (currentModel == model) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    when {
                                        model.contains("pro") -> "Best for complex tasks"
                                        model.contains("flash") -> "Fast & efficient"
                                        else -> "Balanced performance"
                                    },
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            if (currentModel == model) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF0088CC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF0088CC))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Gemini API Key", 
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0088CC)
            ) 
        },
        text = {
            Column {
                Text(
                    "Get your free API key from:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "https://aistudio.google.com",
                    fontSize = 11.sp,
                    color = Color(0xFF0088CC)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("AIzaSy...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show", fontSize = 11.sp)
                        }
                    },
                    visualTransformation = if (showKey) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "🔒 Your key is stored securely on your device only",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (keyInput.isNotBlank()) {
                        onSave(keyInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save API Key", color = Color.White, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
