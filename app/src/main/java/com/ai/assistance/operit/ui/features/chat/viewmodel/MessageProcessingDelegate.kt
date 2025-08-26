package com.ai.assistance.operit.ui.features.chat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ai.assistance.operit.R
import com.ai.assistance.operit.api.chat.EnhancedAIService
import com.ai.assistance.operit.core.chat.AIMessageManager
import com.ai.assistance.operit.data.model.*
import com.ai.assistance.operit.data.model.InputProcessingState as EnhancedInputProcessingState
import com.ai.assistance.operit.data.preferences.PromptFunctionType
import com.ai.assistance.operit.util.NetworkUtils
import com.ai.assistance.operit.util.stream.SharedStream
import com.ai.assistance.operit.util.stream.share
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 委托类，负责处理消息处理相关功能 */
class MessageProcessingDelegate(
        private val context: Context,
        private val viewModelScope: CoroutineScope,
        private val getEnhancedAiService: () -> EnhancedAIService?,
        private val getChatHistory: () -> List<ChatMessage>,
        private val addMessageToChat: (ChatMessage) -> Unit,
        private val saveCurrentChat: () -> Unit,
        private val showErrorMessage: (String) -> Unit,
        private val updateChatTitle: (chatId: String, title: String) -> Unit,
        private val onTurnComplete: () -> Unit
        // toolHandler is no longer needed here
) {
    companion object {
        private const val TAG = "MessageProcessingDelegate"
    }

    private val _userMessage = MutableStateFlow("")
    val userMessage: StateFlow<String> = _userMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputProcessingState = MutableStateFlow<EnhancedInputProcessingState>(EnhancedInputProcessingState.Idle)
    val inputProcessingState: StateFlow<EnhancedInputProcessingState> = _inputProcessingState.asStateFlow()

    private val _scrollToBottomEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToBottomEvent = _scrollToBottomEvent.asSharedFlow()

    private val _nonFatalErrorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val nonFatalErrorEvent = _nonFatalErrorEvent.asSharedFlow()

    // 当前活跃的AI响应流
    private var currentResponseStream: SharedStream<String>? = null
    // 添加一个Job来跟踪流收集协程
    private var streamCollectionJob: Job? = null

    // 获取当前活跃的AI响应流
    fun getCurrentResponseStream(): SharedStream<String>? = currentResponseStream

    init {
        Log.d(TAG, "MessageProcessingDelegate初始化: 创建滚动事件流")
    }

    fun updateUserMessage(message: String) {
        _userMessage.value = message
    }

    fun scrollToBottom() {
        _scrollToBottomEvent.tryEmit(Unit)
    }

    fun sendUserMessage(
            attachments: List<AttachmentInfo> = emptyList(),
            chatId: String? = null,
            workspacePath: String? = null,
            promptFunctionType: PromptFunctionType = PromptFunctionType.CHAT,
            enableThinking: Boolean = false,
            thinkingGuidance: Boolean = false,
            enableMemoryAttachment: Boolean = true, // 新增参数
            maxTokens: Int,
            tokenUsageThreshold: Double
    ) {
        if (_userMessage.value.isBlank() && attachments.isEmpty()) return
        if (_isLoading.value) return

        val messageText = _userMessage.value.trim()
        _userMessage.value = ""
        _isLoading.value = true
        _inputProcessingState.value = EnhancedInputProcessingState.Processing("正在处理消息...")

        viewModelScope.launch(Dispatchers.IO) {
            // 检查这是否是聊天中的第一条用户消息
            val isFirstMessage = getChatHistory().none { it.sender == "user" || it.sender == "ai" }
            if (isFirstMessage && chatId != null) {
                val newTitle =
                    when {
                        messageText.isNotBlank() -> messageText
                        attachments.isNotEmpty() -> attachments.first().fileName
                        else -> context.getString(R.string.new_conversation)
                    }
                updateChatTitle(chatId, newTitle)
            }

            Log.d(TAG, "开始处理用户消息：附件数量=${attachments.size}")

            // 1. 使用 AIMessageManager 构建最终消息
            val finalMessageContent = AIMessageManager.buildUserMessageContent(
                messageText,
                attachments,
                enableMemoryAttachment
            )

            addMessageToChat(ChatMessage(sender = "user", content = finalMessageContent))

            lateinit var aiMessage: ChatMessage
            try {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    withContext(Dispatchers.Main) { showErrorMessage("网络连接不可用") }
                    _isLoading.value = false
                    _inputProcessingState.value = EnhancedInputProcessingState.Idle
                    return@launch
                }

                val service =
                    getEnhancedAiService()
                        ?: run {
                            withContext(Dispatchers.Main) { showErrorMessage("AI服务未初始化") }
                            _isLoading.value = false
                            _inputProcessingState.value = EnhancedInputProcessingState.Idle
                            return@launch
                        }

                val startTime = System.currentTimeMillis()
                val deferred = CompletableDeferred<Unit>()

                // 2. 使用 AIMessageManager 发送消息
                val responseStream = AIMessageManager.sendMessage(
                    enhancedAiService = service,
                    messageContent = finalMessageContent,
                    chatHistory = getChatHistory(),
                    workspacePath = workspacePath,
                    promptFunctionType = promptFunctionType,
                    enableThinking = enableThinking,
                    thinkingGuidance = thinkingGuidance,
                    enableMemoryAttachment = enableMemoryAttachment, // Pass it here
                    maxTokens = maxTokens,
                    tokenUsageThreshold = tokenUsageThreshold,
                    onNonFatalError = { error ->
                        _nonFatalErrorEvent.emit(error)
                    }
                )

                // 将字符串流共享，以便多个收集器可以使用
                val sharedCharStream =
                    responseStream.share(
                        scope = viewModelScope,
                        replay = 0, // 不重放历史消息
                        onComplete = {
                            deferred.complete(Unit)
                            Log.d(
                                TAG,
                                "共享流完成，耗时: ${System.currentTimeMillis() - startTime}ms"
                            )
                            currentResponseStream = null // 清除本地引用
                        }
                    )

                // 更新当前响应流，使其可以被其他组件（如悬浮窗）访问
                currentResponseStream = sharedCharStream

                aiMessage = ChatMessage(
                    sender = "ai", 
                    contentStream = sharedCharStream,
                    timestamp = System.currentTimeMillis()+50
                )
                Log.d(
                    TAG,
                    "创建带流的AI消息, stream is null: ${aiMessage.contentStream == null}, timestamp: ${aiMessage.timestamp}"
                )

                withContext(Dispatchers.Main) { addMessageToChat(aiMessage) }

                // 启动一个独立的协程来收集流内容并持续更新数据库
                streamCollectionJob =
                    viewModelScope.launch(Dispatchers.IO) {
                        val contentBuilder = StringBuilder()
                        sharedCharStream.collect { chunk ->
                            contentBuilder.append(chunk)
                            val content = contentBuilder.toString()
                            val updatedMessage = aiMessage.copy(content = content)
                            // 防止后续读取不到
                            aiMessage.content = content
                            addMessageToChat(updatedMessage)
                            _scrollToBottomEvent.tryEmit(Unit)
                        }
                    }

                // 等待流完成，以便finally块可以正确执行来更新UI状态
                deferred.await()

                Log.d(TAG, "AI响应处理完成，总耗时: ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "消息发送被取消")
                    throw e
                }
                Log.e(TAG, "发送消息时出错", e)
                withContext(Dispatchers.Main) { showErrorMessage("发送消息失败: ${e.message}") }
            } finally {
                // 修改为使用 try-catch 来检查变量是否已初始化，而不是使用 ::var.isInitialized
                try {
                    // 尝试访问 aiMessage，如果未初始化会抛出 UninitializedPropertyAccessException
                    val finalContent = aiMessage.content
                    val finalMessage = aiMessage.copy(content = finalContent, contentStream = null)
                    withContext(Dispatchers.Main) { addMessageToChat(finalMessage) }
                } catch (e: UninitializedPropertyAccessException) {
                    // aiMessage 未初始化，忽略清理步骤
                    Log.d(TAG, "AI消息未初始化，跳过流清理步骤")
                }

                // 清理job引用
                streamCollectionJob = null

                // 添加一个短暂的延迟，以确保UI有足够的时间来渲染最后一个数据块
                // 这有助于解决因竞态条件导致的UI内容（如状态标签）有时无法显示的问题
                withContext(Dispatchers.IO) { delay(100) }
                withContext(Dispatchers.Main) {
                    // 状态现在由 EnhancedAIService 的 inputProcessingState 控制，这里不再重置
                    // _isLoading.value = false
                    // _isProcessingInput.value = false

                    // 即使流处理完成，也需要保存一次聊天记录
                    onTurnComplete()
                }
            }
        }
    }

    fun cancelCurrentMessage() {
        viewModelScope.launch {
            _isLoading.value = false
            _inputProcessingState.value = EnhancedInputProcessingState.Idle

            // 取消正在进行的流收集
            streamCollectionJob?.cancel()
            streamCollectionJob = null
            Log.d(TAG, "流收集任务已取消")

            withContext(Dispatchers.IO) {
                getEnhancedAiService()?.cancelConversation()
                saveCurrentChat()
            }
        }
    }

    fun setInputProcessingState(isProcessing: Boolean, message: String) {
        if(isProcessing) {
            _inputProcessingState.value = EnhancedInputProcessingState.Processing(message)
        } else {
            _inputProcessingState.value = EnhancedInputProcessingState.Idle
        }
    }

    /**
     * 处理来自 EnhancedAIService 的输入处理状态
     * @param state 输入处理状态
     */
    fun handleInputProcessingState(state: EnhancedInputProcessingState) {
        viewModelScope.launch(Dispatchers.Main) {
            _inputProcessingState.value = state
            _isLoading.value = state !is EnhancedInputProcessingState.Idle && state !is EnhancedInputProcessingState.Completed

            when (state) {
                is EnhancedInputProcessingState.Error -> {
                    showErrorMessage(state.message)
                }
                else -> {
                    // Do nothing for other states as they are handled by the state flow itself
                }
            }
        }
    }
}
