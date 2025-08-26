package com.ai.assistance.operit.ui.features.toolbox.screens.texttospeech

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ai.assistance.operit.api.voice.VoiceServiceFactory
import kotlinx.coroutines.launch

/** 文本转语音演示屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechScreen(navController: NavController) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val scrollState = rememberScrollState()

        // 获取VoiceService实例
        val voiceService = remember { VoiceServiceFactory.getInstance(context) }

        // 状态变量
        var inputText by remember { mutableStateOf("") }
        var speechRate by remember { mutableStateOf(1.0f) }
        var speechPitch by remember { mutableStateOf(1.0f) }
        var isInitialized by remember { mutableStateOf(false) }
        var isSpeaking by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        // 监听语音服务状态
        LaunchedEffect(Unit) {
                // 初始化语音服务
                coroutineScope.launch {
                        try {
                                isInitialized = voiceService.initialize()
                                if (!isInitialized) {
                                        error = "初始化语音引擎失败"
                                }
                        } catch (e: Exception) {
                                error = "初始化语音引擎错误: ${e.message}"
                        }
                }

                // 监听发言状态
                voiceService.speakingStateFlow.collect { speaking -> isSpeaking = speaking }
        }

        // 播放文本
        fun speakText() {
                if (inputText.isBlank()) {
                        error = "请输入要转换为语音的文本"
                        return
                }

                coroutineScope.launch {
                        try {
                                val success =
                                        voiceService.speak(inputText, true, speechRate, speechPitch)
                                if (!success) {
                                        error = "播放文本失败"
                                }
                        } catch (e: Exception) {
                                error = "播放文本错误: ${e.message}"
                        }
                }
        }

        // 停止播放
        fun stopSpeaking() {
                coroutineScope.launch {
                        try {
                                voiceService.stop()
                        } catch (e: Exception) {
                                error = "停止播放错误: ${e.message}"
                        }
                }
        }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .verticalScroll(scrollState)
                                .padding(16.dp)
        ) {
                // 标题
                Text(
                        text = "文本转语音演示",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                )

                // 文本输入卡片
                Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                        text = "输入文本",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                        placeholder = { Text("请输入要转换为语音的文本") },
                                        colors =
                                                TextFieldDefaults.outlinedTextFieldColors(
                                                        focusedBorderColor =
                                                                MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor =
                                                                MaterialTheme.colorScheme.outline
                                                ),
                                        maxLines = 5
                                )
                        }
                }

                // 语音设置卡片
                Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                        text = "语音设置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 语速调节
                                Text(
                                        text = "语速: ${speechRate}x",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                )

                                Slider(
                                        value = speechRate,
                                        onValueChange = { speechRate = it },
                                        valueRange = 0.5f..2.0f,
                                        steps = 5,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 音调调节
                                Text(
                                        text = "音调: ${speechPitch}x",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                )

                                Slider(
                                        value = speechPitch,
                                        onValueChange = { speechPitch = it },
                                        valueRange = 0.5f..2.0f,
                                        steps = 5,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                )
                        }
                }

                // 操作按钮
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Button(
                                onClick = { speakText() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = isInitialized && !isSpeaking && inputText.isNotBlank(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "播放",
                                        modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("播放语音", style = MaterialTheme.typography.titleMedium)
                        }

                        Button(
                                onClick = { stopSpeaking() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = isSpeaking,
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "停止",
                                        modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("停止播放", style = MaterialTheme.typography.titleMedium)
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 状态指示器
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.secondaryContainer
                                ),
                        shape = RoundedCornerShape(8.dp)
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                imageVector =
                                                        if (isInitialized) Icons.Default.CheckCircle
                                                        else Icons.Default.Error,
                                                contentDescription = null,
                                                tint =
                                                        if (isInitialized) Color(0xFF4CAF50)
                                                        else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                                text =
                                                        if (isInitialized) "语音引擎已初始化"
                                                        else "语音引擎未初始化",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                imageVector =
                                                        if (isSpeaking) Icons.Default.VolumeUp
                                                        else Icons.Default.VolumeOff,
                                                contentDescription = null,
                                                tint =
                                                        if (isSpeaking) Color(0xFF2196F3)
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onSecondaryContainer
                                        )
                                        Text(
                                                text = if (isSpeaking) "正在播放中..." else "未播放",
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                }
                        }
                }

                // 错误提示
                if (error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                                text = error ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                }
                        }
                }

                // 使用说明
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                )
                ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                        text = "使用说明",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text =
                                                "1. 在输入框中输入要转换为语音的文本\n" +
                                                        "2. 调整语速和音调设置\n" +
                                                        "3. 点击「播放语音」按钮开始播放\n" +
                                                        "4. 点击「停止播放」按钮停止播放",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text = "注意：首次使用时需要在系统设置中启用无障碍服务",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                )
                        }
                }
        }
}
