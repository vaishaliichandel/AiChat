package com.vaishali.aichat.ui.theme.chatViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaishali.aichat.data.GeminiRepository
import com.vaishali.aichat.data.local.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val allChats: List<ChatEntity> = emptyList(),
    val currentChatId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDrawerOpen: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository()
    private val chatDao = ChatDatabase.getDatabase(application).chatDao()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var chatJob: Job? = null

    init {
        observeAllChats()
    }

    private fun observeAllChats() {
        viewModelScope.launch {
            chatDao.getAllChats().collect { chats ->
                _uiState.update { it.copy(allChats = chats) }
            }
        }
    }

    fun startNewChat() {
        chatJob?.cancel()
        _uiState.update { it.copy(
            messages = emptyList(),
            currentChatId = null,
            isLoading = false,
            error = null
        ) }
    }

    fun selectChat(chatId: String) {
        chatJob?.cancel()
        viewModelScope.launch {
            chatDao.getChatWithMessages(chatId).firstOrNull()?.let { chatWithMessages ->
                _uiState.update { it.copy(
                    currentChatId = chatId,
                    messages = chatWithMessages.messages.map { it.toChatMessage() },
                    isLoading = false,
                    error = null
                ) }
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        viewModelScope.launch {
            var chatId = _uiState.value.currentChatId
            if (chatId == null) {
                chatId = UUID.randomUUID().toString()
                val newChat = ChatEntity(id = chatId, title = userInput.take(30))
                chatDao.insertChat(newChat)
                _uiState.update { it.copy(currentChatId = chatId) }
            }

            val userMessage = ChatMessage(text = userInput, isUser = true)
            chatDao.insertMessage(userMessage.toEntity(chatId))

            _uiState.update { it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null
            )}

            executeChat(chatId, userInput)
        }
    }

    private fun executeChat(chatId: String, prompt: String) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                var isFirstChunk = true
                var fullResponse = ""

                repository.sendMessageStream(prompt).collect { chunk ->
                    fullResponse += chunk
                    _uiState.update { currentState ->
                        val updatedMessages = if (isFirstChunk) {
                            currentState.messages + ChatMessage(text = chunk, isUser = false)
                        } else {
                            val lastMsg = currentState.messages.last()
                            currentState.messages.dropLast(1) + lastMsg.copy(text = lastMsg.text + chunk)
                        }

                        currentState.copy(
                            messages = updatedMessages,
                            isLoading = false
                        )
                    }
                    isFirstChunk = false
                }
                
                // Save complete AI message
                val botMessage = ChatMessage(text = fullResponse, isUser = false)
                chatDao.insertMessage(botMessage.toEntity(chatId))

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "An error occurred")
                }
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        if (newText.isBlank()) return
        
        viewModelScope.launch {
            val chatId = _uiState.value.currentChatId ?: return@launch
            
            // 1. Update the message in DB
            chatDao.updateMessageText(messageId, newText)
            
            // 2. Remove all messages after the edited message (and the message itself from UI to re-trigger)
            val currentMessages = _uiState.value.messages
            val messageIndex = currentMessages.indexOfFirst { it.id == messageId }
            
            if (messageIndex != -1) {
                // Keep messages before the edited one, update the edited one, and remove subsequent (AI response)
                val updatedMessages = currentMessages.take(messageIndex) + 
                                     currentMessages[messageIndex].copy(text = newText)
                
                // Also delete from DB the AI response that followed it
                if (messageIndex + 1 < currentMessages.size) {
                    val aiResponseId = currentMessages[messageIndex + 1].id
                    chatDao.deleteMessageById(aiResponseId)
                }

                _uiState.update { it.copy(
                    messages = updatedMessages,
                    isLoading = true,
                    error = null
                )}

                // 3. Re-trigger AI response
                executeChat(chatId, newText)
            }
        }
    }
    
    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatDao.deleteChat(chatId)
            if (_uiState.value.currentChatId == chatId) {
                startNewChat()
            }
        }
    }

    fun renameChat(chatId: String, newTitle: String) {
        viewModelScope.launch {
            chatDao.getAllChats().first().find { it.id == chatId }?.let { chat ->
                chatDao.insertChat(chat.copy(title = newTitle))
            }
        }
    }

    fun stopGenerating() {
        chatJob?.cancel()
        _uiState.update { it.copy(isLoading = false) }
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = isOpen) }
    }
}

fun MessageEntity.toChatMessage() = ChatMessage(id, text, isUser, timestamp)
fun ChatMessage.toEntity(chatId: String) = MessageEntity(id, chatId, text, isUser, timestamp)
