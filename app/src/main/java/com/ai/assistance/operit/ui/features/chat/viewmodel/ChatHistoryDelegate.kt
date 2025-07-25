package com.ai.assistance.operit.ui.features.chat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ai.assistance.operit.api.chat.EnhancedAIService
import com.ai.assistance.operit.data.model.ChatHistory
import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.data.preferences.ApiPreferences
import com.ai.assistance.operit.data.repository.ChatHistoryManager
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 委托类，负责管理聊天历史相关功能 */
class ChatHistoryDelegate(
        private val context: Context,
        private val viewModelScope: CoroutineScope,
        private val onChatHistoryLoaded: (List<ChatMessage>) -> Unit,
        private val onTokenStatisticsLoaded: (inputTokens: Int, outputTokens: Int, windowSize: Int) -> Unit,
        private val resetPlanItems: () -> Unit,
        private val getEnhancedAiService: () -> EnhancedAIService?,
        private val ensureAiServiceAvailable: () -> Unit = {}, // 确保AI服务可用的回调
        private val getChatStatistics: () -> Triple<Int, Int, Int> = { Triple(0, 0, 0) }, // 获取（输入token, 输出token, 窗口大小）
        private val onScrollToBottom: () -> Unit = {} // 滚动到底部事件回调
) {
    companion object {
        private const val TAG = "ChatHistoryDelegate"
        // 聊天总结的消息数量阈值和间隔
        private const val SUMMARY_CHUNK_SIZE = 5
    }

    private val chatHistoryManager = ChatHistoryManager.getInstance(context)
    private val isInitialized = AtomicBoolean(false)
    private val historyUpdateMutex = Mutex()

    // API设置，用于获取内存优化设置
    private val apiPreferences = ApiPreferences(context)

    // State flows
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _showChatHistorySelector = MutableStateFlow(false)
    val showChatHistorySelector: StateFlow<Boolean> = _showChatHistorySelector.asStateFlow()

    private val _chatHistories = MutableStateFlow<List<ChatHistory>>(emptyList())
    val chatHistories: StateFlow<List<ChatHistory>> = _chatHistories.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    // 跟踪是否已经创建了摘要
    private var summarizationPerformed = false

    init {
        initialize()
    }

    private fun initialize() {
        if (!isInitialized.compareAndSet(false, true)) {
            return
        }

        viewModelScope.launch {
            chatHistoryManager.chatHistoriesFlow.collect { histories ->
                _chatHistories.value = histories
            }
        }

        viewModelScope.launch {
            val initialChatId = chatHistoryManager.currentChatIdFlow.first()
            if (initialChatId != null) {
                _currentChatId.value = initialChatId
                loadChatMessages(initialChatId)
            }
        }
    }

    private suspend fun loadChatMessages(chatId: String) {
        try {
            // 直接从数据库加载消息
            val messages = chatHistoryManager.loadChatMessages(chatId)
            Log.d(TAG, "加载聊天 $chatId 的消息：${messages.size} 条")

            // 无论消息是否为空，都更新聊天历史
            _chatHistory.value = messages
            onChatHistoryLoaded(messages)

            // 查找聊天元数据，更新token统计
            val selectedChat = _chatHistories.value.find { it.id == chatId }
            if (selectedChat != null) {
                onTokenStatisticsLoaded(selectedChat.inputTokens, selectedChat.outputTokens, selectedChat.currentWindowSize)

                // 清空并重新提取计划项
                resetPlanItems()
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载聊天消息失败", e)
        }
    }

    /** 检查是否应该创建新聊天，确保同步 */
    fun checkIfShouldCreateNewChat(): Boolean {
        // 只有当历史记录和当前对话ID都已加载，且未创建过初始对话时才检查
        if (!isInitialized.get() || _currentChatId.value == null) {
            return false
        }
        return true
    }

    /** 创建新的聊天 */
    fun createNewChat() {
        viewModelScope.launch {
            val (inputTokens, outputTokens, windowSize) = getChatStatistics()
            saveCurrentChat(inputTokens, outputTokens, windowSize) // 使用获取到的完整统计数据

            val newChat = chatHistoryManager.createNewChat()
            _currentChatId.value = newChat.id
            _chatHistory.value = newChat.messages

            onChatHistoryLoaded(newChat.messages)
            onTokenStatisticsLoaded(0, 0, 0)
            resetPlanItems()
        }
    }

    /** 切换聊天 */
    fun switchChat(chatId: String) {
        viewModelScope.launch {
            val (inputTokens, outputTokens, windowSize) = getChatStatistics()
            saveCurrentChat(inputTokens, outputTokens, windowSize) // 切换前使用正确的窗口大小保存

            chatHistoryManager.setCurrentChatId(chatId)
            _currentChatId.value = chatId

            loadChatMessages(chatId)

            delay(200)
            onScrollToBottom()
        }
    }

    /** 删除聊天历史 */
    fun deleteChatHistory(chatId: String) {
        viewModelScope.launch {
            if (chatId == _currentChatId.value) {
                chatHistoryManager.deleteChatHistory(chatId)
                createNewChat()
            } else {
                chatHistoryManager.deleteChatHistory(chatId)
            }
        }
    }

    /** 删除单条消息 */
    fun deleteMessage(index: Int) {
        viewModelScope.launch {
            historyUpdateMutex.withLock {
                _currentChatId.value?.let { chatId ->
                    val currentMessages = _chatHistory.value.toMutableList()
                    if (index >= 0 && index < currentMessages.size) {
                        val messageToDelete = currentMessages[index]

                        // 从数据库删除
                        chatHistoryManager.deleteMessage(chatId, messageToDelete.timestamp)

                        // 从内存中删除
                        currentMessages.removeAt(index)
                        _chatHistory.value = currentMessages
                        onChatHistoryLoaded(currentMessages)
                    }
                }
            }
        }
    }

    /** 从指定索引删除后续所有消息 */
    fun deleteMessagesFrom(index: Int) {
        viewModelScope.launch {
            historyUpdateMutex.withLock {
                val currentMessages = _chatHistory.value
                if (index >= 0 && index < currentMessages.size) {
                    val messageToStartDeletingFrom = currentMessages[index]
                    val newHistory = currentMessages.subList(0, index)

                    // 这个方法会处理数据库和内存的更新
                    truncateChatHistory(newHistory, messageToStartDeletingFrom.timestamp)
                }
            }
        }
    }

    /** 清空当前聊天 */
    fun clearCurrentChat() {
        viewModelScope.launch {
            _currentChatId.value?.let { chatHistoryManager.deleteChatHistory(it) }
            createNewChat()
        }
    }

    /** 保存当前聊天到持久存储 */
    fun saveCurrentChat(
        inputTokens: Int = 0,
        outputTokens: Int = 0,
        actualContextWindowSize: Int = 0
    ) {
        viewModelScope.launch {
            _currentChatId.value?.let { chatId ->
                if (_chatHistory.value.isNotEmpty()) {
                    chatHistoryManager.updateChatTokenCounts(
                        chatId,
                        inputTokens,
                        outputTokens,
                        actualContextWindowSize
                    )
                }
            }
        }
    }

    /** 绑定聊天到工作区 */
    fun bindChatToWorkspace(chatId: String, workspace: String) {
        viewModelScope.launch {
            // 1. Update the database
            chatHistoryManager.updateChatWorkspace(chatId, workspace)

            // 2. Manually update the UI state to reflect the change immediately
            val updatedHistories = _chatHistories.value.map {
                if (it.id == chatId) {
                    it.copy(workspace = workspace, updatedAt = LocalDateTime.now())
                } else {
                    it
                }
            }
            _chatHistories.value = updatedHistories
        }
    }

    /** 更新聊天标题 */
    fun updateChatTitle(chatId: String, title: String) {
        viewModelScope.launch {
            // 更新数据库
            chatHistoryManager.updateChatTitle(chatId, title)

            // 更新UI状态
            val updatedHistories =
                    _chatHistories.value.map {
                        if (it.id == chatId) {
                            it.copy(title = title, updatedAt = LocalDateTime.now())
                        } else {
                            it
                        }
                    }
            _chatHistories.value = updatedHistories
        }
    }

    /** 根据第一条用户消息生成聊天标题 */
    private fun generateChatTitle(): String {
        val firstUserMessage = _chatHistory.value.firstOrNull { it.sender == "user" }?.content
        return if (firstUserMessage != null) {
            // 截取前20个字符作为标题，并添加省略号
            if (firstUserMessage.length > 20) {
                "${firstUserMessage.take(20)}..."
            } else {
                firstUserMessage
            }
        } else {
            "新对话"
        }
    }

    /** 添加消息到当前聊天 */
    fun addMessageToChat(message: ChatMessage) {
        viewModelScope.launch {
            historyUpdateMutex.withLock {
                val chatId = _currentChatId.value ?: return@withLock

                val currentMessages = _chatHistory.value
                val existingMessageIndex =
                        currentMessages.indexOfFirst { it.timestamp == message.timestamp }

                if (existingMessageIndex != -1) {
                    // val updatedMessages = currentMessages.toMutableList().apply {
                    //     this[existingMessageIndex] = message
                    // }
                    // // 更新StateFlow以触发UI重组
                    // _chatHistory.value = updatedMessages
                    // // 通知监听者（包括悬浮窗）历史记录已更新
                    // onChatHistoryLoaded(updatedMessages)
                    // // 在后台更新数据库
                    chatHistoryManager.updateMessage(chatId, message)
                } else {
                    Log.d(
                            TAG,
                            "添加新消息, stream is null: ${message.contentStream == null}, timestamp: ${message.timestamp}"
                    )
                    val newMessages = currentMessages + message
                    _chatHistory.value = newMessages
                    onChatHistoryLoaded(newMessages) // 通知UI更新
                    chatHistoryManager.addMessage(chatId, message)
                }
            }
        }
    }

    /**
     * 截断聊天记录，会同步删除数据库中指定时间戳之后的消息，并更新内存中的消息列表。
     *
     * @param newHistory 截断后保留的消息列表。
     * @param timestampOfFirstDeletedMessage 用于删除数据库记录的起始时间戳。如果为null，则清空所有消息。
     */
    fun truncateChatHistory(newHistory: List<ChatMessage>, timestampOfFirstDeletedMessage: Long?) {
        viewModelScope.launch {
            historyUpdateMutex.withLock {
                _currentChatId.value?.let { chatId ->
                    if (timestampOfFirstDeletedMessage != null) {
                        // 从数据库中删除指定时间戳之后的消息
                        chatHistoryManager.deleteMessagesFrom(
                                chatId,
                                timestampOfFirstDeletedMessage
                        )
                    } else {
                        // 如果时间戳为空，则清除该聊天的所有消息
                        chatHistoryManager.clearChatMessages(chatId)
                    }

                    // 更新内存中的聊天记录
                    _chatHistory.value = newHistory
                    onChatHistoryLoaded(newHistory)
                }
            }
        }
    }

    /** 更新整个聊天历史 用于编辑或回档等操作 */
    fun updateChatHistory(newHistory: List<ChatMessage>) {
        _chatHistory.value = newHistory.toList()
        onChatHistoryLoaded(_chatHistory.value)
    }

    /**
     * 更新聊天记录的顺序和分组
     * @param reorderedHistories 重新排序后的完整聊天历史列表
     * @param movedItem 移动的聊天项
     * @param targetGroup 目标分组的名称，如果拖拽到分组上
     */
    fun updateChatOrderAndGroup(
        reorderedHistories: List<ChatHistory>,
        movedItem: ChatHistory,
        targetGroup: String?
    ) {
        viewModelScope.launch {
            try {
                // The list is already reordered. We just need to update displayOrder and group.
                val updatedList = reorderedHistories.mapIndexed { index, history ->
                    var newGroup = history.group
                    if (history.id == movedItem.id && targetGroup != null) {
                        newGroup = targetGroup
                    }
                    history.copy(displayOrder = index.toLong(), group = newGroup)
                }

                // Update UI immediately
                _chatHistories.value = updatedList

                // Persist changes
                chatHistoryManager.updateChatOrderAndGroup(updatedList)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update chat order and group", e)
                // Optionally revert UI changes or show an error
            }
        }
    }

    /** 重命名分组 */
    fun updateGroupName(oldName: String, newName: String) {
        viewModelScope.launch {
            chatHistoryManager.updateGroupName(oldName, newName)
        }
    }

    /** 删除分组 */
    fun deleteGroup(groupName: String, deleteChats: Boolean) {
        viewModelScope.launch {
            chatHistoryManager.deleteGroup(groupName, deleteChats)
        }
    }

    /** 创建新分组（通过创建新聊天实现） */
    fun createGroup(groupName: String) {
        viewModelScope.launch {
            val (inputTokens, outputTokens, windowSize) = getChatStatistics()
            saveCurrentChat(inputTokens, outputTokens, windowSize)

            val newChat = chatHistoryManager.createNewChat(group = groupName)
            _currentChatId.value = newChat.id
            _chatHistory.value = newChat.messages

            onChatHistoryLoaded(newChat.messages)
            onTokenStatisticsLoaded(0, 0, 0)
            resetPlanItems()
        }
    }

    /** 检查是否应该生成总结 */
    fun shouldGenerateSummary(messages: List<ChatMessage>): Boolean {
        // 获取用户和AI消息
        val userAiMessages = messages.filter { it.sender == "user" || it.sender == "ai" }

        // 检查最近的消息中是否已经有总结消息
        val recentMessages =
                messages
                        .filter {
                            it.sender == "user" || it.sender == "ai" || it.sender == "summary"
                        }
                        .takeLast(minOf(SUMMARY_CHUNK_SIZE * 2, messages.size))
        val hasRecentSummary = recentMessages.any { it.sender == "summary" }

        // 如果最近已经有总结消息，不需要再生成
        if (hasRecentSummary) {
            Log.d(TAG, "最近消息中已有总结，跳过总结生成")
            return false
        }

        // 如果消息数量不足，不需要总结
        if (userAiMessages.size < SUMMARY_CHUNK_SIZE * 2) {
            Log.d(TAG, "消息数量不足，不生成总结. 当前消息数: ${userAiMessages.size}")
            return false
        }
        return true
    }

    /** 生成记忆总结 */
    suspend fun summarizeMemory(messages: List<ChatMessage>) {
        try {
            Log.d(TAG, "开始生成记忆总结...")
            val messagesForSummary = messages.filter { it.sender == "user" || it.sender == "ai" }
            val lastSummaryIndex = messages.indexOfLast { it.sender == "summary" }
            val previousSummary =
                    if (lastSummaryIndex != -1) {
                        messages[lastSummaryIndex].content.trim()
                    } else {
                        null
                    }
            val messagesToSummarize =
                    if (lastSummaryIndex == -1) {
                        messagesForSummary.take(SUMMARY_CHUNK_SIZE * 2)
                    } else {
                        val messagesAfterLastSummary = mutableListOf<ChatMessage>()
                        for (i in (lastSummaryIndex + 1) until messages.size) {
                            if (messages[i].sender == "user" || messages[i].sender == "ai") {
                                messagesAfterLastSummary.add(messages[i])
                                if (messagesAfterLastSummary.size >= SUMMARY_CHUNK_SIZE * 2) {
                                    break
                                }
                            }
                        }
                        messagesAfterLastSummary
                    }

            if (messagesToSummarize.isEmpty()) {
                Log.d(TAG, "没有新消息需要总结")
                return
            }

            Log.d(TAG, "将总结 ${messagesToSummarize.size} 条消息")

            try {
                // 确保AI服务可用
                ensureAiServiceAvailable()
            } catch (e: Exception) {
                Log.e(TAG, "确保AI服务可用时发生异常", e)
                return
            }

            try {
                // 等待一段时间以允许创建AI服务
                kotlinx.coroutines.delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "等待AI服务创建时发生异常", e)
                return
            }

            // 获取API服务实例
            val enhancedAiService = getEnhancedAiService()
            if (enhancedAiService == null) {
                Log.e(TAG, "AI服务不可用，无法生成总结")
                return
            }

            try {
                // 将消息转换为AI可以处理的格式
                val conversationToSummarize =
                        messagesToSummarize.mapIndexed { index, message ->
                            val role = if (message.sender == "user") "user" else "assistant"
                            Pair(role, "#${index + 1}: ${message.content}")
                        }

                Log.d(TAG, "开始使用AI生成对话总结：总结 ${messagesToSummarize.size} 条消息")

                // 如果有上一条摘要，传入它作为上下文
                val summary = try {
                    if (previousSummary != null) {
                        Log.d(TAG, "使用上一条摘要作为上下文生成新的总结")
                        enhancedAiService.generateSummary(
                                conversationToSummarize,
                                previousSummary
                        )
                    } else {
                        enhancedAiService.generateSummary(conversationToSummarize)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI生成总结过程中发生异常", e)
                    return
                }

                Log.d(TAG, "AI生成总结完成: ${summary.take(50)}...")

                if (summary.isBlank()) {
                    Log.e(TAG, "AI生成的总结内容为空，放弃本次总结")
                    return
                }

                // 创建总结消息
                val summaryMessage =
                        ChatMessage(
                                sender = "summary",
                                content = summary.trim(),
                                timestamp = System.currentTimeMillis()
                        )

                // 获取当前最新的消息列表
                val currentMessages = _chatHistory.value
                val newMessages = currentMessages.toMutableList()
                val insertPosition = findProperSummaryPosition(newMessages)
                Log.d(TAG, "计算出的总结插入位置: $insertPosition，总消息数量: ${newMessages.size}")

                // 在确定的位置插入总结消息
                newMessages.add(insertPosition, summaryMessage)
                Log.d(TAG, "在索引 $insertPosition 处添加总结消息，总消息数量: ${newMessages.size}")

                // 更新消息列表
                _chatHistory.value = newMessages
                // 通知ViewModel聊天历史已更新
                onChatHistoryLoaded(_chatHistory.value)

                // 更新数据库
                _currentChatId.value?.let { currentChatId ->
                    chatHistoryManager.addMessage(currentChatId, summaryMessage, insertPosition)
                }

                val totalMessages = newMessages.count { it.sender == "user" || it.sender == "ai" }
                Log.d(TAG, "总结完成，总消息数 $totalMessages")
            } catch (e: Exception) {
                Log.e(TAG, "AI生成总结失败", e)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "记忆总结失败", e)
        }
    }

    /** 找到合适的总结插入位置 对于第一次总结，放在前SUMMARY_CHUNK_SIZE条消息后面 对于后续总结，放在上一次总结之后的SUMMARY_CHUNK_SIZE条消息后面 */
    private fun findProperSummaryPosition(messages: List<ChatMessage>): Int {
        var lastSummaryIndex = messages.indexOfLast { it.sender == "summary" }
        if (lastSummaryIndex == -1) {
            lastSummaryIndex = 0
        }
        var userAiCount = 0
        // 从上一个摘要之后开始计数
        for (i in lastSummaryIndex until messages.size) {
            if (messages[i].sender == "ai") {
                userAiCount++
                // 在 SUMMARY_CHUNK_SIZE 条消息之后插入
                if (userAiCount == SUMMARY_CHUNK_SIZE) {
                    return i + 1
                }
            }
        }
        // 如果遍历完都没有达到数量，则插入到末尾
        return messages.size
    }

    /** 切换是否显示聊天历史选择器 */
    fun toggleChatHistorySelector() {
        _showChatHistorySelector.value = !_showChatHistorySelector.value
    }

    /** 显示或隐藏聊天历史选择器 */
    fun showChatHistorySelector(show: Boolean) {
        _showChatHistorySelector.value = show
    }

    /** 获取当前聊天历史的内存记录 只包含user和ai消息，并且自动截止到上次总结 */
    fun getMemory(includePlanInfo: Boolean = true): List<Pair<String, String>> {
        val messages = _chatHistory.value
        val summaryMessageIndex = messages.indexOfLast { it.sender == "summary" }
        val messagesToSummarize =
                if (summaryMessageIndex != -1) {
                    messages.subList(summaryMessageIndex, messages.size)
                } else {
                    messages
                }
        return messagesToSummarize
                .filter { it.sender == "user" || it.sender == "ai" || it.sender == "summary" }
                .map {
                    val role = if (it.sender == "ai") "assistant" else "user"
                    Pair(role, it.content)
                }
    }

    /** 获取EnhancedAIService实例 */
    private fun getEnhancedAiService(): EnhancedAIService? {
        // 使用构造函数中传入的callback获取EnhancedAIService实例
        return getEnhancedAiService.invoke()
    }

    /** 通过回调获取当前token统计数据 */
    private fun getCurrentTokenCounts(): Pair<Int, Int> {
        // 使用构造函数中传入的回调获取当前token统计数据
        val stats = getChatStatistics()
        return Pair(stats.first, stats.second)
    }
}
