package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.ai.assistance.operit.data.preferences.ApiPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaifuModeSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val apiPreferences = remember { ApiPreferences(context) }
    val scope = rememberCoroutineScope()
    
    // 状态
    val isWaifuModeEnabled = apiPreferences.enableWaifuModeFlow.collectAsState(initial = false).value
    val charDelay = apiPreferences.waifuCharDelayFlow.collectAsState(initial = 500).value
    val removePunctuation = apiPreferences.waifuRemovePunctuationFlow.collectAsState(initial = false).value
    val disableActions = apiPreferences.waifuDisableActionsFlow.collectAsState(initial = false).value
    val enableEmoticons = apiPreferences.waifuEnableEmoticonsFlow.collectAsState(initial = false).value
    val enableSelfie = apiPreferences.waifuEnableSelfieFlow.collectAsState(initial = false).value
    val selfiePrompt = apiPreferences.waifuSelfiePromptFlow.collectAsState(initial = "").value
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    // 显示保存成功的提示
    LaunchedEffect(showSaveSuccess) {
        if (showSaveSuccess) {
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
    }

    Scaffold(

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 页面标题和说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Waifu模式",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "启用后，AI的回复将按标点分割逐句发送，营造更自然的对话体验。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Waifu模式开关
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "启用Waifu模式",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI回复将按标点分割发送",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        Switch(
                            checked = isWaifuModeEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    apiPreferences.saveEnableWaifuMode(enabled)
                                    showSaveSuccess = true
                                }
                            }
                        )
                    }
                }
            }

            // 延迟时间配置（仅在waifu模式启用时显示）
            if (isWaifuModeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "打字速度设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "控制每字符的延迟时间，模拟真人打字节奏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 显示当前速度
                        val charsPerSecond = if (charDelay > 0) 1000f / charDelay else 0f
                        Text(
                            text = "当前速度：每秒 %.1f 字符".format(charsPerSecond),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 延迟时间滑块
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "快",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(30.dp)
                            )
                            Slider(
                                value = charDelay.toFloat(),
                                onValueChange = { newDelay ->
                                    scope.launch {
                                        apiPreferences.saveWaifuCharDelay(newDelay.toInt())
                                        showSaveSuccess = true
                                    }
                                },
                                valueRange = 200f..1000f, // 200ms-1000ms per character (5-1字符/秒)
                                steps = 39, // 20ms步长
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "慢",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(30.dp)
                            )
                        }
                        
                        Text(
                            text = "当前：${charDelay}ms/字符",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            // 标点符号配置（仅在waifu模式启用时显示）
            if (isWaifuModeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "移除句末标点符号",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "移除句子末尾的句号、问号、感叹号",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            Switch(
                                checked = removePunctuation,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        apiPreferences.saveWaifuRemovePunctuation(enabled)
                                        showSaveSuccess = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 动作表情配置（仅在waifu模式启用时显示）
            if (isWaifuModeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "禁止动作表情",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "禁止AI使用（动作表情）格式",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "注意：此项设置仅由额外提示词控制，只能减少频率并不能保证百分百禁止，建议在提示词配置中进行针对性说明",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                            
                            Switch(
                                checked = disableActions,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        apiPreferences.saveWaifuDisableActions(enabled)
                                        showSaveSuccess = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 表情包配置（仅在waifu模式启用时显示）
            if (isWaifuModeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "启用表情包",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "在对话中插入情绪状态标签，增强表达效果",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "可用表情：哭泣、喜欢你、开心、惊讶、想你、无语、生气、疑惑、难过",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                            
                            Switch(
                                checked = enableEmoticons,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        apiPreferences.saveWaifuEnableEmoticons(enabled)
                                        showSaveSuccess = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 自拍功能配置（仅在waifu模式启用时显示）
            if (isWaifuModeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "启用自拍功能",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "允许AI生成角色自拍图片，需要配置外貌提示词",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            Switch(
                                checked = enableSelfie,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        apiPreferences.saveWaifuEnableSelfie(enabled)
                                        showSaveSuccess = true
                                    }
                                }
                            )
                        }
                        
                        // 如果启用了自拍功能，显示外貌提示词输入框
                        if (enableSelfie) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "外貌提示词",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "描述角色的外貌特征，用于生成自拍图片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var promptText by remember { mutableStateOf(selfiePrompt) }
                            
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { newText ->
                                    promptText = newText
                                    scope.launch {
                                        apiPreferences.saveWaifuSelfiePrompt(newText)
                                        showSaveSuccess = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("外貌描述") },
                                placeholder = { Text("例如：long hair, purple eyes, sweater vest...") },
                                minLines = 3,
                                maxLines = 6,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "提示：使用英文关键词描述外貌特征，多个关键词用逗号分隔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 功能说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "功能说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 普通模式：AI回复一次性全部发送\n" +
                                "• Waifu模式：AI回复按句子分割，每句独立气泡\n" +
                                "• 智能延迟：根据句子长度计算延迟时间\n" +
                                "• 自动清理：移除状态标签，只显示纯文本\n" +
                                "• 可选功能：移除句末标点、禁止动作表情、启用表情包\n" +
                                "• 适用场景：模拟更自然的对话节奏\n" +
                                "• 注意：此模式对工具调用的适配可能不佳",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 保存成功提示
            if (showSaveSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "设置已保存",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
} 