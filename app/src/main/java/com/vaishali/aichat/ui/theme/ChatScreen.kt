package com.vaishali.aichat.ui.theme

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
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
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

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
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(uiState.messages) { message ->
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
                isLoading = uiState.isLoading,
                isEditing = editingMessageId != null,
                onCancelEdit = {
                    editingMessageId = null
                    inputText = ""
                }
            )
        }
    }
}

@Composable
@Preview
fun showInit() {
    InitialScreen("OK", {}, {})
}

@Composable
fun InitialScreen(
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    onSuggestionClick: (String) -> Unit
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

            // Replaced LazyVerticalGrid with Rows to support outer scrolling when keyboard is open
            mainCategories.chunked(2).forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCategories.forEach { category ->
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryItem(category) { onCategorySelect(category.title) }
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
    category: String,
    onBack: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = when (category) {
        "Help me write" -> listOf(
            "Help me write a cover letter",
            "Help me write a blog post",
            "Help me write a screenplay",
            "Help me write a bedtime story"
        )

        "Explain Code" -> listOf(
            "Explain how recursion works",
            "Explain this Python script",
            "Explain REST APIs",
            "Explain Jetpack Compose"
        )

        "Summarize" -> listOf(
            "Summarize this long article",
            "Give me key takeaways from this text",
            "Bullet point these meeting notes",
            "Simplify this complex topic"
        )

        "Make a plan" -> listOf(
            "3-day trip to Paris",
            "Healthy meal plan",
            "Weekly workout routine",
            "Study schedule for exams"
        )

        else -> emptyList()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(category, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.height(16.dp))
        suggestions.forEach { suggestion ->
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

data class Category(val title: String, val icon: ImageVector, val color: Color)

val mainCategories = listOf(
    Category("Explain Code", Icons.Default.Code, Color(0xFF00BCD4)),
    Category("Summarize", Icons.Default.Description, Color(0xFFFF9800)),
    Category("Help me write", Icons.Default.Edit, Color(0xFF9C27B0)),
    Category("Make a plan", Icons.Default.Lightbulb, Color(0xFF15FF00))
)

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth(1F)
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

@Composable
fun ChatDrawer(
    allChats: List<ChatEntity>,
    currentChatId: String?,
    onChatSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
            items(allChats) { chat ->
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
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) }
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
    isLoading: Boolean,
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {}
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
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank()) Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                            else Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                        )
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isEditing) "Confirm Edit" else "Send",
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
            Text(
                text = if (isUser) AnnotatedString(message.text) else parseMarkdown(message.text),
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge
            )
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
