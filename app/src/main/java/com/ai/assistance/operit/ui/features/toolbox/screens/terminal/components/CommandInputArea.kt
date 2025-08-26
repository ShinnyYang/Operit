package com.ai.assistance.operit.ui.features.toolbox.screens.terminal.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.model.TerminalLine
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.model.TerminalSession
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.utils.TerminalColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 命令输入区域组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandInputArea(
    context: Context,
    session: TerminalSession,
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    fontSize: Int,
    scope: CoroutineScope,
    focusRequester: FocusRequester,
    onCommandProcessed: () -> Unit,
    setCurrentExecutionId: (Int) -> Unit,
    setInteractivePrompt: (String) -> Unit,
    setInteractiveInputText: (String) -> Unit,
    setShowInputDialog: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(TerminalColors.ParrotBgLight)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分段样式的命令提示符
        Row {
            Text(
                text = "[",
                color = TerminalColors.ParrotAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 用户名部分
            Text(
                text = if (session.currentUser == "root") "root" else "user",
                color = if (session.currentUser == "root") TerminalColors.ParrotRed else TerminalColors.ParrotYellow,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
            )
            
            
            Text(
                text = "]",
                color = TerminalColors.ParrotAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 获取当前工作目录名称
        val currentDirName = remember(session.workingDirectory) {
            val path = session.workingDirectory
            when {
                // Termux 主目录显示为 ~ 
                path == "/data/data/com.termux/files/home" -> "~"
                
                // 根目录
                path == "/" -> "/"
                
                // Android 存储目录显示为更友好的名称
                path.startsWith("/storage/emulated/0") -> {
                    val relativePath = path.removePrefix("/storage/emulated/0")
                    if (relativePath.isEmpty()) "sdcard" else "sdcard${relativePath}"
                }
                
                // 完整 Termux 路径
                path.startsWith("/data/data/com.termux/files/") -> {
                    val relativePath = path.removePrefix("/data/data/com.termux/files/")
                    if (relativePath.isEmpty()) "termux" else relativePath
                }
                
                // 其他路径显示最后一个目录名
                else -> {
                    val parts = path.split("/").filter { it.isNotEmpty() }
                    parts.lastOrNull() ?: "/"
                }
            }
        }
        
        // 完整路径提示
        val fullPathTooltip = remember(session.workingDirectory) {
            "当前工作目录: ${session.workingDirectory}"
        }
        
        // 路径部分 - 使用会话的实际工作目录
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TerminalColors.ParrotBg.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    // 长按显示完整路径提示
                    detectTapGestures(
                        onLongPress = {
                            // 显示完整路径的Toast提示
                            android.widget.Toast.makeText(
                                context, 
                                fullPathTooltip, 
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
        ) {
            Text(
                text = " $currentDirName ",
                color = TerminalColors.ParrotPurple,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        
        // 命令提示符箭头
        Text(
            text = if (session.currentUser == "root") "# " else "$ ",
            color = TerminalColors.ParrotAccent,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 历史命令浏览部分
        Box {
            var showHistoryDropdown by remember { mutableStateOf(false) }
        
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(vertical = 0.dp, horizontal = 2.dp)
                    // 添加向上滑动手势来显示历史命令
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // 如果是向上拖动开始，显示历史命令
                                if (offset.y < 0) {
                                    showHistoryDropdown = true
                                }
                            },
                            onDragEnd = {
                                // 不在这里关闭，让用户点击选择
                            },
                            onDragCancel = {
                                // 不处理
                            },
                            onDrag = { change, dragAmount ->
                                // 不处理
                            }
                        )
                    }
                    .onKeyEvent { keyEvent ->
                        // 处理上下方向键浏览命令历史
                        when {
                            keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyUp -> {
                                session.getPreviousCommand()?.let {
                                    onInputTextChange(TextFieldValue(it))
                                }
                                true
                            }
                            keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyUp -> {
                                session.getNextCommand()?.let {
                                    onInputTextChange(TextFieldValue(it))
                                }
                                true
                            }
                            keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyUp -> {
                                val command = inputText.text.trim()
                                if (command.isNotEmpty()) {
                                    scope.launch {
                                        handleCommand(
                                            context = context,
                                            command = command,
                                            session = session,
                                            scope = scope,
                                            onCommandProcessed = onCommandProcessed,
                                            setCurrentExecutionId = setCurrentExecutionId,
                                            setInteractivePrompt = setInteractivePrompt,
                                            setInteractiveInputText = setInteractiveInputText,
                                            setShowInputDialog = setShowInputDialog
                                        )
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val command = inputText.text.trim()
                        if (command.isNotEmpty()) {
                            scope.launch {
                                handleCommand(
                                    context = context,
                                    command = command,
                                    session = session,
                                    scope = scope,
                                    onCommandProcessed = onCommandProcessed,
                                    setCurrentExecutionId = setCurrentExecutionId,
                                    setInteractivePrompt = setInteractivePrompt,
                                    setInteractiveInputText = setInteractiveInputText,
                                    setShowInputDialog = setShowInputDialog
                                )
                            }
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TerminalColors.ParrotGreen,
                    unfocusedTextColor = TerminalColors.ParrotGreen,
                    cursorColor = TerminalColors.ParrotAccent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = TerminalColors.ParrotBgLight,
                    unfocusedContainerColor = TerminalColors.ParrotBgLight
                ),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                trailingIcon = {
                    // 添加历史命令按钮，点击后显示历史命令下拉菜单
                    IconButton(
                        onClick = { showHistoryDropdown = !showHistoryDropdown }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "历史命令",
                            tint = TerminalColors.ParrotAccent
                        )
                    }
                }
            )
            
            // 历史命令下拉菜单
            DropdownMenu(
                expanded = showHistoryDropdown,
                onDismissRequest = { showHistoryDropdown = false },
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TerminalColors.ParrotBgLight.copy(alpha = 0.95f),
                                TerminalColors.ParrotBg.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = TerminalColors.ParrotAccent.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // 获取最多10条历史命令
                val commandHistory = session.getUserInputHistory().takeLast(10).reversed()
                
                if (commandHistory.isEmpty()) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "没有历史命令",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = (fontSize - 2).coerceAtLeast(10).sp
                            ) 
                        },
                        onClick = { showHistoryDropdown = false },
                        enabled = false
                    )
                } else {
                    // 显示历史命令列表
                    commandHistory.forEachIndexed { index, command ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = command,
                                    color = TerminalColors.ParrotGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (fontSize - 1).coerceAtLeast(10).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onInputTextChange(TextFieldValue(command))
                                showHistoryDropdown = false
                            },
                            leadingIcon = {
                                Text(
                                    text = "${commandHistory.size - index}",
                                    color = TerminalColors.ParrotAccent,
                                    fontSize = (fontSize - 2).coerceAtLeast(8).sp
                                )
                            }
                        )
                    }
                }
            }
        }
        
        // 添加闪烁光标
        if (inputText.text.isEmpty()) {
            BlinkingCursor(fontSize)
        }
    }
}



/**
 * 处理命令输入
 */
private suspend fun handleCommand(
    context: Context,
    command: String,
    session: TerminalSession,
    scope: CoroutineScope,
    onCommandProcessed: () -> Unit,
    setCurrentExecutionId: (Int) -> Unit,
    setInteractivePrompt: (String) -> Unit,
    setInteractiveInputText: (String) -> Unit,
    setShowInputDialog: (Boolean) -> Unit
) {
    // 将命令添加到历史，保存当前提示符
    val currentPrompt = session.getPrompt()
    // 记录当前工作目录和提示符，以便调试
    // android.util.Log.d("TerminalScreen", "执行命令时的工作目录: ${session.workingDirectory}")
    // android.util.Log.d("TerminalScreen", "使用提示符: $currentPrompt")
    
    session.commandHistory.add(TerminalLine.Input(command, currentPrompt))
    
    // 特殊命令处理
    when (command) {
        "exit" -> {
            // 通知用户应该按返回键返回
            Toast.makeText(context, context.getString(R.string.please_press_back_to_return), Toast.LENGTH_SHORT).show()
            onCommandProcessed()
        }
        else -> {
            // 立即返回，不阻塞UI
            onCommandProcessed()
            
            // 用于安全修改命令历史的互斥锁
            val historyMutex = kotlinx.coroutines.sync.Mutex()
            
            // 使用Termux执行命令，返回输出流
            val outputFlow = session.executeCommand(
                context = context,
                command = command,
                scope = scope,
                onCompletion = { exitCode, success ->
                    // 记录命令执行完成状态
                    android.util.Log.d("TerminalScreen", "命令执行完成: 退出码=$exitCode, 成功=$success")
                }
            )
            
            // 收集输出流并更新UI
            try {
                outputFlow.collect { output ->
                    // 使用scope.launch确保在主线程更新UI状态
                    withContext(Dispatchers.Main) {
                        // 为避免并发修改，使用互斥锁保护
                        historyMutex.withLock {
                            android.util.Log.d("TerminalScreen", "收到命令输出: ${output.text}")
                            
                            // 检查是否包含交互式提示标记
                            if (output.text.startsWith("INTERACTIVE_PROMPT:")) {
                                // 提取提示信息
                                val executionId = output.text.substringAfter("ID:").substringBefore(":PROMPT:")
                                val prompt = output.text.substringAfter(":PROMPT:")
                                
                                android.util.Log.d("TerminalScreen", "检测到交互式提示: ID=$executionId, 提示=$prompt")
                                
                                // 显示交互式输入对话框
                                session.commandHistory.add(TerminalLine.Output("等待用户输入..."))
                                
                                try {
                                    val id = executionId.toInt()
                                    setCurrentExecutionId(id)
                                    setInteractivePrompt(prompt)
                                    setInteractiveInputText("y") // 默认为 yes
                                    setShowInputDialog(true)
                                } catch (e: NumberFormatException) {
                                    session.commandHistory.add(TerminalLine.Output("[无法解析执行ID: $executionId]"))
                                }
                            } 
                            // 检查是否是用户切换消息
                            else if (output.text.startsWith("USER_SWITCHED:")) {
                                // 这是一个特殊的用户切换消息，我们只记录实际的切换消息
                                // 不添加这条特殊消息到历史记录中
                                android.util.Log.d("TerminalScreen", "检测到用户切换: ${output.text}")
                            }
                            else {
                                // 检查是否可能是交互式提示但未被前面的代码识别
                                val output_text = output.text.trim()
                                if (output_text.contains("[Y/n]") || output_text.contains("[y/N]") || 
                                    output_text.contains("(yes/no)") || output_text.contains("yes/no") ||
                                    output_text.contains("是否继续") || output_text.contains("Press Enter")) {
                                    
                                    android.util.Log.d("TerminalScreen", "可能存在未捕获的交互式提示: ${output_text}")
                                }
                                
                                // 普通输出
                                session.commandHistory.add(output)
                            }
                        }
                    }
                }
                
                // 命令执行完毕后记录当前工作目录，以验证是否正确更新
                android.util.Log.d("TerminalScreen", "命令执行完毕后的工作目录: ${session.workingDirectory}")
            } catch (e: Exception) {
                // 处理流收集过程中的异常
                android.util.Log.e("TerminalScreen", "收集命令输出时出错: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    historyMutex.withLock {
                        session.commandHistory.add(TerminalLine.Output("[输出收集失败: ${e.message}]"))
                    }
                }
            }
        }
    }
}
