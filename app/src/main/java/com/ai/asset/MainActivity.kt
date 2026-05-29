package com.ai.asset

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
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
        setContent {
            AIAssetProApp(viewModel = viewModel)
        }
    }
}

@Composable
fun AIAssetProApp(viewModel: AssetViewModel) {
    val isDarkTheme = viewModel.isDarkTheme.collectAsStateWithLifecycle()
    
    MaterialTheme(
        colorScheme = if (isDarkTheme.value) darkColorScheme() else lightColorScheme(),
        typography = Typography()
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ProChatScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProChatScreen(viewModel: AssetViewModel) {
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
    
    // Auto scroll to bottom
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
                        // Animated gradient logo
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0088CC), Color(0xFF00A8FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!hasValidApiKey) {
                                Text(
                                    "API Key required",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Model Selector Button
                    IconButton(onClick = { showModelSelector = true }) {
                        Badge(
                            containerColor = Color(0xFF0088CC),
                            contentColor = Color.White
                        ) {
                            Text("Pro", fontSize = 8.sp)
                        }
                        Icon(Icons.Default.ModelTraining, contentDescription = "Model")
                    }
                    
                    // Settings Button
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    
                    // Theme Toggle
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            if (viewModel.isDarkTheme.value) Icons.Default.LightMode 
                            else Icons.Default.DarkMode,
                            contentDescription = "Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        ProWelcomeCard()
                    }
                }
                
                items(
                    items = chatMessages,
                    key = { it.id }
                ) { message ->
                    ProChatBubble(
                        message = message,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            // Share intent
                        }
                    )
                }
                
                if (isAiLoading) {
                    item {
                        ProTypingIndicator()
                    }
                }
            }
            
            // API Key Warning
            if (!hasValidApiKey) {
                ProApiKeyWarning(onClick = { showApiKeyDialog = true })
            }
            
            // Input Area
            ProInputArea(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && hasValidApiKey && !isAiLoading) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        scope.launch {
                            listState.animateScrollToItem(chatMessages.size)
                        }
                    }
                },
                isEnabled = hasValidApiKey && !isAiLoading
            )
        }
    }
    
    // Model Selector Dialog
    if (showModelSelector) {
        ProModelSelectorDialog(
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
        ProApiKeyDialog(
            currentKey = viewModel.geminiApiKey.value,
            onSave = { key ->
                viewModel.saveApiKey(context, key)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
fun ProWelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0088CC), Color(0xFF00A8FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "AI Asset Pro",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Powered by Google Gemini AI",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "• Multiple AI Models Available\n• Chat History Saved Locally\n• Markdown Support\n• Dark/Light Mode",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ProChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
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
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) Color(0xFF0088CC) 
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { showMenu = !showMenu }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF0088CC)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Gemini AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0088CC)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.formattedTime,
                    fontSize = 9.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.6f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        
        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copy", fontSize = 13.sp) },
                onClick = {
                    onCopy()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Share", fontSize = 13.sp) },
                onClick = {
                    onShare()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Share, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
fun ProTypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(min = 60.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val delay = index * 120L
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(300, delayMillis = delay),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp * scale)
                            .clip(CircleShape)
                            .background(Color(0xFF0088CC))
                    )
                }
            }
        }
    }
}

@Composable
fun ProInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
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
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        if (isEnabled) "Message AI Assistant..." else "Enter API Key first",
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                enabled = isEnabled,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0088CC),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )
            
            AnimatedVisibility(
                visible = inputText.isNotBlank(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier.size(44.dp),
                    containerColor = Color(0xFF0088CC),
                    shape = CircleShape
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
fun ProApiKeyWarning(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFFFF9800).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini API Key required", fontSize = 13.sp, color = Color(0xFFFF9800))
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Add Key", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ProModelSelectorDialog(
    currentModel: String,
    models: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ModelTraining, tint = Color(0xFF0088CC))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select AI Model", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("Choose which Gemini model to use", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                models.forEach { model ->
                    val displayName = model.replace("models/", "")
                        .replace("gemini-", "Gemini ")
                        .replace("-pro", " Pro")
                        .replace("-flash", " Flash")
                        .replace("-vision", " Vision")
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(model) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentModel == model) 
                                Color(0xFF0088CC).copy(alpha = 0.1f) 
                            else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
                                Icon(Icons.Default.CheckCircle, tint = Color(0xFF0088CC), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ProApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, tint = Color(0xFF0088CC))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini API Key", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Get your free API key from Google AI Studio",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "https://aistudio.google.com",
                    fontSize = 11.sp,
                    color = Color(0xFF0088CC)
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🔒 Your key is stored securely on your device",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save API Key", color = Color.White)
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
