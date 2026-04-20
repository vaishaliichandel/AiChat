package com.vaishali.aichat.ui.theme

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaishali.aichat.data.local.ChatEntity
import com.vaishali.aichat.ui.theme.chatViewModel.ChatMessage
import com.vaishali.aichat.ui.theme.chatViewModel.ChatUiState
import com.vaishali.aichat.ui.theme.chatViewModel.ChatViewModel
import com.vaishali.aichat.util.VoiceToTextParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onBack = { showSettings = false }
        )
    } else {
        ChatScreenInternal(
            uiState = uiState,
            onSendMessage = viewModel::sendMessage,
            onEditMessage = viewModel::editMessage,
            onChatSelected = viewModel::selectChat,
            onNewChat = viewModel::startNewChat,
            onDeleteChat = viewModel::deleteChat,
            onRenameChat = viewModel::renameChat,
            onStopGenerating = viewModel::stopGenerating,
            onSettingsClick = { showSettings = true }
        )
    }
}

@Composable
private fun ChatScreenInternal(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onChatSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (String) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onStopGenerating: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                allChats = uiState.allChats,
                currentChatId = uiState.currentChatId,
                onChatSelected = {
                    onChatSelected(it)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    onNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteChat = onDeleteChat,
                onRenameChat = onRenameChat,
                onSettingsClick = {
                    onSettingsClick()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        ChatScreenContent(
            uiState = uiState,
            onSendMessage = onSendMessage,
            onEditMessage = onEditMessage,
            onStopGenerating = onStopGenerating,
            onMenuClick = { scope.launch { drawerState.open() } },
            onNewChatClick = onNewChat
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                AppTheme.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = currentTheme == theme,
                        onClick = { onThemeChange(theme) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size)
                    ) {
                        Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text("About Me", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "I am Aethra AI, your personal intelligent assistant powered by Gemini. " +
                "I can help you write, code, summarize information, and plan your day. " +
                "Developed with Jetpack Compose for a modern, fluid experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onStopGenerating: () -> Unit,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    val context = LocalContext.current
    val voiceParser = remember { VoiceToTextParser(context.applicationContext as android.app.Application) }
    val voiceState by voiceParser.state.collectAsStateWithLifecycle()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                voiceParser.startListening()
            }
        }
    )

    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            inputText = voiceState.spokenText
        }
    }

    val randomCategories = remember { allCategories.shuffled().take(4) }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Aethra AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNewChatClick) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    InitialScreen(
                        selectedCategory = selectedCategory,
                        onCategorySelect = { selectedCategory = it },
                        onSuggestionClick = {
                            inputText = it
                            selectedCategory = null
                        },
                        categories = randomCategories
                    )
                } else {
                    val groupedMessages = remember(uiState.messages) {
                        uiState.messages.groupBy { 
                            val date = Date(it.timestamp)
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            sdf.format(date)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        groupedMessages.forEach { (date, messages) ->
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = date,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            items(messages) { message ->
                                MessageBubble(
                                    message = message,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(message.text))
                                    },
                                    onEdit = if (message.isUser) {
                                        {
                                            inputText = message.text
                                            editingMessageId = message.id
                                        }
                                    } else null
                                )
                            }
                        }
                        if (uiState.isLoading) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }

            ChatInputArea(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        if (editingMessageId != null) {
                            onEditMessage(editingMessageId!!, inputText)
                            editingMessageId = null
                        } else {
                            onSendMessage(inputText)
                        }
                        inputText = ""
                        keyboardController?.hide()
                        selectedCategory = null
                    }
                },
                onStop = onStopGenerating,
                isLoading = uiState.isLoading,
                isEditing = editingMessageId != null,
                onCancelEdit = {
                    editingMessageId = null
                    inputText = ""
                },
                onVoiceClick = {
                    if (voiceState.isSpeaking) {
                        voiceParser.stopListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                isListening = voiceState.isSpeaking,
                voiceState = voiceState
            )
        }
    }
}

@Composable
fun InitialScreen(
    selectedCategory: Category?,
    onCategorySelect: (Category?) -> Unit,
    onSuggestionClick: (String) -> Unit,
    categories: List<Category>
) {
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState, enabled = isKeyboardVisible),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        if (selectedCategory == null) {
            Text(
                "What can I help with?",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            categories.chunked(2).forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCategories.forEach { category ->
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryItem(category) { onCategorySelect(category) }
                        }
                    }
                    if (rowCategories.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            SubOptionsScreen(
                category = selectedCategory,
                onBack = { onCategorySelect(null) },
                onSuggestionClick = onSuggestionClick
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SubOptionsScreen(
    category: Category,
    onBack: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(category.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.height(16.dp))
        category.suggestions.forEach { suggestion ->
            SuggestionListItem(suggestion) { onSuggestionClick(suggestion) }
        }
    }
}

@Composable
fun SuggestionListItem(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class Category(val title: String, val icon: ImageVector, val color: Color, val suggestions: List<String>)

val allCategories = listOf(
    Category("Explain Code", Icons.Default.Code, Color(0xFF00BCD4), listOf("Explain how recursion works", "Explain this Python script", "Explain REST APIs", "Explain Jetpack Compose")),
    Category("Summarize", Icons.Default.Description, Color(0xFFFF9800), listOf("Summarize this long article", "Give me key takeaways", "Bullet point these notes", "Simplify this complex topic")),
    Category("Help me write", Icons.Default.Edit, Color(0xFF9C27B0), listOf("Help me write a cover letter", "Help me write a blog post", "Help me write a screenplay", "Help me write a story")),
    Category("Make a plan", Icons.Default.Lightbulb, Color(0xFF15FF00), listOf("3-day trip to Paris", "Healthy meal plan", "Weekly workout routine", "Study schedule for exams")),
    Category("Analyze Data", Icons.Default.BarChart, Color(0xFFE91E63), listOf("Analyze this sales trend", "Interpret these statistics", "Explain correlation", "Predict future growth")),
    Category("Brainstorm", Icons.Default.Psychology, Color(0xFF3F51B5), listOf("Gift ideas for a techie", "Creative marketing ideas", "New business concepts", "Names for my pet cat")),
    Category("Translate", Icons.Default.Translate, Color(0xFF4CAF50), listOf("Translate this to Spanish", "How to say 'hello' in Japanese", "Translate an email to French", "Explain idiomatic expressions")),
    Category("Debug Error", Icons.Default.BugReport, Color(0xFF607D8B), listOf("Find the bug in this JS code", "Fix a NullPointerException", "Debug CSS layout issues", "Resolve a SQL query error")),
    Category("Learn Language", Icons.Default.Language, Color(0xFFFF5722), listOf("Start learning German", "Practice Italian grammar", "Common Korean phrases", "Learn Chinese characters")),
    Category("Design Tips", Icons.Default.Palette, Color(0xFFFFC107), listOf("Improve UI layout", "Color palette for a blog", "Typography best practices", "Modern design trends")),
    Category("Health advice", Icons.Default.Favorite, Color(0xFFFF5252), listOf("Tips for better sleep", "Manage stress levels", "Morning yoga routine", "Balanced diet principles")),
    Category("Cooking Guide", Icons.Default.Restaurant, Color(0xFF795548), listOf("Quick pasta recipe", "Bake a chocolate cake", "Vegan dinner ideas", "How to poach an egg")),
    Category("Travel Guide", Icons.Default.Flight, Color(0xFF03A9F4), listOf("Best places in Tokyo", "Budget travel tips", "Packing essentials list", "Hidden gems in Italy")),
    Category("Study Help", Icons.Default.School, Color(0xFF673AB7), listOf("Physics formula guide", "History timeline overview", "Mathematical logic help", "Biology cell structure"))
)

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth(1F)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, category.color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = category.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(category.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDrawer(
    allChats: List<ChatEntity>,
    currentChatId: String?,
    onChatSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (String) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var chatToRename by remember { mutableStateOf<ChatEntity?>(null) }
    
    if (chatToRename != null) {
        var newTitle by remember { mutableStateOf(chatToRename!!.title) }
        AlertDialog(
            onDismissRequest = { chatToRename = null },
            title = { Text("Rename Chat") },
            text = {
                TextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameChat(chatToRename!!.id, newTitle)
                    chatToRename = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { chatToRename = null }) { Text("Cancel") }
            }
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Ai Chat History",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New Chat", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Text(
                    "Recent Chats",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(allChats, key = { it.id }) { chat ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            chat.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    selected = chat.id == currentChatId,
                    onClick = { onChatSelected(chat.id) },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .combinedClickable(
                            onClick = { onChatSelected(chat.id) },
                            onLongClick = { chatToRename = chat }
                        ),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                    badge = {
                        IconButton(onClick = { onDeleteChat(chat.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Chat",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit = {},
    isLoading: Boolean,
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    isListening: Boolean = false,
    voiceState: com.vaishali.aichat.util.VoiceToTextParserState = com.vaishali.aichat.util.VoiceToTextParserState()
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column {
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Editing message",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = {
                        Text(
                            "Ask anything...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            if (isListening) {
                                val scale = 1f + (voiceState.rms / 10f).coerceIn(0f, 1f)
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                        .clip(CircleShape)
                        .background(
                            brush = if (isListening) Brush.linearGradient(
                                listOf(
                                    Color.Red,
                                    Color.Red.copy(alpha = 0.6f)
                                )
                            )
                            else SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                        )
                ) {
                    Icon(
                        if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Voice input",
                        tint = if (isListening) Color.White else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = if (isLoading) onStop else onSend,
                    enabled = (value.isNotBlank() || isLoading),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank() || isLoading) Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                            else SolidColor(Color.Gray)
                        )
                ) {
                    Icon(
                        if (isLoading) Icons.Default.Stop 
                        else if (isEditing) Icons.Default.Check 
                        else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isLoading) "Stop" else if (isEditing) "Confirm Edit" else "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val isUser = message.isUser
    val timeString = remember(message.timestamp) {
        val date = Date(message.timestamp)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(date)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column {
                SelectionContainer {
                    Text(
                        text = if (isUser) AnnotatedString(message.text) else parseMarkdown(message.text),
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0

        // Regex for Headers, Code blocks, Inline code, Bold, and Italic
        val pattern = Pattern.compile(
            "(^#+ .*$|```[\\s\\S]*?```|`[^`]+`|\\*\\*.*?\\*\\*|\\*.*?\\*)",
            Pattern.MULTILINE
        )
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            // Append text before the match
            append(text.substring(lastIndex, matcher.start()))

            val match = matcher.group()
            when {
                match.startsWith("#") -> {
                    val level = match.takeWhile { it == '#' }.length
                    val headerText = match.removePrefix("#".repeat(level)).trim()
                    val fontSize = when (level) {
                        1 -> 24.sp
                        2 -> 22.sp
                        else -> 20.sp
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize
                        )
                    ) {
                        append(headerText)
                    }
                }

                match.startsWith("```") && match.length >= 6 -> {
                    val code = match.substring(3, match.length - 3).trim()
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    ) {
                        append(code)
                    }
                }

                match.startsWith("**") && match.length >= 4 -> {
                    val boldText = match.substring(2, match.length - 2)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldText)
                    }
                }

                match.startsWith("*") && match.length >= 2 -> {
                    val italicText = match.substring(1, match.length - 1)
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicText)
                    }
                }

                match.startsWith("`") && match.length >= 2 -> {
                    val code = match.substring(1, match.length - 1)
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    ) {
                        append(code)
                    }
                }

                else -> append(match)
            }
            lastIndex = matcher.end()
        }

        // Append remaining text
        append(text.substring(lastIndex))
    }
}

@Composable
fun TypingIndicator() {
    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    AiChatTheme {
        ChatScreen(
            currentTheme = AppTheme.SYSTEM,
            onThemeChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenEmptyPreview() {
    AiChatTheme {
        ChatScreen(
            currentTheme = AppTheme.SYSTEM,
            onThemeChange = {}
        )
    }
}
