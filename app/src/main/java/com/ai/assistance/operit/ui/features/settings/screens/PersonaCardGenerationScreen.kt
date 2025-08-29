package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.CharacterCard
import com.ai.assistance.operit.data.model.PromptTag
import com.ai.assistance.operit.data.preferences.CharacterCardManager
import com.ai.assistance.operit.data.preferences.PromptTagManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

// --- 本地最小工具执行器：仅处理 save_character_info ---
private object LocalCharacterToolExecutor {
    const val TOOL_NAME = "save_character_info"

    fun extractInvocations(raw: String): List<Pair<String, Map<String, String>>> {
        val list = mutableListOf<Pair<String, Map<String, String>>>()
        // 简单 XML 提取：<tool name="..."> <param name="field">..</param><param name="content">..</param></tool>
        val toolRegex = Regex("(?s)<tool\\s+name=\"([^\"]+)\">([\\s\\S]*?)</tool>")
        val paramRegex = Regex("(?s)<param\\s+name=\"([^\"]+)\">([\\s\\S]*?)</param>")
        toolRegex.findAll(raw).forEach { m ->
            val name = m.groupValues.getOrNull(1)?.trim().orEmpty()
            val body = m.groupValues.getOrNull(2) ?: ""
            val params = mutableMapOf<String, String>()
            paramRegex.findAll(body).forEach { pm ->
                val pName = pm.groupValues.getOrNull(1)?.trim().orEmpty()
                val pVal = pm.groupValues.getOrNull(2)?.trim().orEmpty()
                params[pName] = pVal
            }
            list.add(name to params)
        }
        return list
    }

    suspend fun executeSaveCharacterInfo(
        context: android.content.Context,
        characterCardId: String,
        field: String,
        content: String
    ): com.ai.assistance.operit.data.model.ToolResult {
        return try {
            val manager = CharacterCardManager.getInstance(context)
            
            // 获取当前角色卡
            val currentCard = manager.getCharacterCard(characterCardId)
            if (currentCard == null) {
                return com.ai.assistance.operit.data.model.ToolResult(
                    toolName = TOOL_NAME,
                    success = false,
                    result = com.ai.assistance.operit.core.tools.StringResultData(""),
                    error = "角色卡不存在"
                )
            }
            
            // 根据字段更新对应内容
            val updatedCard = when (field) {
                "name" -> currentCard.copy(name = content)
                "description" -> currentCard.copy(description = content)
                "characterSetting" -> currentCard.copy(characterSetting = content)
                "otherContent" -> currentCard.copy(otherContent = content)
                "advancedCustomPrompt" -> currentCard.copy(advancedCustomPrompt = content)
                else -> {
                    return com.ai.assistance.operit.data.model.ToolResult(
                        toolName = TOOL_NAME,
                        success = false,
                        result = com.ai.assistance.operit.core.tools.StringResultData(""),
                        error = "不支持的字段: $field"
                    )
                }
            }
            
            withContext(kotlinx.coroutines.Dispatchers.IO) { 
                manager.updateCharacterCard(updatedCard)
            }
            
            com.ai.assistance.operit.data.model.ToolResult(
                toolName = TOOL_NAME,
                success = true,
                result = com.ai.assistance.operit.core.tools.StringResultData("ok"),
                error = null
            )
        } catch (e: Exception) {
            com.ai.assistance.operit.data.model.ToolResult(
                toolName = TOOL_NAME,
                success = false,
                result = com.ai.assistance.operit.core.tools.StringResultData(""),
                error = e.message
            )
        }
    }
}

private data class CharacterChatMessage(
    val role: String, // "user" | "assistant"
    var content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaCardGenerationScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToUserPreferences: () -> Unit = {},
    onNavigateToModelConfig: () -> Unit = {},
    onNavigateToModelPrompts: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val TAG = "CharacterCardGeneration"

    // 引导文案（顶部说明）
    val characterAssistantIntro = remember {
        """
        嗨嗨～这里是你的角色卡小助手(｡･ω･｡)ﾉ♡ 我会陪你一起把专属角色慢慢捏出来～
        我们按部就班来哦：先告诉我你的称呼，再说说你想要的角色大方向，比方说：
        - 角色名字和身份大概是怎样的？
        - 有哪些可爱的性格关键词？
        - 长相/发型/瞳色/穿搭想要什么感觉？
        - 有没有特别的小设定或能力？
        - 跟其他角色的关系要不要安排一点点？
        
        接下来我会一步步问你关键问题，帮你把细节补齐～
        """.trimIndent()
    }

    val listState = rememberLazyListState()
    val chatMessages = remember { mutableStateListOf<CharacterChatMessage>() }
    var userInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // 角色卡数据
    val characterCardManager = remember { CharacterCardManager.getInstance(context) }
    val tagManager = remember { PromptTagManager.getInstance(context) }
    var allCharacterCards by remember { mutableStateOf(listOf<CharacterCard>()) }
    var allTags by remember { mutableStateOf(listOf<PromptTag>()) }
    var activeCardId by remember { mutableStateOf("") }
    var activeCard by remember { mutableStateOf<CharacterCard?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCardName by remember { mutableStateOf("") }

    // 编辑器值
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editCharacterSetting by remember { mutableStateOf("") }
    var editOtherContent by remember { mutableStateOf("") }
    var editAdvancedCustomPrompt by remember { mutableStateOf("") }

    // 初始化数据
    LaunchedEffect(Unit) {
        if (chatMessages.isEmpty()) {
            chatMessages.add(
                CharacterChatMessage("assistant", characterAssistantIntro)
        )
        }
        
        withContext(Dispatchers.IO) {
            characterCardManager.initializeIfNeeded()
            allCharacterCards = characterCardManager.getAllCharacterCards()
            allTags = tagManager.getAllTags()

            activeCardId = characterCardManager.activeCharacterCardIdFlow.first()
            activeCard = characterCardManager.getCharacterCard(activeCardId)

            // 如果没有活跃卡，并且列表不为空，则设置第一个为活跃
            if (activeCard == null && allCharacterCards.isNotEmpty()) {
                val firstCardId = allCharacterCards.first().id
                characterCardManager.setActiveCharacterCard(firstCardId)
                activeCardId = firstCardId
                activeCard = characterCardManager.getCharacterCard(firstCardId)
            }
        }
        
        activeCard?.let { card ->
            editName = card.name
            editDescription = card.description
            editCharacterSetting = card.characterSetting
            editOtherContent = card.otherContent
            editAdvancedCustomPrompt = card.advancedCustomPrompt
        }
    }

    fun refreshData() {
        scope.launch(Dispatchers.IO) {
            allCharacterCards = characterCardManager.getAllCharacterCards()
            activeCardId = characterCardManager.activeCharacterCardIdFlow.first()
            activeCard = characterCardManager.getCharacterCard(activeCardId)
            
            withContext(Dispatchers.Main) {
                activeCard?.let { card ->
                    editName = card.name
                    editDescription = card.description
                    editCharacterSetting = card.characterSetting
                    editOtherContent = card.otherContent
                    editAdvancedCustomPrompt = card.advancedCustomPrompt
                }
            }
        }
    }

    // 通过默认底层 AIService 发送消息
    suspend fun requestFromDefaultService(
        fullPrompt: String,
        historyPairs: List<Pair<String, String>>
    ): com.ai.assistance.operit.util.stream.Stream<String> = withContext(Dispatchers.IO) {
        val aiService = com.ai.assistance.operit.api.chat.EnhancedAIService
            .getInstance(context)
            .getAIServiceForFunction(FunctionType.CHAT)
        val functionalConfigManager = com.ai.assistance.operit.data.preferences.FunctionalConfigManager(context)
        functionalConfigManager.initializeIfNeeded()
        val configId = functionalConfigManager.getConfigIdForFunction(FunctionType.CHAT)
        val modelParameters = com.ai.assistance.operit.data.preferences.ModelConfigManager(context)
            .getModelParametersForConfig(configId)
        aiService.sendMessage(
            message = fullPrompt,
            chatHistory = historyPairs,
            modelParameters = modelParameters,
            enableThinking = false
        )
    }

    // 解析并执行工具调用
    suspend fun processToolInvocations(rawContent: String, assistantIndex: Int) {
        try {
            val invList = LocalCharacterToolExecutor.extractInvocations(rawContent)
            if (invList.isEmpty()) return

            Log.d(TAG, "Found ${invList.size} tool invocation(s).")
            invList.forEach { (name, params) ->
                Log.d(TAG, "Tool invocation: name='$name', params=$params")

                if (name != LocalCharacterToolExecutor.TOOL_NAME) {
                    Log.w(TAG, "Skipping unknown tool: '$name'")
                    return@forEach
                }
                val field = params["field"].orEmpty().trim()
                val content = params["content"].orEmpty().trim()
                val cardId = activeCardId

                if (field.isBlank() || content.isBlank()) {
                    Log.w(TAG, "Skipping tool call with blank field or content.")
                    return@forEach
                }

                val result = LocalCharacterToolExecutor.executeSaveCharacterInfo(context, cardId, field, content)
                if (result.success) {
                    Log.d(TAG, "Tool '$name' executed successfully for field '$field'.")
                } else {
                    Log.e(TAG, "Tool '$name' execution failed for field '$field': ${result.error}")
                }

                // 刷新数据
                withContext(Dispatchers.Main) {
                    refreshData()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Local tool processing failed: ${e.message}", e)
        }
    }

    fun sendMessage() {
        if (userInput.isBlank() || isGenerating) return
        val input = userInput
        userInput = ""

        scope.launch(Dispatchers.Main) {
            chatMessages.add(CharacterChatMessage("user", input))
            isGenerating = true

            val guidancePrefix = """
                你是"角色卡生成助手"。请在每次回复中自行判断当前进度并进入还没完成的步骤，遵循以下多步流程：
                [步骤]
                1) 角色名称：询问并确认角色名称
                2) 角色描述：简短的角色描述
                3) 角色设定：详细的角色设定，包括身份、外貌、性格等
                4) 其他内容：背景故事、特殊能力等补充信息
                5) 高级自定义：特殊的提示词或交互方式
                [规则]
                - 全程语气要活泼可爱喵~
                - 每轮对话如果用户输入了角色设定就对其进行适当优化与丰富，然后用一小段话总结当前的进度
                - 如果用户说"随便/你看着写"，就帮用户体贴地生成设定内容，合理细节并输出生成的内容
                - 生成或者补充完之后判断现在到哪一步或者还有什么需要补充的，然后对于下一个步骤提几个最关键、最具体的小问题
                - 不要重复问已经确认过的内容，也不要一下子把所有问题都问完，慢慢来更贴心
                [工具调用]
                - 每轮对话必须进行判断，如果本轮对话得到了新的角色信息，你必须调用一次工具保存信息
                - field 取值限定为："name" | "description" | "characterSetting" | "otherContent" | "advancedCustomPrompt"
                - content 该部分对应的优化丰富后的设定文本，不要带有其他多余内容
                - 请勿在对话可见内容中展示任何工具调用，仅在内部使用
                - 工具调用XML示例：
                  <tool name="save_character_info"><param name="field">name</param><param name="content">角色名称</param></tool>
            """.trimIndent()

            val historyPairs = withContext(Dispatchers.Default) {
                chatMessages.map { it.role to it.content }
            }

            val characterJson = withContext(Dispatchers.IO) {
                activeCard?.let { card ->
                    JSONObject().apply {
                        put("name", card.name)
                        put("description", card.description)
                        put("characterSetting", card.characterSetting)
                        put("otherContent", card.otherContent)
                        put("advancedCustomPrompt", card.advancedCustomPrompt)
                    }.toString()
                } ?: "{}"
            }

            val stream = run {
                val fullPrompt = buildString {
                    append(guidancePrefix)
                    append('\n')
                    append("[当前角色卡信息] ")
                    append(characterJson)
                    append('\n')
                    append("[用户输入] ")
                    append(input)
                }
                requestFromDefaultService(fullPrompt, historyPairs)
            }

            // 提前插入占位的"生成中…"助手消息
            chatMessages.add(CharacterChatMessage("assistant", "生成中…"))
            val assistantIndex = chatMessages.lastIndex

            val toolTagRegex = Regex("(?s)\\s*<tool\\b[\\s\\S]*?</tool>\\s*")
            val toolResultRegex = Regex("(?s)\\s*<tool_result\\s+name=\"[^\"]+\"\\s+status=\"[^\"]+\"[^>]*>[\\s\\S]*?</tool_result>\\s*")
            val statusRegex = Regex("(?s)\\s*<status\\b[^>]*>[\\s\\S]*?</status>\\s*")

            // 原始缓冲，用于工具解析
            val rawBuffer = StringBuilder()
            var firstChunkReceived = false

            try {
                withContext(Dispatchers.IO) {
                    stream.collect { chunk ->
                        rawBuffer.append(chunk)
                        withContext(Dispatchers.Main) {
                            if (!firstChunkReceived) {
                                firstChunkReceived = true
                                isGenerating = false
                            }
                            val sanitized = (chatMessages[assistantIndex].content.replace("生成中…", "") + chunk)
                                .replace(toolTagRegex, "")
                                .replace(toolResultRegex, "")
                                .replace(statusRegex, "")
                                .replace(Regex("(\\r?\\n){2,}"), "\n")
                            chatMessages[assistantIndex] = chatMessages[assistantIndex].copy(content = sanitized)
                            scope.launch { listState.animateScrollToItem(chatMessages.lastIndex) }
                        }
                    }
                }

                // 流结束后解析并执行工具
                withContext(Dispatchers.IO) {
                    processToolInvocations(rawBuffer.toString(), assistantIndex)
                }
            } catch (e: Exception) {
                chatMessages.add(
                    CharacterChatMessage(
                        role = "assistant",
                        content = "发送失败：${e.message ?: "未知错误"}"
                    )
                )
            } finally {
                isGenerating = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("角色卡配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    // 选择不同角色卡
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = activeCard?.name ?: "无角色卡",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("当前角色卡") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            allCharacterCards.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text(card.name) },
                                    onClick = {
                                        expanded = false
                                        scope.launch {
                                            characterCardManager.setActiveCharacterCard(card.id)
                                            refreshData()
                                        }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ 新建角色卡") },
                                onClick = {
                                    expanded = false
                                    showCreateDialog = true
                                }
                            )
                        }
                    }

                    // 删除当前角色卡（默认卡不可删）
                    if (activeCard?.isDefault == false) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("删除当前角色卡")
                            }
                        }
                    }

                    // 新建角色卡弹窗
                    if (showCreateDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreateDialog = false },
                            title = { Text("新建角色卡") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = newCardName,
                                        onValueChange = { newCardName = it },
                                        singleLine = true,
                                        label = { Text("角色卡名称") },
                                        placeholder = { Text("例如：小昔-校园版") }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val name = newCardName.trim().ifBlank { "新角色" }
                                    showCreateDialog = false
                                    newCardName = ""
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val newCard = CharacterCard(
                                                id = "",
                                                name = name,
                                                description = "",
                                                characterSetting = "",
                                                otherContent = "",
                                                attachedTagIds = emptyList(),
                                                advancedCustomPrompt = "",
                                                isDefault = false
                                            )
                                            val newId = characterCardManager.createCharacterCard(newCard)
                                            characterCardManager.setActiveCharacterCard(newId)
                                        }
                                        refreshData()
                                    }
                                }) { Text("创建") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
                            }
                        )
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("删除角色卡") },
                            text = { Text("确定删除当前角色卡吗？此操作不可撤销。") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    scope.launch {
                                        activeCard?.let { card ->
                                            withContext(Dispatchers.IO) {
                                                characterCardManager.deleteCharacterCard(card.id)
                                                // 删除后，activeCharacterCardIdFlow 会自动更新为列表中的第一项
                                                // 或者如果没有角色卡，会是空字符串
                                            }
                                            refreshData()
                                        }
                                    }
                                }) { Text("删除") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("当前角色卡内容", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    
                    // 角色名称
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { newValue ->
                            editName = newValue
                            scope.launch {
                                activeCard?.let { card ->
                                    withContext(Dispatchers.IO) {
                                        characterCardManager.updateCharacterCard(card.copy(name = newValue))
                                    }
                                }
                            }
                        },
                        label = { Text("角色名称") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 角色描述
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { newValue ->
                            editDescription = newValue
                            scope.launch {
                                activeCard?.let { card ->
                                    withContext(Dispatchers.IO) {
                                        characterCardManager.updateCharacterCard(card.copy(description = newValue))
                                    }
                                }
                            }
                        },
                        label = { Text("角色描述") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 角色设定
                    OutlinedTextField(
                        value = editCharacterSetting,
                        onValueChange = { newValue ->
                            editCharacterSetting = newValue
                            scope.launch {
                                activeCard?.let { card ->
                                    withContext(Dispatchers.IO) {
                                        characterCardManager.updateCharacterCard(card.copy(characterSetting = newValue))
                                    }
                                }
                            }
                        },
                        label = { Text("角色设定") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 其他内容
                            OutlinedTextField(
                        value = editOtherContent,
                                onValueChange = { newValue ->
                            editOtherContent = newValue
                            scope.launch {
                                activeCard?.let { card ->
                                    withContext(Dispatchers.IO) {
                                        characterCardManager.updateCharacterCard(card.copy(otherContent = newValue))
                                    }
                                }
                            }
                                },
                        label = { Text("其他内容") },
                                modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 高级自定义提示词
                    OutlinedTextField(
                        value = editAdvancedCustomPrompt,
                        onValueChange = { newValue ->
                            editAdvancedCustomPrompt = newValue
                            scope.launch {
                                activeCard?.let { card ->
                                    withContext(Dispatchers.IO) {
                                        characterCardManager.updateCharacterCard(card.copy(advancedCustomPrompt = newValue))
                        }
                    }
                            }
                        },
                        label = { Text("高级自定义提示词") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "角色卡生成助手", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { scope.launch { drawerState.open() } }) {
                    Text(activeCard?.name ?: "无角色卡")
                }
            }

            // 聊天列表
            val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            LaunchedEffect(chatMessages.size) {
                if (chatMessages.isNotEmpty()) {
                    listState.animateScrollToItem(chatMessages.lastIndex)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                items(chatMessages) { msg ->
                    val isUser = msg.role == "user"
                    val bubbleContainer = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val bubbleTextColor = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!isUser) {
                            Card(colors = CardDefaults.cardColors(containerColor = bubbleContainer)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(msg.content, color = bubbleTextColor)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        timeFormatter.format(Date(msg.timestamp)),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                            Card(colors = CardDefaults.cardColors(containerColor = bubbleContainer)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(msg.content, color = bubbleTextColor)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        timeFormatter.format(Date(msg.timestamp)),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(80.dp)) }
            }

            // 底部输入栏
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        placeholder = { Text(if (isGenerating) "正在生成…" else "描述你想要的角色…") },
                        enabled = !isGenerating,
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { if (!isGenerating) sendMessage() },
                        enabled = !isGenerating
                    ) {
                        Icon(
                            imageVector = if (isGenerating) Icons.Filled.HourglassBottom else Icons.Filled.Send,
                            contentDescription = if (isGenerating) "生成中" else "发送"
                        )
                    }
                }
            }
        }
    }
} 