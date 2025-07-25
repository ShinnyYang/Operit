package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.api.speech.SpeechServiceFactory
import com.ai.assistance.operit.api.voice.VoiceServiceFactory
import com.ai.assistance.operit.data.preferences.SpeechServicesPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.foundation.layout.Arrangement
import com.ai.assistance.operit.api.voice.VoiceServiceFactory.VoiceServiceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechServicesSettingsScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { SpeechServicesPreferences(context) }
    
    var showSaveSuccessMessage by remember { mutableStateOf(false) }

    // --- State for TTS Settings ---
    val ttsServiceType by prefs.ttsServiceTypeFlow.collectAsState(initial = VoiceServiceFactory.VoiceServiceType.SIMPLE_TTS)
    val httpConfig by prefs.ttsHttpConfigFlow.collectAsState(initial = SpeechServicesPreferences.BAIDU_TTS_PRESET)

    var ttsServiceTypeInput by remember(ttsServiceType) { mutableStateOf(ttsServiceType) }
    var ttsUrlTemplateInput by remember(httpConfig) { mutableStateOf(httpConfig.urlTemplate) }
    var ttsApiKeyInput by remember(httpConfig) { mutableStateOf(httpConfig.apiKey) }
    var ttsHeadersInput by remember(httpConfig) { mutableStateOf(Json.encodeToString(httpConfig.headers)) }
    var ttsJsonError by remember { mutableStateOf<String?>(null) }

    // --- State for STT Settings ---
    val sttServiceType by prefs.sttServiceTypeFlow.collectAsState(initial = SpeechServiceFactory.SpeechServiceType.SHERPA_NCNN)
    var sttServiceTypeInput by remember(sttServiceType) { mutableStateOf(sttServiceType) }

    // 保存设置的函数
    fun saveSettings() {
        val headersMap = try {
            Json.decodeFromString<Map<String, String>>(ttsHeadersInput)
        } catch (e: Exception) {
            ttsJsonError = "无效的 JSON 格式"
            return
        }
        
        ttsJsonError = null
        
        scope.launch {
            prefs.saveTtsSettings(
                serviceType = ttsServiceTypeInput,
                httpConfig = SpeechServicesPreferences.TtsHttpConfig(
                    urlTemplate = ttsUrlTemplateInput,
                    apiKey = ttsApiKeyInput,
                    headers = headersMap
                )
            )
            prefs.saveSttSettings(
                serviceType = sttServiceTypeInput
            )
            
            VoiceServiceFactory.resetInstance()
            SpeechServiceFactory.resetInstance()
            
            showSaveSuccessMessage = true
            delay(3000)
            showSaveSuccessMessage = false
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- TTS Section ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "文本转语音 (TTS) 设置",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = "配置如何将文本转换为语音输出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "服务类型",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        var ttsDropdownExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = ttsDropdownExpanded,
                            onExpandedChange = { ttsDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = when(ttsServiceTypeInput) {
                                    VoiceServiceFactory.VoiceServiceType.SIMPLE_TTS -> "系统 TTS (简单)"
                                    VoiceServiceFactory.VoiceServiceType.HTTP_TTS -> "HTTP API (远程)"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("TTS 引擎") },
                                trailingIcon = { 
                                    Icon(Icons.Default.ArrowDropDown, "展开下拉菜单")
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = ttsDropdownExpanded,
                                onDismissRequest = { ttsDropdownExpanded = false }
                            ) {
                                VoiceServiceFactory.VoiceServiceType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = when(type) {
                                                    VoiceServiceFactory.VoiceServiceType.SIMPLE_TTS -> "系统 TTS (简单)"
                                                    VoiceServiceFactory.VoiceServiceType.HTTP_TTS -> "HTTP API (远程)"
                                                },
                                                fontWeight = if (ttsServiceTypeInput == type) FontWeight.Medium else FontWeight.Normal
                                            ) 
                                        },
                                        onClick = {
                                            ttsServiceTypeInput = type
                                            ttsDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = ttsServiceTypeInput == VoiceServiceFactory.VoiceServiceType.HTTP_TTS) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "HTTP API 配置",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    OutlinedButton(onClick = { 
                                        val preset = SpeechServicesPreferences.BAIDU_TTS_PRESET
                                        ttsUrlTemplateInput = preset.urlTemplate
                                        ttsApiKeyInput = preset.apiKey
                                        ttsHeadersInput = Json.encodeToString(preset.headers)
                                    }) {
                                        Text("加载百度翻译预设")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = ttsUrlTemplateInput,
                                    onValueChange = { ttsUrlTemplateInput = it },
                                    label = { Text("URL 模板") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    placeholder = { Text("https://example.com/tts?text={text}") },
                                    supportingText = {
                                        Text("使用 {text}, {rate}, {pitch}, {voice} 作为占位符。")
                                    },
                                    minLines = 2
                                )
                                
                                OutlinedTextField(
                                    value = ttsApiKeyInput,
                                    onValueChange = { ttsApiKeyInput = it },
                                    label = { Text("API 密钥 (可选)") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    placeholder = { Text("将作为 Authorization: Bearer <key> 发送") },
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = ttsHeadersInput,
                                    onValueChange = { 
                                        ttsHeadersInput = it
                                        try {
                                            Json.decodeFromString<Map<String, String>>(it)
                                            ttsJsonError = null
                                        } catch (e: Exception) {
                                            if (it.isNotBlank() && it != "{}") {
                                                ttsJsonError = "无效的 JSON 格式"
                                            } else {
                                                ttsJsonError = null
                                            }
                                        }
                                    },
                                    label = { Text("自定义请求头 (JSON)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = ttsJsonError != null,
                                    supportingText = {
                                        if (ttsJsonError != null) {
                                            Text(ttsJsonError!!, color = MaterialTheme.colorScheme.error)
                                        } else {
                                            Text("用于添加额外的 HTTP 请求头。")
                                        }
                                    },
                                    minLines = 2
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // --- STT Section ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicExternalOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "语音转文本 (STT) 设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "配置如何将语音转换为文本",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "服务类型",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        var sttDropdownExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = sttDropdownExpanded,
                            onExpandedChange = { sttDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = when(sttServiceTypeInput) {
                                    SpeechServiceFactory.SpeechServiceType.SHERPA_NCNN -> "Sherpa NCNN (本地)"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("STT 引擎") },
                                trailingIcon = { 
                                    Icon(Icons.Default.ArrowDropDown, "展开下拉菜单")
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = sttDropdownExpanded,
                                onDismissRequest = { sttDropdownExpanded = false }
                            ) {
                                SpeechServiceFactory.SpeechServiceType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = when(type) {
                                                    SpeechServiceFactory.SpeechServiceType.SHERPA_NCNN -> "Sherpa NCNN (本地)"
                                                },
                                                fontWeight = if (sttServiceTypeInput == type) FontWeight.Medium else FontWeight.Normal
                                            ) 
                                        },
                                        onClick = {
                                            sttServiceTypeInput = type
                                            sttDropdownExpanded = false
                                        },
                                        // 未来支持HTTP STT时，可以移除这里的enabled逻辑
                                        enabled = type == SpeechServiceFactory.SpeechServiceType.SHERPA_NCNN
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "目前仅支持本地 Sherpa-NCNN 引擎。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "关于语音服务",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsInfoRow(
                                title = "文本转语音 (TTS)",
                                description = "TTS 服务将文本转换为语音。您可以使用内置的系统 TTS 引擎，或连接到远程 HTTP API。"
                            )
                            
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            
                            SettingsInfoRow(
                                title = "语音转文本 (STT)",
                                description = "STT 服务将语音转换为文本。目前仅支持内置的 Sherpa NCNN 引擎，该引擎在本地设备上运行，不需要网络连接。"
                            )
                        }
                    }
                }
                
                // 显示保存成功的消息
                AnimatedVisibility(visible = showSaveSuccessMessage) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "设置已保存",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // 保存按钮
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { saveSettings() },
                    enabled = ttsJsonError == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存设置")
                }
                
                // 底部空间
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsInfoRow(title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
    }
} 