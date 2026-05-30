package com.ai.asset

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource // ✨ ADDED: strings.xml နဲ့ ချိတ်ဆက်ဖို့
import com.ai.asset.R // ✨ ADDED: Resource ID တွေ သိစေဖို့
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
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

val PrimaryGradient = Brush.horizontalGradient(listOf(Color(0xFF7F56D9), Color(0xFF6366F1)))
val AiBubbleBgLight = Color(0xFFF3F4F6)
val AiBubbleBgDark = Color(0xFF1F2937)
val ScreenBgDark = Color(0xFF0B0F19)
val CardBgDark = Color(0xFF1E293B)

@Composable
fun AIAssetProApp(viewModel: AssetViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme(primary = Color(0xFF7F56D9)) else lightColorScheme(primary = Color(0xFF6366F1)),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDarkTheme) ScreenBgDark else Color(0xFFF9FAFB)
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }
    
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val hasValidApiKey by viewModel.hasValidApiKey.collectAsStateWithLifecycle()
    val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
    val availableModels = viewModel.availableModels
    val isDark = viewModel.isDarkTheme.collectAsStateWithLifecycle().value
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            // ✨ FIX: App Name ကို strings.xml နဲ့ ချိတ်ဆက်ထားပါတယ်
                            Text(stringResource(R.string.app_name), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF111827))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (hasValidApiKey) Color(0xFF10B981) else Color(0xFFF59E0B)))
                                Spacer(modifier = Modifier.width(5.dp))
                                // ✨ FIX: Status စာသားကို strings.xml နဲ့ ချိတ်ဆက်ထားပါတယ်
                                Text(
                                    if (hasValidApiKey) stringResource(R.string.ai_ready) else stringResource(R.string.api_key_required), 
                                    fontSize = 11.sp, 
                                    color = if (hasValidApiKey) Color(0xFF10B981) else Color(0xFFF59E0B), 
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showModelSelector = true }) { Icon(Icons.Default.ModelTraining, contentDescription = "Model", tint = if(isDark) Color.LightGray else Color(0xFF4B5563)) }
                    IconButton(onClick = { showApiKeyDialog = true }) { Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = if(isDark) Color.LightGray else Color(0xFF4B5563)) }
                    IconButton(onClick = { viewModel.toggleTheme() }) { Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = if(isDark) Color.LightGray else Color(0xFF4B5563)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) Color(0xFF111827) else Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item { WelcomeScreen(isDark = isDark, onSuggestionClick = { inputText = it }) }
                }
                
                items(chatMessages, key = { it.id }) { message ->
                    ChatBubble(message = message, isDark = isDark, onCopy = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        val copiedMessage = context.getString(R.string.copied_to_clipboard)
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    })
                }
                
                if (isAiLoading) {
                    item { TypingIndicator(isDark) }
                }
            }
            
            // API Banner Warning
            AnimatedVisibility(visible = !hasValidApiKey, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { showApiKeyDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        // ✨ FIX: Banner စာသားကို strings.xml နဲ့ ချိတ်ဆက်ထားပါတယ်
                        Text(stringResource(R.string.tap_to_enter_key), fontSize = 12.sp, color = Color(0xFF92400E), modifier = Modifier.weight(1f))
                    }
                }
            }
            
            // Image Preview Section
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .size(70.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.LightGray)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable { selectedImageUri = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
            
            // Modern Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = if (isDark) Color(0xFF111827) else Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.background(if(isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image", tint = Color(0xFF6366F1))
                    }
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        // ✨ FIX: Hint စာသားများကို strings.xml နဲ့ ချိတ်ဆက်ထားပါတယ်
                        placeholder = { 
                            Text(
                                if(selectedImageUri != null) stringResource(R.string.ask_about_image) else stringResource(R.string.ask_anything), 
                                fontSize = 14.sp, 
                                color = Color.Gray
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = if(isDark) Color(0xFF374151) else Color(0xFFE5E7EB),
                            focusedContainerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF9FAFB),
                            unfocusedContainerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF9FAFB)
                        )
                    )
                    
                    // Send Button
                    val isButtonEnabled = inputText.isNotBlank() || selectedImageUri != null
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isButtonEnabled) PrimaryGradient else Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB))))
                            .clickable(enabled = isButtonEnabled && !isAiLoading) {
                                viewModel.sendMessage(context, inputText, selectedImageUri)
                                inputText = ""
                                selectedImageUri = null
                                scope.launch { listState.animateScrollToItem(chatMessages.size) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    
    // Model Picker Dialog
    if (showModelSelector) {
        AlertDialog(
            onDismissRequest = { showModelSelector = false },
            title = { Text(stringResource(R.string.select_ai_engine), fontWeight = FontWeight.Bold, color = Color(0xFF6366F1)) },
            text = {
                Column {
                    availableModels.forEach { model ->
                        val displayName = model.replace("gemini-", "Gemini ").replace("-pro", " Pro").replace("-flash", " Flash")
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                viewModel.updateModel(model)
                                showModelSelector = false
                            },
                            colors = CardDefaults.cardColors(containerColor = if (currentModel == model) Color(0xFF6366F1).copy(alpha = 0.15f) else Color.Transparent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(displayName, fontSize = 14.sp, fontWeight = if (currentModel == model) FontWeight.Bold else FontWeight.Normal)
                                if (currentModel == model) { Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModelSelector = false }) { Text(stringResource(R.string.cancel)) } },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(viewModel.geminiApiKey.value) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(stringResource(R.string.setup_gemini_key), fontWeight = FontWeight.Bold, color = Color(0xFF6366F1)) },
            text = {
                Column {
                    Text("Get a free key from Google AI Studio:", fontSize = 13.sp, color = Color.Gray)
                    Text("https://aistudio.google.com", fontSize = 12.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = keyInput, onValueChange = { keyInput = it },
                        placeholder = { Text("Paste AIzaSy... key here", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyInput.isNotBlank()) {
                            viewModel.saveApiKey(context, keyInput)
                            showApiKeyDialog = false
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.save_configuration)) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun WelcomeScreen(isDark: Boolean, onSuggestionClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(PrimaryGradient), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        // ✨ FIX: App Name နှင့် Welcome Subtitle ကို strings.xml ချိတ်ဆက်ထားပါတယ်
        Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if(isDark) Color.White else Color(0xFF111827))
        Text(stringResource(R.string.next_gen_assistant), fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(24.dp))
        
        // ✨ FIX: Suggestion Title ကို strings.xml ချိတ်ဆက်ထားပါတယ်
        Text(stringResource(R.string.try_asking), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
        val suggestions = listOf(
            "🖼️ ဓါတ်ပုံတစ်ပုံတင်ပြီး 'ဒီပုံကို ရှင်းပြပေးပါ' ဟု မေးမြန်းပါ",
            "💡 အလန်းစား Business Idea ၅ ခုလောက်ပြောပြပါ",
            "💻 Kotlin အခြေခံကို ရှင်းပြပေးပါ"
        )
        suggestions.forEach { prompt ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSuggestionClick(prompt.substring(3)) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) CardBgDark else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(text = prompt, fontSize = 13.sp, color = if(isDark) Color(0xFFE5E7EB) else Color(0xFF374151), modifier = Modifier.padding(14.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isDark: Boolean, onCopy: () -> Unit) {
    val isUser = message.isUser
    var showMenu by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isUser) 18.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 18.dp))
                .background(if (isUser) PrimaryGradient else Brush.linearGradient(listOf(if (isDark) AiBubbleBgDark else AiBubbleBgLight, if (isDark) AiBubbleBgDark else AiBubbleBgLight)))
                .clickable { showMenu = !showMenu }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(12.dp), tint = Color(0xFF6366F1))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                    }
                }
                Text(text = message.text, fontSize = 14.sp, color = if (isUser) Color.White else (if(isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)), lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message.formattedTime, fontSize = 9.sp, color = if (isUser) Color.White.copy(alpha = 0.65f) else Color.Gray, modifier = Modifier.align(Alignment.End))
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            // ✨ FIX: Dropdown Menu Text ကို strings.xml ချိတ်ဆက်ထားပါတယ်
            DropdownMenuItem(text = { Text(stringResource(R.string.copy_text)) }, onClick = { onCopy(); showMenu = false })
        }
    }
}

@Composable
fun TypingIndicator(isDark: Boolean) {
    Row(modifier = Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.Start) {
        Surface(shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp), color = if (isDark) AiBubbleBgDark else AiBubbleBgLight, modifier = Modifier.widthIn(min = 65.dp)) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF6366F1))) }
            }
        }
    }
}
