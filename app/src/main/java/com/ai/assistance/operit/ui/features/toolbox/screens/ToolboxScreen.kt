package com.ai.assistance.operit.ui.features.toolbox.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ai.assistance.operit.ui.components.CustomScaffold
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ai.assistance.operit.ui.features.toolbox.screens.apppermissions.AppPermissionsScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.ffmpegtoolbox.FFmpegToolboxScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.filemanager.FileManagerScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.logcat.LogcatScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.shellexecutor.ShellExecutorScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.screens.TerminalScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.terminalconfig.TerminalAutoConfigScreen
import com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.UIDebuggerScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 工具类别
enum class ToolCategory(val displayName: String) {
        ALL("全部工具"),
        FILE_MANAGEMENT("文件管理"),
        DEVELOPMENT("开发工具"),
        SYSTEM("系统工具")
}

data class Tool(
        val name: String,
        val icon: ImageVector,
        val description: String,
        val category: ToolCategory,
        val onClick: () -> Unit
)

/** 工具箱屏幕，展示可用的各种工具 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolboxScreen(
        navController: NavController,
        onFormatConverterSelected: () -> Unit,
        onFileManagerSelected: () -> Unit,
        onTerminalSelected: () -> Unit,
        onTerminalAutoConfigSelected: () -> Unit,
        onAppPermissionsSelected: () -> Unit,
        onUIDebuggerSelected: () -> Unit,
        onFFmpegToolboxSelected: () -> Unit,
        onShellExecutorSelected: () -> Unit,
        onLogcatSelected: () -> Unit,
        onMarkdownDemoSelected: () -> Unit,
        onTextToSpeechSelected: () -> Unit,
        onSpeechToTextSelected: () -> Unit,
        onToolTesterSelected: () -> Unit,
        onAgreementSelected: () -> Unit
) {
        // 屏幕配置信息，用于响应式布局
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        // 根据屏幕宽度决定每行显示的卡片数量
        val columnsCount =
                when {
                        screenWidth > 840.dp -> 3 // 大屏幕设备显示3列
                        screenWidth > 600.dp -> 2 // 中等屏幕设备显示2列
                        else -> 2 // 小屏幕设备显示2列
                }

        // 当前选中的分类过滤器
        var selectedCategory by remember { mutableStateOf(ToolCategory.ALL) }

        val tools =
                listOf(
                        Tool(
                                name = "工具测试中心",
                                icon = Icons.Default.BuildCircle,
                                description = "测试AI工具的可用性和功能状态",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onToolTesterSelected
                        ),
                        Tool(
                                name = "万能格式转换",
                                icon = Icons.Rounded.Transform,
                                description = "支持多种文件格式之间的转换，方便快捷",
                                category = ToolCategory.FILE_MANAGEMENT,
                                onClick = onFormatConverterSelected
                        ),
                        Tool(
                                name = "文件管理器",
                                icon = Icons.Rounded.Folder,
                                description = "浏览和管理设备文件，支持多种操作",
                                category = ToolCategory.FILE_MANAGEMENT,
                                onClick = onFileManagerSelected
                        ),
                        Tool(
                                name = "文本转语音",
                                icon = Icons.Default.RecordVoiceOver,
                                description = "将文本转换为语音，支持调整语速和音调",
                                category = ToolCategory.SYSTEM,
                                onClick = onTextToSpeechSelected
                        ),
                        Tool(
                                name = "语音识别",
                                icon = Icons.Default.Mic,
                                description = "将语音转换为文本，支持多种引擎和语言",
                                category = ToolCategory.SYSTEM,
                                onClick = onSpeechToTextSelected
                        ),
                        Tool(
                                name = "应用权限管理",
                                icon = Icons.Rounded.Security,
                                description = "管理手机各个应用的权限，保护隐私安全",
                                category = ToolCategory.SYSTEM,
                                onClick = onAppPermissionsSelected
                        ),
                        Tool(
                                name = "用户协议",
                                icon = Icons.Default.Policy,
                                description = "查看应用的用户协议与隐私政策",
                                category = ToolCategory.SYSTEM,
                                onClick = onAgreementSelected
                        ),
                        Tool(
                                name = "命令终端",
                                icon = Icons.Rounded.Terminal,
                                description = "功能强大的命令行终端，执行系统指令",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onTerminalSelected
                        ),
                        Tool(
                                name = "终端自动配置",
                                icon = Icons.Rounded.Build,
                                description = "自动安装配置Python、PIP等开发工具",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onTerminalAutoConfigSelected
                        ),
                        Tool(
                                name = "UI调试工具",
                                icon = Icons.Default.DeviceHub,
                                description = "调试和分析界面元素，支持元素查找和交互",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onUIDebuggerSelected
                        ),
                        Tool(
                                name = "FFmpeg工具箱",
                                icon = Icons.Default.VideoSettings,
                                description = "强大的音视频处理工具，支持转换、裁剪、合并等功能",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onFFmpegToolboxSelected
                        ),
                        Tool(
                                name = "流式Markdown演示",
                                icon = Icons.Default.FormatAlignLeft,
                                description = "高性能流式Markdown渲染演示，支持实时字符流动效果",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onMarkdownDemoSelected
                        ),
                        Tool(
                                name = "命令执行器",
                                icon = Icons.Default.Code,
                                description = "执行Shell命令并查看输出结果，支持常用命令预设",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onShellExecutorSelected
                        ),
                        Tool(
                                name = "日志查看器",
                                icon = Icons.Default.DataObject,
                                description = "实时捕获并查看系统和应用日志，支持过滤和搜索",
                                category = ToolCategory.DEVELOPMENT,
                                onClick = onLogcatSelected
                        )
                )

        // 根据选中的分类过滤工具
        val filteredTools =
                if (selectedCategory == ToolCategory.ALL) {
                        tools
                } else {
                        tools.filter { it.category == selectedCategory }
                }

        Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                        // 顶部标题区域
                        TopAppSection()

                        // 分类选择器
                        CategorySelector(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it }
                        )

                        // 工具网格
                        LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 120.dp),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                        ) { items(filteredTools) { tool -> ToolCard(tool = tool) } }
                }
        }
}

@Composable
private fun TopAppSection() {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.05f
                                                                        ),
                                                                        MaterialTheme.colorScheme
                                                                                .surface
                                                                )
                                                )
                                )
                                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
        ) {
                Text(
                        text = "工具箱",
                        style =
                                MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "便捷实用的工具集合，提升您的工作效率",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
        }
}

@Composable
private fun CategorySelector(
        selectedCategory: ToolCategory,
        onCategorySelected: (ToolCategory) -> Unit
) {
        val categories = ToolCategory.values()

        // 水平滚动的分类选择器
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        val backgroundColor =
                                if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                } else {
                                        MaterialTheme.colorScheme.surface
                                }

                        val textColor =
                                if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }

                        Surface(
                                onClick = { onCategorySelected(category) },
                                shape = RoundedCornerShape(20.dp),
                                color = backgroundColor,
                                tonalElevation = if (isSelected) 0.dp else 1.dp,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.height(36.dp)
                        ) {
                                Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                        Text(
                                                text = category.displayName,
                                                style =
                                                        MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight =
                                                                        if (isSelected)
                                                                                FontWeight.Bold
                                                                        else FontWeight.Normal
                                                        ),
                                                color = textColor
                                        )
                                }
                        }
                }
        }
}

/** 工具项卡片 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolCard(tool: Tool) {
        var isPressed by remember { mutableStateOf(false) }

        // 创建协程作用域
        val scope = rememberCoroutineScope()

        // 缩放动画
        val scale by
                animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = tween(durationMillis = if (isPressed) 100 else 200),
                        label = "scale"
                )

        Card(
                onClick = {
                        isPressed = true
                        // 使用rememberCoroutineScope来启动协程
                        scope.launch {
                                delay(100)
                                tool.onClick()
                                isPressed = false
                        }
                },
                modifier = Modifier.defaultMinSize(minHeight = 140.dp).scale(scale),
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation =
                        CardDefaults.cardElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 8.dp
                        ),
                shape = RoundedCornerShape(12.dp)
        ) {
                // 卡片内容
                Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        // 工具图标带有背景圆圈
                        Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                        Modifier.size(48.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        color =
                                                                when (tool.category) {
                                                                        ToolCategory
                                                                                .FILE_MANAGEMENT ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                        ToolCategory.DEVELOPMENT ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .tertiaryContainer
                                                                        ToolCategory.SYSTEM ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondaryContainer
                                                                        else ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                }
                                                )
                                                .padding(8.dp)
                        ) {
                                Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.name,
                                        modifier = Modifier.size(24.dp),
                                        tint =
                                                when (tool.category) {
                                                        ToolCategory.FILE_MANAGEMENT ->
                                                                MaterialTheme.colorScheme.primary
                                                        ToolCategory.DEVELOPMENT ->
                                                                MaterialTheme.colorScheme.tertiary
                                                        ToolCategory.SYSTEM ->
                                                                MaterialTheme.colorScheme.secondary
                                                        else -> MaterialTheme.colorScheme.primary
                                                }
                                )
                        }

                        Text(
                                text = tool.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                        )

                        Text(
                                text = tool.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 2.dp),
                                maxLines = 2
                        )
                }
        }
}

/** 显示格式转换工具屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatConverterToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        FormatConverterScreen(navController = navController)
                }
        }
}

/** 显示文件管理器工具屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        FileManagerScreen(navController = navController)
                }
        }
}

/** 显示终端工具屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) { TerminalScreen() }
        }
}

/** 显示终端自动配置工具屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalAutoConfigToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        TerminalAutoConfigScreen(navController = navController)
                }
        }
}

/** 显示应用权限管理工具屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPermissionsToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        AppPermissionsScreen(navController = navController)
                }
        }
}

/** 显示UI调试工具屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UIDebuggerToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        UIDebuggerScreen(navController = navController)
                }
        }
}

/** 显示FFmpeg工具箱屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFmpegToolboxToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        FFmpegToolboxScreen(navController = navController)
                }
        }
}

/** 显示Shell命令执行器屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellExecutorToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        ShellExecutorScreen(navController = navController)
                }
        }
}

/** 显示日志查看器屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        LogcatScreen(navController = navController)
                }
        }
}

/** 显示工具测试屏幕 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTesterToolScreen(navController: NavController) {
        CustomScaffold() { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                        com.ai.assistance.operit.ui.features.toolbox.screens.tooltester
                                .ToolTesterScreen(navController = navController)
                }
        }
}
