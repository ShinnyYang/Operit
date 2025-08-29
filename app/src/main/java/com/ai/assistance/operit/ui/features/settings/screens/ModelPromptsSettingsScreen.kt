package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ai.assistance.operit.ui.components.CustomScaffold
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.CharacterCard
import com.ai.assistance.operit.data.model.PromptTag
import com.ai.assistance.operit.data.model.TagType
import com.ai.assistance.operit.data.preferences.CharacterCardManager
import com.ai.assistance.operit.data.preferences.PromptTagManager
import com.ai.assistance.operit.data.preferences.PromptPreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ModelPromptsSettingsScreen(
        onBackPressed: () -> Unit = {},
        onNavigateToMarket: () -> Unit = {},
        onNavigateToPersonaGeneration: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 管理器
    val characterCardManager = remember { CharacterCardManager.getInstance(context) }
    val promptTagManager = remember { PromptTagManager.getInstance(context) }
    val oldPromptPreferencesManager = remember { PromptPreferencesManager(context) }
    
    // 状态
    var currentTab by remember { mutableStateOf(0) } // 0: 角色卡, 1: 标签, 2: 旧配置
    var showSaveSuccessMessage by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // 角色卡相关状态
    val characterCardList by characterCardManager.characterCardListFlow.collectAsState(initial = emptyList())
    var showAddCharacterCardDialog by remember { mutableStateOf(false) }
    var showEditCharacterCardDialog by remember { mutableStateOf(false) }
    var editingCharacterCard by remember { mutableStateOf<CharacterCard?>(null) }
    
    // 酒馆角色卡导入相关状态
    var showImportSuccessMessage by remember { mutableStateOf(false) }
    var showImportErrorMessage by remember { mutableStateOf(false) }
    var importErrorMessage by remember { mutableStateOf("") }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonContent = reader.readText()
                    reader.close()
                    
                    val result = characterCardManager.createCharacterCardFromTavernJson(jsonContent)
                    result.onSuccess { newCardId ->
                        showImportSuccessMessage = true
                        refreshTrigger++
                    }.onFailure { exception ->
                        importErrorMessage = exception.message ?: "未知错误"
                        showImportErrorMessage = true
                    }
                } catch (e: Exception) {
                    importErrorMessage = "读取文件失败: ${e.message}"
                    showImportErrorMessage = true
                }
            }
        }
    }
    
    // 标签相关状态
    val tagList by promptTagManager.tagListFlow.collectAsState(initial = emptyList())
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showEditTagDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<PromptTag?>(null) }

    // 旧配置相关状态
    val oldProfileList by oldPromptPreferencesManager.profileListFlow.collectAsState(initial = emptyList())
    var showOldConfigDialog by remember { mutableStateOf(false) }
    var selectedOldProfileId by remember { mutableStateOf("") }
    
    // 初始化
    LaunchedEffect(Unit) {
        characterCardManager.initializeIfNeeded()
    }
    
    // 获取所有标签
    var allTags by remember { mutableStateOf(emptyList<PromptTag>()) }
    LaunchedEffect(tagList) {
        scope.launch {
            val tags = promptTagManager.getAllTags()
            allTags = tags
        }
    }

    // 获取所有角色卡
    var allCharacterCards by remember { mutableStateOf(emptyList<CharacterCard>()) }
    LaunchedEffect(characterCardList, refreshTrigger) {
        scope.launch {
            val cards = characterCardManager.getAllCharacterCards()
            allCharacterCards = cards
        }
    }
    
    // 保存角色卡
    fun saveCharacterCard() {
        editingCharacterCard?.let { card ->
            scope.launch {
                if (card.id.isEmpty()) {
                    // 新建
                    characterCardManager.createCharacterCard(card)
                } else {
                    // 更新
                    characterCardManager.updateCharacterCard(card)
                }
                showAddCharacterCardDialog = false
                showEditCharacterCardDialog = false
                editingCharacterCard = null
                showSaveSuccessMessage = true
                refreshTrigger++
            }
        }
    }

    // 保存标签
    fun saveTag() {
        editingTag?.let { tag ->
            scope.launch {
                if (tag.id.isEmpty()) {
                    // 新建
                    promptTagManager.createPromptTag(
                        name = tag.name,
                        description = tag.description,
                        promptContent = tag.promptContent,
                        tagType = tag.tagType
                    )
                } else {
                    // 更新
                    promptTagManager.updatePromptTag(
                        id = tag.id,
                        name = tag.name,
                        description = tag.description,
                        promptContent = tag.promptContent
                    )
                }
                showEditTagDialog = false
                editingTag = null
                showSaveSuccessMessage = true
            }
        }
    }

    // 删除角色卡
    fun deleteCharacterCard(id: String) {
        scope.launch {
            characterCardManager.deleteCharacterCard(id)
        }
    }
    
    // 删除标签
    fun deleteTag(id: String) {
        scope.launch {
            promptTagManager.deletePromptTag(id)
            }
        }
    
    CustomScaffold() { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标签栏
                TabRow(selectedTabIndex = currentTab) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("角色卡", fontSize = 14.sp)
                        }
                    }
                    Tab(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                            Icon(
                                Icons.Default.Label,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("标签", fontSize = 14.sp)
                        }
                    }
                    Tab(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("旧配置", fontSize = 14.sp)
                        }
                    }
                                }

                // 内容区域
                when (currentTab) {
                    0 -> CharacterCardTab(
                        characterCards = allCharacterCards,
                        allTags = allTags,
                        onAddCharacterCard = {
                            editingCharacterCard = CharacterCard(
                                id = "",
                                name = "",
                                description = "",
                                characterSetting = "",
                                otherContent = "",
                                attachedTagIds = emptyList(),
                                advancedCustomPrompt = "",
                                marks = ""
                            )
                            showAddCharacterCardDialog = true
                        },
                        onEditCharacterCard = { card ->
                            editingCharacterCard = card.copy()
                            showEditCharacterCardDialog = true
                        },
                        onDeleteCharacterCard = { deleteCharacterCard(it) },
                        onNavigateToPersonaGeneration = onNavigateToPersonaGeneration,
                        onImportTavernCard = {
                            filePickerLauncher.launch("application/json")
                        }
                    )
                    1 -> TagTab(
                        tags = allTags,
                        onAddTag = {
                            editingTag = PromptTag(
                                id = "",
                                name = "",
                                description = "",
                                promptContent = "",
                                tagType = TagType.CUSTOM
                            )
                            showAddTagDialog = true
                        },
                        onEditTag = { tag ->
                            editingTag = tag.copy()
                            showEditTagDialog = true
                        },
                        onDeleteTag = { deleteTag(it) },
                        onNavigateToMarket = onNavigateToMarket
                    )
                    2 -> OldConfigTab(
                        profileList = oldProfileList,
                        promptPreferencesManager = oldPromptPreferencesManager,
                        onShowOldConfig = { profileId ->
                            selectedOldProfileId = profileId
                            showOldConfigDialog = true
                        }
                    )
                }
            }
            
                        // 成功保存消息
            if (showSaveSuccessMessage) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    showSaveSuccessMessage = false
                }
                
                Card(
                            modifier = Modifier
                                .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                        ) {
                            Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                            imageVector = Icons.Default.Check,
                                        contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                                    )
                        Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                            text = "保存成功",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        
            // 导入成功消息
            if (showImportSuccessMessage) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showImportSuccessMessage = false
                }
                
                Card(
                            modifier = Modifier
                                .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                        ) {
                            Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.tavern_card_import_success),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // 导入失败消息
            if (showImportErrorMessage) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    showImportErrorMessage = false
                }
                
                Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                            ) {
                                Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                                    Text(
                                text = stringResource(R.string.tavern_card_import_failed),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (importErrorMessage.isNotBlank()) {
                                Text(
                                    text = importErrorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
    
    // 新建角色卡对话框
    if (showAddCharacterCardDialog) {
        CharacterCardDialog(
            characterCard = editingCharacterCard ?: CharacterCard(
                id = "",
                name = "",
                description = "",
                characterSetting = "",
                otherContent = "",
                attachedTagIds = emptyList(),
                advancedCustomPrompt = ""
            ),
            allTags = allTags,
            onDismiss = {
                showAddCharacterCardDialog = false
                editingCharacterCard = null
            },
            onSave = {
                editingCharacterCard = it
                saveCharacterCard()
            }
        )
    }
    
    // 编辑角色卡对话框
    if (showEditCharacterCardDialog) {
        CharacterCardDialog(
            characterCard = editingCharacterCard ?: CharacterCard(
                id = "",
                name = "",
                description = "",
                characterSetting = "",
                otherContent = "",
                attachedTagIds = emptyList(),
                advancedCustomPrompt = ""
            ),
            allTags = allTags,
            onDismiss = {
                showEditCharacterCardDialog = false
                editingCharacterCard = null
            },
            onSave = {
                editingCharacterCard = it
                saveCharacterCard()
                                }
        )
                        }
                        
    // 新建标签对话框
    if (showAddTagDialog) {
        TagDialog(
            tag = editingTag ?: PromptTag(
                id = "",
                name = "",
                description = "",
                promptContent = "",
                tagType = TagType.CUSTOM
            ),
            onDismiss = {
                showAddTagDialog = false
                editingTag = null
            },
            onSave = {
                editingTag = it
                saveTag()
            }
        )
    }
    
    // 编辑标签对话框
    if (showEditTagDialog) {
        TagDialog(
            tag = editingTag ?: PromptTag(
                id = "",
                name = "",
                description = "",
                promptContent = "",
                tagType = TagType.CUSTOM
            ),
            onDismiss = {
                showEditTagDialog = false
                editingTag = null
            },
            onSave = {
                editingTag = it
                saveTag()
            }
        )
    }
    
    // 旧配置查看对话框
    if (showOldConfigDialog) {
        OldConfigDialog(
            profileId = selectedOldProfileId,
            promptPreferencesManager = oldPromptPreferencesManager,
            onDismiss = {
                showOldConfigDialog = false
                selectedOldProfileId = ""
                                        }
        )
    }
    
    // 成功保存消息
}

// 角色卡标签页
@Composable
fun CharacterCardTab(
    characterCards: List<CharacterCard>,
    allTags: List<PromptTag>,
    onAddCharacterCard: () -> Unit,
    onEditCharacterCard: (CharacterCard) -> Unit,
    onDeleteCharacterCard: (String) -> Unit,
    onNavigateToPersonaGeneration: () -> Unit,
    onImportTavernCard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // 标题和按钮区域
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 第一行：标题和新建按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                    Text(
                        text = "角色卡管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onAddCharacterCard,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建", fontSize = 13.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第二行：功能按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToPersonaGeneration,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI创作", fontSize = 12.sp)
                    }
                    
                    OutlinedButton(
                        onClick = onImportTavernCard,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.import_tavern_card), fontSize = 12.sp)
                            }
                        }
                    }
                }

        // 角色卡列表
        items(characterCards) { characterCard ->
            CharacterCardItem(
                characterCard = characterCard,
                allTags = allTags,
                onEdit = { onEditCharacterCard(characterCard) },
                onDelete = { onDeleteCharacterCard(characterCard.id) }
            )
        }
    }
}

// 角色卡项目
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCardItem(
    characterCard: CharacterCard,
    allTags: List<PromptTag>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
                        ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题和操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                        text = characterCard.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                    if (characterCard.description.isNotBlank()) {
                                Text(
                            text = characterCard.description,
                            style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp))
                    }
                    
                    if (!characterCard.isDefault) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            // 角色设定预览
            if (characterCard.characterSetting.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                    text = "角色设定：${characterCard.characterSetting.take(40)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            
            // 其他内容预览
            if (characterCard.otherContent.isNotBlank()) {
                                    Text(
                    text = "其他内容：${characterCard.otherContent.take(40)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            
            // 附着的标签
            if (characterCard.attachedTagIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    characterCard.attachedTagIds.take(3).forEach { tagId ->
                        val tag = allTags.find { it.id == tagId }
                        tag?.let {
                            AssistChip(
                                onClick = { },
                                label = { Text(it.name, fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.height(24.dp)
                                        )
                        }
                    }
                    if (characterCard.attachedTagIds.size > 3) {
                                        Text(
                            text = "+${characterCard.attachedTagIds.size - 3}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            // 高级自定义提示词预览
            if (characterCard.advancedCustomPrompt.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                    text = "高级自定义：${characterCard.advancedCustomPrompt.take(40)}...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 标签标签页
@Composable
fun TagTab(
    tags: List<PromptTag>,
    onAddTag: () -> Unit,
    onEditTag: (PromptTag) -> Unit,
    onDeleteTag: (String) -> Unit,
    onNavigateToMarket: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // 标题和按钮区域
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 第一行：标题和新建按钮
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                    Text(
                        text = "标签管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onAddTag,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建标签", fontSize = 13.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第二行：标签市场按钮
                OutlinedButton(
                    onClick = onNavigateToMarket,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("标签市场", fontSize = 12.sp)
                }
            }
        }
        
        // 系统标签
        val systemTags = tags.filter { it.isSystemTag }
        if (systemTags.isNotEmpty()) {
            item {
                                                    Text(
                    text = "系统标签",
                    style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            items(systemTags) { tag ->
                TagItem(
                    tag = tag,
                    onEdit = { onEditTag(tag) },
                    onDelete = { onDeleteTag(tag.id) }
                )
            }
        }
        
        // 自定义标签
        val customTags = tags.filter { !it.isSystemTag }
        if (customTags.isNotEmpty()) {
            item {
                                Text(
                    text = "自定义标签",
                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            items(customTags) { tag ->
                TagItem(
                    tag = tag,
                    onEdit = { onEditTag(tag) },
                    onDelete = { onDeleteTag(tag.id) }
                )
            }
        }
    }
}

// 标签项目
@Composable
fun TagItem(
    tag: PromptTag,
    onEdit: () -> Unit,
    onDelete: () -> Unit
                                        ) {
                                            Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tag.isSystemTag) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
                                                        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                    text = tag.name,
                                            style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                                        )
                if (tag.description.isNotBlank()) {
                                                        Text(
                        text = tag.description,
                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                if (tag.promptContent.isNotBlank()) {
                    Text(
                        text = "内容：${tag.promptContent.take(50)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                                        )
                                    }
                                }

                                    Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp))
                }
                
                if (!tag.isSystemTag) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                                                            }
                                    }
                                }
                            }
                        }
                    }

// 旧配置标签页
@Composable
fun OldConfigTab(
    profileList: List<String>,
    promptPreferencesManager: PromptPreferencesManager,
    onShowOldConfig: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
                                                    Text(
                text = "旧版提示词配置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
                                                        Text(
                text = "这些是旧版系统的提示词配置，您可以查看和复制内容到新的角色卡系统中。",
                style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp
            )
        }
        
        items(profileList) { profileId ->
            OldConfigItem(
                profileId = profileId,
                promptPreferencesManager = promptPreferencesManager,
                onShowConfig = { onShowOldConfig(profileId) }
            )
        }
    }
}

// 旧配置项目
@Composable
fun OldConfigItem(
    profileId: String,
    promptPreferencesManager: PromptPreferencesManager,
    onShowConfig: () -> Unit
) {
    val profile by promptPreferencesManager.getPromptProfileFlow(profileId).collectAsState(
        initial = null
    )
    
    profile?.let { promptProfile ->
        Card(
                                            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp)
        ) {
                        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowConfig)
                    .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = promptProfile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (promptProfile.introPrompt.isNotBlank()) {
                        Text(
                            text = "引导词：${promptProfile.introPrompt.take(50)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                
                            Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "查看详情",
                                tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                            )
            }
        }
    }
}

// 角色卡对话框
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCardDialog(
    characterCard: CharacterCard,
    allTags: List<PromptTag>,
    onDismiss: () -> Unit,
    onSave: (CharacterCard) -> Unit
) {
    var name by remember(characterCard.id) { mutableStateOf(characterCard.name) }
    var description by remember(characterCard.id) { mutableStateOf(characterCard.description) }
    var characterSetting by remember(characterCard.id) { mutableStateOf(characterCard.characterSetting) }
    var otherContent by remember(characterCard.id) { mutableStateOf(characterCard.otherContent) }
    var attachedTagIds by remember(characterCard.id) { mutableStateOf(characterCard.attachedTagIds) }
    var advancedCustomPrompt by remember(characterCard.id) { mutableStateOf(characterCard.advancedCustomPrompt) }
    var marks by remember(characterCard.id) { mutableStateOf(characterCard.marks) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
                            Text(
                if (characterCard.id.isEmpty()) "新建角色卡" else "编辑角色卡",
                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色卡名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = characterSetting,
                    onValueChange = { characterSetting = it },
                    label = { Text("角色设定（引导词）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                OutlinedTextField(
                    value = otherContent,
                    onValueChange = { otherContent = it },
                    label = { Text("其他内容（引导词）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                // 标签选择
                        Text(
                    text = "选择标签",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 过滤掉系统标签，只显示自定义标签
                    allTags.filter { !it.isSystemTag }.forEach { tag ->
                        val isSelected = attachedTagIds.contains(tag.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                attachedTagIds = if (isSelected) {
                                    attachedTagIds.filter { it != tag.id }
                                } else {
                                    attachedTagIds + tag.id
                                }
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
                
                // 显示系统标签信息（只读）
                if (allTags.any { it.isSystemTag }) {
                    Text(
                        text = "系统标签（自动加入）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.filter { it.isSystemTag }.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { /* 系统标签不可点击 */ },
                                enabled = false,
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = advancedCustomPrompt,
                    onValueChange = { advancedCustomPrompt = it },
                    label = { Text("高级自定义（引导词）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                OutlinedTextField(
                    value = marks,
                    onValueChange = { marks = it },
                    label = { Text("备注信息（不会被拼接到提示词中）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("可以记录角色卡来源、作者信息等") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        characterCard.copy(
                            name = name,
                            description = description,
                            characterSetting = characterSetting,
                            otherContent = otherContent,
                            attachedTagIds = attachedTagIds,
                            advancedCustomPrompt = advancedCustomPrompt,
                            marks = marks
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
                    }
                }
    )
    }
    
// 标签对话框
@Composable
fun TagDialog(
    tag: PromptTag,
    onDismiss: () -> Unit,
    onSave: (PromptTag) -> Unit
) {
    var name by remember { mutableStateOf(tag.name) }
    var description by remember { mutableStateOf(tag.description) }
    var promptContent by remember { mutableStateOf(tag.promptContent) }
    var tagType by remember { mutableStateOf(tag.tagType) }
    
        AlertDialog(
        onDismissRequest = onDismiss,
            title = {
                Text(
                if (tag.id.isEmpty()) "新建标签" else "编辑标签",
                style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("标签名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!tag.isSystemTag) {
                    // 只有自定义标签可以选择类型
                    Text(
                        text = "标签类型",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TagType.values().filter { it != TagType.CUSTOM }.forEach { type ->
                            FilterChip(
                                selected = tagType == type,
                                onClick = { tagType = type },
                                label = { Text(type.name) }
                            )
                        }
                    }
                }
                
                    OutlinedTextField(
                    value = promptContent,
                    onValueChange = { promptContent = it },
                    label = { Text("提示词内容") },
                        modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                    onSave(
                        tag.copy(
                            name = name,
                            description = description,
                            promptContent = promptContent,
                            tagType = tagType
                        )
                    )
                }
            ) {
                Text("保存")
            }
            },
            dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 旧配置查看对话框
@Composable
fun OldConfigDialog(
    profileId: String,
    promptPreferencesManager: PromptPreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profile by promptPreferencesManager.getPromptProfileFlow(profileId).collectAsState(
        initial = null
    )
    
    // 复制到剪贴板的函数
    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        // 这里可以添加一个 Toast 提示，但为了简洁我们先不添加
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "旧版配置详情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            profile?.let { promptProfile ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "配置名称：${promptProfile.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (promptProfile.introPrompt.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "引导词：",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { copyToClipboard(promptProfile.introPrompt, "引导词") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制引导词",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        SelectionContainer {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = promptProfile.introPrompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    
                    if (promptProfile.tonePrompt.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
        Text(
                                text = "语气词：",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { copyToClipboard(promptProfile.tonePrompt, "语气词") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制语气词",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        SelectionContainer {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = promptProfile.tonePrompt,
            style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // 复制全部内容按钮
                    val allContent = buildString {
                        append("配置名称：${promptProfile.name}\n\n")
                        if (promptProfile.introPrompt.isNotBlank()) {
                            append("引导词：\n${promptProfile.introPrompt}\n\n")
                        }
                        if (promptProfile.tonePrompt.isNotBlank()) {
                            append("语气词：\n${promptProfile.tonePrompt}")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { copyToClipboard(allContent, "旧版配置") },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制全部内容", fontSize = 14.sp)
                    }
        
        Text(
                        text = "您可以复制这些内容到新的角色卡系统中使用。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
    }
        }
    )
}
