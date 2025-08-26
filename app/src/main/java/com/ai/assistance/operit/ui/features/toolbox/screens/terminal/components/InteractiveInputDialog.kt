package com.ai.assistance.operit.ui.features.toolbox.screens.terminal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.utils.TerminalColors
import com.ai.assistance.operit.R

/**
 * 交互式输入对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveInputDialog(
    prompt: String,
    initialInput: String,
    onDismissRequest: () -> Unit,
    onInputSubmit: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialInput) }
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TerminalColors.ParrotBg,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, TerminalColors.ParrotAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "命令需要输入",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TerminalColors.ParrotAccent,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 提示文本
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .fillMaxWidth()
                )
                
                // 输入框
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            // 简化处理，只发送干净的文本
                            val cleanInput = inputText.trim()
                            onInputSubmit(cleanInput) 
                        }
                    ),
                    label = { Text("输入响应", color = Color.White.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        IconButton(
                            onClick = { 
                                // 简化处理，只发送干净的文本
                                val cleanInput = inputText.trim()
                                onInputSubmit(cleanInput) 
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = context.getString(R.string.send), tint = TerminalColors.ParrotAccent)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = TerminalColors.ParrotAccent,
                        focusedBorderColor = TerminalColors.ParrotAccent,
                        unfocusedBorderColor = TerminalColors.ParrotAccent.copy(alpha = 0.5f),
                        focusedContainerColor = TerminalColors.ParrotBgLight,
                        unfocusedContainerColor = TerminalColors.ParrotBgLight
                    ),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace
                    )
                )
                
                // 按钮行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 快捷按钮 - 改为发送完整单词
                    Button(
                        onClick = { onInputSubmit("yes") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalColors.ParrotGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(context.getString(R.string.terminal_yes_y), fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { onInputSubmit("no") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalColors.ParrotRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(context.getString(R.string.terminal_no_n), fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = { 
                        // 清理输入文本并发送
                        val cleanInput = inputText.trim()
                        onInputSubmit(cleanInput) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalColors.ParrotAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(context.getString(R.string.terminal_send_custom_input), fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(context.getString(R.string.cancel))
                }
            }
        }
    }
} 