package com.ai.asset // package စာလုံးအသေးသေချာပါတယ်

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
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

// 🎨 Premium Design Constants
val PrimaryGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
val ScreenBgLight = Color(0xFFF8FAFC)
val CardBgLight = Color(0xFFFFFFFF)

@Composable
fun AIAssetProApp(viewModel: AssetViewModel) {
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = if (isDark) Color(0xFF0F172A) else ScreenBgLight) {
            MainChatScreen(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(viewModel: AssetViewModel) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    // UI States
    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    
    // ViewModel States
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val hasValidKey by viewModel.hasValidApiKey.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    // Key မရှိရင် အလိုလို Dialog တက်လာအောင် လုပ်ပေးထားပါတယ် (အရောင်း Project အတွက် အရေးကြီးပါတယ်)
    LaunchedEffect(Unit) {
        if (!hasValidKey) showApiKeyDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Asset Pro", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) { Icon(Icons.Default.VpnKey, contentDescription = "Key") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg -> ChatBubble(msg, isDark) }
                if (isAiLoading) item { TypingIndicator(isDark) }
            }

            // Input Area
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("AI ကို မေးမြန်းပါ...") },
                trailingIcon = {
                    IconButton(onClick = { 
                        if(inputText.isNotBlank()) {
                            viewModel.sendMessage(context, inputText)
                            inputText = ""
                        }
                    }) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF6366F1)) }
                },
                shape = RoundedCornerShape(30.dp)
            )
        }
    }
    
    // API Key Dialog - တစ်နေရာတည်းမှာပဲ စီမံထားပါတယ်
    if (showApiKeyDialog) {
        ApiKeySetupDialog(viewModel, onDismiss = { showApiKeyDialog = false })
    }
}

@Composable
fun ApiKeySetupDialog(viewModel: AssetViewModel, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Key ထည့်သွင်းပါ") },
        text = {
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Gemini Key") })
        },
        confirmButton = {
            Button(onClick = {
                viewModel.saveApiKey(context, key)
                onDismiss()
            }) { Text("Save") }
        }
    )
}

@Composable
fun ChatBubble(message: ChatMessage, isDark: Boolean) {
    val isUser = message.isUser
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isUser) Color(0xFF6366F1) else if (isDark) Color(0xFF334155) else Color.White,
            shadowElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp),
                color = if (isUser) Color.White else if (isDark) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun TypingIndicator(isDark: Boolean) {
    Text("AI စဉ်းစားနေသည်...", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
}
