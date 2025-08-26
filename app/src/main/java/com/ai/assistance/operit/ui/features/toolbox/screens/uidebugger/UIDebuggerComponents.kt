package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.assistance.operit.core.tools.automatic.UIEdgeDefinition
import com.ai.assistance.operit.core.tools.automatic.UIFunction
import com.ai.assistance.operit.core.tools.automatic.UINode
import com.ai.assistance.operit.core.tools.automatic.config.AutomationPackageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.ai.assistance.operit.core.tools.automatic.UINodeType
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import com.ai.assistance.operit.core.tools.system.action.ActionListener
import androidx.compose.ui.text.input.KeyboardType
import com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.components.ActivityMonitorPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.CheckCircle


@Composable
fun UIDebuggerOverlay(
    viewModelStoreOwner: ViewModelStoreOwner,
    onClose: () -> Unit,
    onMinimize: (() -> Unit)? = null
) {
    val viewModel: UIDebuggerViewModel = UIDebuggerViewModel.getInstance()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // 简化状态管理 - 只管理UI分析和元素选择
    var isUIAnalysisActive by remember { mutableStateOf(false) }
    var selectedElement by remember { mutableStateOf<UIElement?>(null) }
    var showAnalysisPanel by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // 注释掉自动停止监听的逻辑，让监听保持运行
    // 用户可以通过手动点击停止按钮来停止监听
    // DisposableEffect(Unit) {
    //     onDispose {
    //         try {
    //             if (uiState.isActivityListening) {
    //                 viewModel.stopActivityListening()
    //             }
    //         } catch (e: Exception) {
    //             // 忽略清理时的异常
    //         }
    //     }
    // }

    Box(modifier = Modifier.fillMaxSize()) {
        // UI元素高亮层
        if (isUIAnalysisActive) {
            ElementHighlightOverlay(
                elements = uiState.elements,
                onElementClick = { element ->
                    selectedElement = element
                }
            )
        }

        // 选中元素信息面板
        if (selectedElement != null && isUIAnalysisActive) {
            ElementInfoPanel(
                element = selectedElement!!,
                onDismiss = { selectedElement = null },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .zIndex(10f)
            )
        }

        // Activity监听面板
        if (uiState.showActivityMonitor) {
            ActivityMonitorPanel(
                isListening = uiState.isActivityListening,
                events = uiState.activityEvents,
                currentActivityName = uiState.currentActivityName,
                onStartListening = { viewModel.startActivityListening() },
                onStopListening = { viewModel.stopActivityListening() },
                onClearEvents = { viewModel.clearActivityEvents() },
                onDismiss = { 
                    viewModel.toggleActivityMonitor()
                    // 不再自动停止监听，让用户手动控制
                },
                // 新增自动构建相关参数
                autoGraphBuilding = uiState.autoGraphBuilding,
                detectedCurrentPackageName = uiState.detectedCurrentPackageName,
                selectedPackage = uiState.selectedPackage,
                autoGeneratedNodes = uiState.autoGeneratedNodes,
                autoGeneratedEdges = uiState.autoGeneratedEdges,
                onToggleAutoGraphBuilding = { viewModel.toggleAutoGraphBuilding() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .zIndex(20f)
            )
        }

        // 当前分析Activity信息面板
        if (isUIAnalysisActive && showAnalysisPanel && (uiState.currentAnalyzedActivityName != null || uiState.currentAnalyzedPackageName != null)) {
            val clipboardManager = LocalClipboardManager.current
            val currentActivityName = uiState.currentAnalyzedActivityName
            val currentPackageName = uiState.currentAnalyzedPackageName
            
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .widthIn(max = 280.dp)
                    .zIndex(15f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "当前分析界面",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        // 复制按钮
                        if (currentActivityName != null) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentActivityName))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制Activity名称",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        // 关闭按钮
                        IconButton(
                            onClick = { showAnalysisPanel = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭面板",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    
                    if (currentActivityName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentActivityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (currentPackageName != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "包名: $currentPackageName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // 紧凑的控制面板 - 右下角，4个小按钮
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .zIndex(5f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UI分析按钮 - 更小的尺寸
                FloatingActionButton(
                    onClick = {
                        try {
                            if (isUIAnalysisActive) {
                                // 关闭UI分析
                                isUIAnalysisActive = false
                                selectedElement = null
                                showAnalysisPanel = false
                            } else {
                                // 开启UI分析
                                viewModel.refreshUI()
                                isUIAnalysisActive = true
                                showAnalysisPanel = true
                            }
                        } catch (e: Exception) {
                            // 处理异常
                        }
                    },
                    containerColor = if (isUIAnalysisActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = if (isUIAnalysisActive) "关闭UI分析" else "UI分析",
                        tint = if (isUIAnalysisActive) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Activity监听按钮
                FloatingActionButton(
                    onClick = {
                        try {
                            if (uiState.showActivityMonitor) {
                                // 关闭监听面板，但不停止监听
                                viewModel.toggleActivityMonitor()
                            } else {
                                // 打开监听面板
                                viewModel.toggleActivityMonitor()
                            }
                        } catch (e: Exception) {
                            // 处理异常
                        }
                    },
                    containerColor = when {
                        uiState.showActivityMonitor -> MaterialTheme.colorScheme.primary
                        uiState.isActivityListening -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when {
                            uiState.isActivityListening -> Icons.Default.VisibilityOff
                            else -> Icons.Default.Visibility
                        },
                        contentDescription = if (uiState.showActivityMonitor) "折叠面板" else "Activity监听",
                        tint = when {
                            uiState.showActivityMonitor -> MaterialTheme.colorScheme.onPrimary
                            uiState.isActivityListening -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 最小化按钮
                onMinimize?.let { minimizeCallback ->
                    FloatingActionButton(
                        onClick = minimizeCallback,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "最小化",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 关闭按钮
                FloatingActionButton(
                    onClick = onClose,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 操作反馈提示
        if (uiState.showActionFeedback) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 100.dp)
                    .zIndex(25f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.actionFeedbackMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                }
            }
        }

        // 错误消息提示
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 100.dp)
                    .zIndex(25f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error,
                        style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
    }
}

// 调试器模式枚举 - 简化版本
enum class DebuggerMode {
    CLOSED,           // 关闭状态
    UI_ANALYSIS,      // UI分析模式
    ACTIVITY_MONITOR  // Activity监听模式
}



@Composable
fun NodeListPanel(
    packageInfo: com.ai.assistance.operit.core.tools.automatic.config.AutomationPackageInfo,
    nodes: List<com.ai.assistance.operit.core.tools.automatic.UINode>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(max = 350.dp)
            .heightIn(max = 500.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${packageInfo.name} - 页面节点",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (nodes.isEmpty()) {
                Text(
                    text = "没有找到页面节点",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(nodes) { node ->
                        NodeItem(node = node, isSelected = false, onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
fun NodeItem(
    node: UINode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (isSelected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = colors
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "类型: ${node.nodeType}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NodeItemWithActions(
    node: UINode,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val colors = if (isSelected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Blue,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                } else {
                    Modifier
                }
            ),
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        ListItem(
            headlineContent = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 选中状态指示器
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已选中",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            supportingContent = { 
                Column {
                    Text(
                        text = "类型: ${node.nodeType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    node.activityName?.let { activityName ->
                        Text(
                            text = "Activity: $activityName",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 编辑按钮
                    onEdit?.let { editCallback ->
                        IconButton(
                            onClick = editCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑节点",
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 删除按钮
                    onDelete?.let { deleteCallback ->
                        IconButton(
                            onClick = deleteCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除节点",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun EdgeItem(
    fromNodeName: String,
    edge: UIEdgeDefinition,
    onClick: () -> Unit,
    onShowDetails: ((UIEdgeDefinition, String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text("到: ${edge.toNodeName}", fontWeight = FontWeight.Bold) },
            supportingContent = {
                val description = edge.operations.joinToString(separator = " -> ") { it.description }
                Text("操作: $description")
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 查看详情按钮
                    onShowDetails?.let { showDetailsCallback ->
                        IconButton(
                            onClick = { showDetailsCallback(edge, fromNodeName) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "查看操作详情",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // 跳转按钮
                Icon(Icons.Default.ArrowForward, contentDescription = "跳转到节点")
                }
            }
        )
    }
}

@Composable
fun EdgeItemWithActions(
    fromNodeName: String,
    edge: UIEdgeDefinition,
    onClick: () -> Unit,
    onShowDetails: ((UIEdgeDefinition, String) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
                modifier = Modifier
                    .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text("到: ${edge.toNodeName}", fontWeight = FontWeight.Bold) },
            supportingContent = { EdgeOperationDetails(edge = edge) },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 编辑按钮
                    onEdit?.let { editCallback ->
                        IconButton(
                            onClick = editCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑边",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // 删除按钮
                    onDelete?.let { deleteCallback ->
                        IconButton(
                            onClick = deleteCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除边",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // 查看详情按钮
                    onShowDetails?.let { showDetailsCallback ->
                        IconButton(
                            onClick = { showDetailsCallback(edge, fromNodeName) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "查看操作详情",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // 跳转按钮
                    Icon(Icons.Default.ArrowForward, contentDescription = "跳转到节点")
                }
            }
        )
    }
}



@Composable
private fun getOperationIcon(operation: UIOperation): ImageVector {
    return when (operation) {
        is UIOperation.Click -> Icons.Filled.TouchApp
        is UIOperation.Input -> Icons.Filled.TextFields
        is UIOperation.Wait -> Icons.Filled.Timer
        is UIOperation.ValidateElement -> Icons.Filled.Verified
        is UIOperation.PressKey -> Icons.Filled.Keyboard
        is UIOperation.LaunchApp -> Icons.Filled.Launch
        is UIOperation.KillApp -> Icons.Filled.Close
        is UIOperation.NoOp -> Icons.Filled.Info
        is UIOperation.Swipe -> Icons.Filled.ArrowForward
        is UIOperation.ValidateState -> Icons.Filled.VerifiedUser
        is UIOperation.WaitForPage -> Icons.Filled.Timer
        is UIOperation.Sequential -> Icons.Filled.MoreVert
    }
}

@Composable
private fun EdgeOperationDetails(edge: UIEdgeDefinition) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Operation Icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            edge.operations.forEach { op ->
                Icon(
                    imageVector = getOperationIcon(op),
                    contentDescription = op.javaClass.simpleName,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Description with highlighted template variables
        val operationText = remember(edge.operations) {
            buildAnnotatedString {
                val fullDescription = edge.operations.joinToString(" -> ") { it.description }
                val pattern = "\\{\\{([^}]+)\\}\\}".toRegex()
                var lastIndex = 0

                if (fullDescription.isBlank()) {
                    append("No description")
                } else {
                    pattern.findAll(fullDescription).forEach { matchResult ->
                        val startIndex = matchResult.range.first
                        val endIndex = matchResult.range.last + 1
                        if (startIndex > lastIndex) {
                            append(fullDescription.substring(lastIndex, startIndex))
                        }
                        // Corrected: Define SpanStyle directly without calling a Composable
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(fullDescription.substring(startIndex, endIndex))
                        }
                        lastIndex = endIndex
                    }
                    if (lastIndex < fullDescription.length) {
                        append(fullDescription.substring(lastIndex))
                    }
                }
            }
        }

                Text(
            text = operationText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f) // Give weight to allow wrapping and ellipsis
        )

        // Validation Icon
        if (edge.validation != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Has Validation Step",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
fun FunctionItemWithActions(
    function: UIFunction,
    onClick: () -> Unit,
    onShowDetails: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text(function.name, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(function.description) },
            trailingContent = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    onEdit?.let { editCallback ->
                        IconButton(
                            onClick = editCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑功能",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    onDelete?.let { deleteCallback ->
                        IconButton(
                            onClick = deleteCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除功能",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    onShowDetails?.let { showDetailsCallback ->
                        IconButton(
                            onClick = showDetailsCallback,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "查看功能详情",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun NodeFunctionItem(
    function: UIFunction,
    onShowDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    text = function.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            supportingContent = {
                Text(
                    text = function.description,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "功能",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            trailingContent = {
                IconButton(
                    onClick = onShowDetails,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "查看功能详情",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        )
    }
}

@Composable
fun ElementHighlightOverlay(
    elements: List<UIElement>,
    onElementClick: (UIElement) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(elements) {
                detectTapGestures { offset ->
                    val tappedElement = elements
                        .filter { element ->
                            element.bounds?.let { bounds ->
                                offset.x >= bounds.left && 
                                offset.x <= bounds.right && 
                                offset.y >= bounds.top && 
                                offset.y <= bounds.bottom
                            } ?: false
                        }
                        .minByOrNull { element ->
                            element.bounds?.let { bounds ->
                                bounds.width() * bounds.height()
                            } ?: Int.MAX_VALUE
                        }
                    
                    tappedElement?.let(onElementClick)
                }
            }
    ) {
        elements
            .sortedByDescending { element ->
                element.bounds?.let { bounds ->
                    bounds.width() * bounds.height()
                } ?: 0
            }
            .forEach { element ->
                element.bounds?.let { bounds ->
                    drawElementHighlight(bounds)
                }
            }
    }
}

private fun DrawScope.drawElementHighlight(
    bounds: android.graphics.Rect
) {
    val color = Color.Red
    
    drawRect(
        color = color,
        topLeft = Offset(bounds.left.toFloat(), bounds.top.toFloat()),
        size = Size(bounds.width().toFloat(), bounds.height().toFloat()),
        style = Stroke(width = 2.dp.toPx())
    )
}

@Composable
fun ElementInfoPanel(
    element: UIElement,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Surface(
        modifier = modifier
            .widthIn(max = 300.dp)
            .heightIn(max = 400.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "控件信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = element.typeDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ActivityName特殊显示区域
            if (element.activityName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "当前Activity",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(element.activityName))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制Activity名称",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = element.activityName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (element.packageName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "包名: ${element.packageName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false)
            ) {
                Text(
                    text = element.getFullDetails(),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (element.bounds != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "尺寸: ${element.bounds.width()}×${element.bounds.height()}px",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 

@Composable
fun ExportPackageItem(
    packageInfo: com.ai.assistance.operit.core.tools.automatic.config.AutomationPackageInfo,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageInfo.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = packageInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onExport) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("导出")
            }
        }
    }
} 


// 编辑相关组件

@Composable
fun CreatePackageDialog(
    onCreatePackage: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("创建新配置包")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("应用名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("包名") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (appName.isNotBlank() && packageName.isNotBlank()) {
                        onCreatePackage(appName, packageName, description)
                        onDismiss()
                    }
                },
                enabled = appName.isNotBlank() && packageName.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


fun getSelectorTypeName(selector: UISelector?): String {
    return when (selector) {
        is UISelector.ByResourceId -> "ByResourceId"
        is UISelector.ByText -> "ByText"
        is UISelector.ByContentDesc -> "ByContentDesc"
        is UISelector.ByClassName -> "ByClassName"
        is UISelector.ByBounds -> "ByBounds"
        is UISelector.ByXPath -> "ByXPath"
        is UISelector.Compound -> "Compound"
        else -> "Unknown"
    }
}

fun getSelectorValue(selector: UISelector?): String {
    return when (selector) {
        is UISelector.ByResourceId -> selector.id
        is UISelector.ByText -> selector.text
        is UISelector.ByContentDesc -> selector.desc
        is UISelector.ByClassName -> selector.name
        is UISelector.ByBounds -> selector.bounds
        is UISelector.ByXPath -> selector.xpath
        is UISelector.Compound -> "..."
        else -> ""
    }
}

