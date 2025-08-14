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
import com.ai.assistance.operit.data.preferences.PersonaCardPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.ai.assistance.operit.api.chat.enhance.ConversationMarkupManager
import android.util.Log

// --- 本地最小工具执行器：仅处理 save_persona_info ---
private object LocalPersonaToolExecutor {
    const val TOOL_NAME = "save_persona_info"

    fun extractInvocations(raw: String): List<Pair<String, Map<String, String>>> {
        val list = mutableListOf<Pair<String, Map<String, String>>>()
        // 简单 XML 提取：<tool name="..."> <param name="section">..</param><param name="content">..</param></tool>
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

    suspend fun executeSavePersonaInfo(
        context: android.content.Context,
        section: String,
        content: String
    ): com.ai.assistance.operit.data.model.ToolResult {
        return try {
            val prefs = com.ai.assistance.operit.data.preferences.PersonaCardPreferences(context)
            withContext(kotlinx.coroutines.Dispatchers.IO) { prefs.saveSection(section, content) }
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

private data class PersonaChatMessage(
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
    val TAG = "PersonaCardGeneration"

    // 引导文案（顶部说明）
    val personaAssistantIntro = remember {
        """
        嗨嗨～这里是你的人设小助手(｡･ω･｡)ﾉ♡ 我会陪你一起把专属人设慢慢捏出来～
        我们按部就班来哦：先告诉我你的称呼，再说说你想要的人设大方向，比方说：
        - 角色名字和身份大概是怎样的？
        - 有哪些可爱的性格关键词？
        - 长相/发型/瞳色/穿搭想要什么感觉？
        - 有没有特别的小设定或能力？
        - 跟其他角色的关系要不要安排一点点？
        
        接下来我会一步步问你关键问题，帮你把细节补齐～
        """.trimIndent()
    }

    val listState = rememberLazyListState()
    val chatMessages = remember { mutableStateListOf<PersonaChatMessage>() }
    var userInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // 人设卡数据
    val prefs = remember { PersonaCardPreferences(context) }
    var profiles by remember { mutableStateOf(listOf<String>()) }
    var activeProfile by remember { mutableStateOf("默认卡") }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    // 侧栏展示用的快照（从数据库直接拉取最新值）
    var sectionSnapshot by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 文本框的单一数据源：与数据库双向同步
    val editorValues = remember(activeProfile) { mutableStateMapOf<String, String>() }

    // 同步：当 activeProfile 变化时，拉取一次快照
    LaunchedEffect(activeProfile) {
        sectionSnapshot = withContext(Dispatchers.IO) { prefs.getSectionsSnapshot(activeProfile) }
        editorValues.clear()
        editorValues.putAll(sectionSnapshot)
    }

    // 同时订阅Flow用于后续自动刷新（数据库写入后会推送变更）
    val sections by remember(activeProfile) { prefs.sectionsFlow(activeProfile) }
        .collectAsState(initial = emptyMap())
    LaunchedEffect(sections) {
        sectionSnapshot = sections
        editorValues.clear()
        editorValues.putAll(sectionSnapshot)
    }

    LaunchedEffect(Unit) {
        if (chatMessages.isEmpty()) chatMessages.add(
            PersonaChatMessage(
                "assistant",personaAssistantIntro
            )
        )
        val ensuredActive = prefs.ensureDefaultProfile()
        prefs.seedDefaultPersonaIfEmpty()
        profiles = prefs.profilesFlow.first()
        activeProfile = ensuredActive
        sectionSnapshot = withContext(Dispatchers.IO) { prefs.getSectionsSnapshot(activeProfile) }
        editorValues.clear()
        editorValues.putAll(sectionSnapshot)
    }

    fun refreshSnapshot() {
        scope.launch(Dispatchers.IO) {
            val snap = prefs.getSectionsSnapshot(activeProfile)
            withContext(Dispatchers.Main) {
                sectionSnapshot = snap
                editorValues.clear()
                editorValues.putAll(sectionSnapshot)
            }
        }
    }

    // 新增：通过默认底层 AIService 发送消息（不依赖 EnhancedAIService）
    suspend fun requestFromDefaultService(
        fullPrompt: String,
        historyPairs: List<Pair<String, String>>
    ): com.ai.assistance.operit.util.stream.Stream<String> = withContext(Dispatchers.IO) {
        val aiService = com.ai.assistance.operit.api.chat.EnhancedAIService
            .getInstance(context)
            .getAIServiceForFunction(FunctionType.CHAT)
        val modelParameters = com.ai.assistance.operit.data.preferences.ApiPreferences(context)
            .getAllModelParameters()
        aiService.sendMessage(
            message = fullPrompt,
            chatHistory = historyPairs,
            modelParameters = modelParameters,
            enableThinking = false
        )
    }

    // 解析并执行工具调用（本地版，仅支持 save_persona_info）
    suspend fun processToolInvocations(rawContent: String, assistantIndex: Int) {
        try {
            val invList = LocalPersonaToolExecutor.extractInvocations(rawContent)
            if (invList.isEmpty()) return

            invList.forEach { (name, params) ->
                if (name != LocalPersonaToolExecutor.TOOL_NAME) return@forEach
                val section = params["section"].orEmpty().trim()
                val content = params["content"].orEmpty().trim()
                val result = if (section.isBlank() || content.isBlank()) {
                    com.ai.assistance.operit.data.model.ToolResult(
                        toolName = name,
                        success = false,
                        result = com.ai.assistance.operit.core.tools.StringResultData(""),
                        error = "缺少必要参数 section 或 content"
                    )
                } else {
                    LocalPersonaToolExecutor.executeSavePersonaInfo(context, section, content)
                }
                // 隐藏工具结果在可见聊天中，不追加任何可见文本
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
            chatMessages.add(PersonaChatMessage("user", input))
            isGenerating = true

            val guidancePrefix = """
                你是“人设卡生成助手”。请在每次回复中自行判断当前进度并进入还没完成的步骤，遵循以下多步流程（有限状态机）：
                [步骤]
                1) 角色名称：询问并确认 角色名称；
                2）基础设定：询问并确认 身份概述与角色设定；
                3) 外貌特征：发型发色、眼睛/瞳色、身材、显著特征、穿着风格；
                4) 性格与爱好：3~6 个关键词，兴趣/偏好；
                5) 背景故事：简短合理的来历设定；
                6) 说话风格：称呼、语气特点、口头禅或表达方式；
                [规则]
                - 全程语气要活泼可爱喵~
                - 每轮对话如果用户输入了角色设定就对其进行适当优化与丰富，然后用一小段话总结当前的进度。
                - 如果用户说“随便/你看着写”，就帮用户体贴地生成设定内容，合理细节并输出生成的内容；
                - 生成或者补充完之后判断现在到哪一步或者还有什么需要补充的，然后对于下一个步骤提几个最关键、最具体的小问题，像陪用户做问卷一样耐心可爱；
                - 不要重复问已经确认过的内容，也不要一下子把所有问题都问完，慢慢来更贴心；
                [工具调用]
                - 每轮对话必须进行判断，如果本轮对话得到了新的人物信息，你必须调用一次工具 <tool name=\"save_persona_info\"> 保存本轮得到的关键信息；
                - section 取值限定为：\"角色名称\" |\"基础设定\" | \"外貌特征\" | \"性格与爱好\" | \"背景故事\" | \"说话风格\"；
                - content 该部分对应的优化丰富后的设定文本（字段值），不要带有其他多余内容；
                - 请勿在对话可见内容中展示任何 <tool>…</tool> 调用，仅在内部使用。
                - 工具调用XML示例：
                  <tool name=\"save_persona_info\"><param name=\"section\">基础设定</param><param name=\"content\">（在此仅填入该部分的优化丰富后的设定字段值）</param></tool>
            """.trimIndent()

            val historyPairs = withContext(Dispatchers.Default) {
                chatMessages.map { it.role to it.content }
            }

            val personaJson = withContext(Dispatchers.IO) {
                val map = prefs.sectionsFlow(activeProfile).first()
                JSONObject(map).toString()
            }

            val stream = run {
                val fullPrompt = buildString {
                    append(guidancePrefix)
                    append('\n')
                    append("[当前已保存的人设字典] ")
                    append(personaJson)
                    append('\n')
                    append("[用户输入]")
                    append(input)
                }
                requestFromDefaultService(fullPrompt, historyPairs)
            }

            // 提前插入占位的“生成中…”助手消息
            chatMessages.add(PersonaChatMessage("assistant", "生成中…"))
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

                // 流结束后解析并执行工具（本地）
                withContext(Dispatchers.IO) {
                    processToolInvocations(rawBuffer.toString(), assistantIndex)
                }
            } catch (e: Exception) {
                chatMessages.add(
                    PersonaChatMessage(
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
                    Text("人设卡配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    // 选择不同角色卡
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = activeProfile,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("当前人设卡") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            profiles.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        expanded = false
                                        activeProfile = name
                                        scope.launch { prefs.setActiveProfile(name) }
                                        refreshSnapshot()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ 新建人设卡") },
                                onClick = {
                                    expanded = false
                                    showCreateDialog = true
                                }
                            )
                        }
                    }

                    // 删除当前人设卡（默认卡不可删）
                    if (activeProfile != PersonaCardPreferences.DEFAULT_PROFILE_NAME) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("删除当前人设卡")
                            }
                        }
                    }

                    // 新建人设卡命名弹窗
                    if (showCreateDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreateDialog = false },
                            title = { Text("新建人设卡") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = newProfileName,
                                        onValueChange = { newProfileName = it },
                                        singleLine = true,
                                        label = { Text("人设卡名称") },
                                        placeholder = { Text("例如：Cielo-校园版") }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val name = newProfileName.trim().ifBlank { "新建人设卡" }
                                    showCreateDialog = false
                                    newProfileName = ""
                                    scope.launch {
                                        val newName = prefs.createProfile(name)
                                        profiles = prefs.profilesFlow.first()
                                        activeProfile = newName
                                        prefs.setActiveProfile(newName)
                                        refreshSnapshot()
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
                            title = { Text("删除人设卡") },
                            text = { Text("确定删除当前人设卡吗？此操作不可撤销。") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    scope.launch {
                                        val newActive = prefs.deleteProfile(activeProfile)
                                        profiles = prefs.profilesFlow.first()
                                        activeProfile = newActive
                                        refreshSnapshot()
                                    }
                                }) { Text("删除") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("当前人设卡内容", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val order = PersonaCardPreferences.DefaultSections
                    order.forEach { sec ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            OutlinedTextField(
                                value = editorValues[sec] ?: "",
                                onValueChange = { newValue ->
                                    editorValues[sec] = newValue
                                    scope.launch { prefs.saveSection(activeProfile, sec, newValue) }
                                },
                                label = { Text(sec) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = if (sec == "角色名称") 1 else 6
                            )
                        }
                    }
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏（右上角进入侧栏）
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "当前人设卡配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { scope.launch { drawerState.open() } }) {
                    Text(activeProfile)
                }
            }

            // 聊天列表（保持原有简洁样式与右侧对齐规则）
            val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            val listStateRemembered = listState
            LaunchedEffect(chatMessages.size) {
                if (chatMessages.isNotEmpty()) listStateRemembered.animateScrollToItem(chatMessages.lastIndex)
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