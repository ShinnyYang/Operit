package com.ai.assistance.operit.ui.features.toolbox.screens.terminalconfig

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ai.assistance.operit.R
import com.ai.assistance.operit.core.tools.system.AndroidShellExecutor.CommandResult
import com.ai.assistance.operit.core.tools.system.TermuxCommandExecutor
import com.ai.assistance.operit.core.tools.system.termux.TermuxCommandOutputReceiver
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.components.InteractiveInputDialog
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.model.TerminalLine
import com.ai.assistance.operit.ui.features.toolbox.screens.terminal.model.TerminalSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "TerminalAutoConfig"

/** 终端自动配置屏幕 提供自动化安装和配置常用命令行工具的功能 */
@Composable
fun TerminalAutoConfigScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 状态变量
    var outputText by remember { mutableStateOf("欢迎使用终端自动配置工具\n选择需要安装的组件点击按钮开始安装") }
    var isExecuting by remember { mutableStateOf(false) }
    var currentTask by remember { mutableStateOf("") }

    // 添加初始化加载状态
    var isInitialLoading by remember { mutableStateOf(true) }

    // 软件包安装状态
    var pythonInstalled by remember { mutableStateOf(false) }
    var pipInstalled by remember { mutableStateOf(false) }
    var nodeInstalled by remember { mutableStateOf(false) }
    var gitInstalled by remember { mutableStateOf(false) }
    var rubyInstalled by remember { mutableStateOf(false) }
    var goInstalled by remember { mutableStateOf(false) }
    var rustInstalled by remember { mutableStateOf(false) }
    var tunaSourceEnabled by remember { mutableStateOf(false) }

    // 会话ID
    var sessionId by remember { mutableStateOf<String?>(null) }

    // 交互式输入对话框状态
    var showInputDialog by remember { mutableStateOf(false) }
    var interactivePrompt by remember { mutableStateOf("") }
    var interactiveInputText by remember { mutableStateOf("") }
    var currentExecutionId by remember { mutableStateOf(-1) }

    // PIP包选择对话框状态
    var showPipPackageDialog by remember { mutableStateOf(false) }

    // 创建或获取会话
    LaunchedEffect(key1 = Unit) {
        // 如果没有已有会话，创建一个新会话
        if (TerminalSessionManager.getSessionCount() == 0) {
            val session = TerminalSessionManager.createSession("自动配置会话")
            sessionId = session.id
        } else {
            // 使用已存在的会话
            sessionId = TerminalSessionManager.activeSessionId.value
            if (sessionId == null && TerminalSessionManager.sessions.isNotEmpty()) {
                sessionId = TerminalSessionManager.sessions[0].id
            }
        }
    }

    // 检查已安装的组件
    LaunchedEffect(key1 = Unit) {
        // 开始加载
        isInitialLoading = true

        checkInstalledComponents(
                context,
                onResult = { python, pip, node, git, ruby, go, rust ->
                    pythonInstalled = python
                    pipInstalled = pip
                    nodeInstalled = node
                    gitInstalled = git
                    rubyInstalled = ruby
                    goInstalled = go
                    rustInstalled = rust
                }
        )

        // 检查是否已启用清华源
        checkTunaSourceEnabled(context) { enabled ->
            tunaSourceEnabled = enabled

            // 所有检查完成，结束加载
            isInitialLoading = false
        }
    }

    // 加载对话框
    if (isInitialLoading) {
        AlertDialog(
                onDismissRequest = { /* 不允许用户关闭 */},
                title = { Text("正在加载") },
                text = {
                    Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                        Text("正在检查已安装的组件，请稍候...")
                    }
                },
                confirmButton = {}
        )
    }

    // 交互式输入对话框
    if (showInputDialog) {
        InteractiveInputDialog(
                prompt = interactivePrompt,
                initialInput = interactiveInputText,
                onDismissRequest = { showInputDialog = false },
                onInputSubmit = { input ->
                    showInputDialog = false
                    interactiveInputText = ""

                    // 处理用户输入
                    scope.launch {
                        try {
                            val success =
                                    TermuxCommandExecutor.sendInputToCommand(
                                            context = context,
                                            executionId = currentExecutionId,
                                            input = input
                                    )

                            if (success) {
                                sessionId?.let { id ->
                                    TerminalSessionManager.sessions.find { it.id == id }?.let {
                                            session ->
                                        session.commandHistory.add(
                                                TerminalLine.Input(input, "User Input: ")
                                        )
                                    }
                                }
                            } else {
                                outputText += "\n[输入发送失败]"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "发送用户输入时出错: ${e.message}", e)
                        }
                    }
                }
        )
    }

    // PIP包选择对话框
    if (showPipPackageDialog) {
        PipPackageSelectionDialog(
                onDismissRequest = { showPipPackageDialog = false },
                onPackagesSelected = { packages ->
                    showPipPackageDialog = false
                    if (packages.isNotEmpty()) {
                        scope.launch {
                            isExecuting = true
                            currentTask = "安装 PIP 包"
                            installPipPackages(
                                    context,
                                    sessionId,
                                    packages,
                                    onOutput = { output -> outputText += "\n$output" },
                                    onInteractivePrompt = { prompt, executionId ->
                                        interactivePrompt = prompt
                                        currentExecutionId = executionId
                                        showInputDialog = true
                                    },
                                    onComplete = { success ->
                                        isExecuting = false
                                        if (success) {
                                            Toast.makeText(context, context.getString(R.string.pip_package_install_success), Toast.LENGTH_SHORT)
                                                    .show()
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.pip_package_install_failed), Toast.LENGTH_SHORT)
                                                    .show()
                                        }
                                    }
                            )
                        }
                    }
                }
        )
    }

    // 使用Box作为根容器，确保滚动可以覆盖整个屏幕
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(scrollState) // 添加对整个界面的滚动支持
                                .padding(16.dp)
        ) {
            // 标题
            Text(
                    text = "终端自动配置",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
            )

            // 命令输出区域 - 设置固定高度确保可见
            Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp), // 固定高度确保输出区域可见
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    // 使用Modifier.fillMaxWidth()而不是fillMaxSize()，避免挤压高度
                    Text(
                            text = outputText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )

                    if (isExecuting) {
                        LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        )

                        Text(
                                text = "正在执行: $currentTask",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp)
                                                .background(
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                        alpha = 0.8f
                                                                ),
                                                        shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 安装选项
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = "安装选项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // 清华源设置选项
                InstallOptionCard(
                        title = "清华源镜像",
                        description = "切换至清华大学开源镜像站，提高下载速度",
                        icon = Icons.Default.Speed,
                        installed = tunaSourceEnabled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "切换软件源"
                                switchToTunaMirror(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            tunaSourceEnabled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "清华源设置成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "清华源设置失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                InstallOptionCard(
                        title = "Python 环境",
                        description = "安装 Python 3 和相关依赖",
                        icon = Icons.Default.Code,
                        installed = pythonInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "安装 Python"
                                installPython(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            pythonInstalled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "Python 安装成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "Python 安装失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                // PIP 清华源选项卡 - 用于选择并安装 PIP 包
                InstallOptionCard(
                        title = "PIP 清华源",
                        description = "通过清华源安装 Python 包",
                        icon = Icons.Default.Settings,
                        installed = pipInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            // 显示 PIP 包选择对话框
                            showPipPackageDialog = true
                        }
                )

                InstallOptionCard(
                        title = "Node.js 环境",
                        description = "安装 Node.js 和 NPM",
                        icon = Icons.Default.Code,
                        installed = nodeInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "安装 Node.js"
                                installNodejs(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            nodeInstalled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "Node.js 安装成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "Node.js 安装失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                InstallOptionCard(
                        title = "Git 版本控制",
                        description = "安装 Git 版本控制系统",
                        icon = Icons.Default.Code,
                        installed = gitInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "安装 Git"
                                installGit(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            gitInstalled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "Git 安装成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "Git 安装失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                // 添加 Ruby 环境选项
                InstallOptionCard(
                        title = "Ruby 环境",
                        description = "安装 Ruby 语言和 Gem 包管理器",
                        icon = Icons.Default.Code,
                        installed = rubyInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "安装 Ruby"
                                installRuby(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            rubyInstalled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "Ruby 安装成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "Ruby 安装失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                // 添加 Go 环境选项
                InstallOptionCard(
                        title = "Go 环境",
                        description = "安装 Go 语言及开发环境",
                        icon = Icons.Default.Code,
                        installed = goInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "安装 Go"
                                installGo(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            goInstalled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "Go 安装成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "Go 安装失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                // 添加 Rust 环境选项
                InstallOptionCard(
                        title = "Rust 环境",
                        description = "安装 Rust 语言及 Cargo 包管理器",
                        icon = Icons.Default.Code,
                        installed = rustInstalled,
                        isExecuting = isExecuting,
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "安装 Rust"
                                installRust(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            rustInstalled = success
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "Rust 安装成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "Rust 安装失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        }
                )

                Button(
                        onClick = {
                            scope.launch {
                                isExecuting = true
                                currentTask = "更新软件包"
                                updatePackages(
                                        context,
                                        sessionId,
                                        onOutput = { output -> outputText += "\n$output" },
                                        onInteractivePrompt = { prompt, executionId ->
                                            interactivePrompt = prompt
                                            currentExecutionId = executionId
                                            showInputDialog = true
                                        },
                                        onComplete = { success ->
                                            isExecuting = false
                                            if (success) {
                                                Toast.makeText(
                                                                context,
                                                                "软件包更新成功",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                // 更新后重新检查安装状态
                                                scope.launch {
                                                    checkInstalledComponents(
                                                            context,
                                                            onResult = {
                                                                    python,
                                                                    pip,
                                                                    node,
                                                                    git,
                                                                    ruby,
                                                                    go,
                                                                    rust ->
                                                                pythonInstalled = python
                                                                pipInstalled = pip
                                                                nodeInstalled = node
                                                                gitInstalled = git
                                                                rubyInstalled = ruby
                                                                goInstalled = go
                                                                rustInstalled = rust
                                                            }
                                                    )
                                                }
                                            } else {
                                                Toast.makeText(
                                                                context,
                                                                "软件包更新失败",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            }
                                        }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = !isExecuting
                ) {
                    Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "更新",
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("更新所有软件包")
                }
            }
        }
    }
}

@Composable
fun InstallOptionCard(
        title: String,
        description: String,
        icon: ImageVector,
        installed: Boolean,
        isExecuting: Boolean,
        onClick: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (installed) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                    ),
            enabled = !isExecuting,
            onClick = onClick
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint =
                            if (installed) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color =
                                if (installed) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                )

                Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (installed)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.8f
                                        )
                                else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (installed) {
                Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已安装",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// 检查已安装的组件
suspend fun checkInstalledComponents(
        context: Context,
        onResult:
                (
                        pythonInstalled: Boolean,
                        pipInstalled: Boolean,
                        nodeInstalled: Boolean,
                        gitInstalled: Boolean,
                        rubyInstalled: Boolean,
                        goInstalled: Boolean,
                        rustInstalled: Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        var pythonInstalled = false
        var pipInstalled = false
        var nodeInstalled = false
        var gitInstalled = false
        var rubyInstalled = false
        var goInstalled = false
        var rustInstalled = false

        // 检查Python
        val pythonResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v python3"
                )
        pythonInstalled = pythonResult.success && pythonResult.stdout.contains("python3")

        // 检查PIP
        val pipResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v pip"
                )
        pipInstalled = pipResult.success && pipResult.stdout.contains("pip")

        // 检查Node.js
        val nodeResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v node"
                )
        nodeInstalled = nodeResult.success && nodeResult.stdout.contains("node")

        // 检查Git
        val gitResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v git"
                )
        gitInstalled = gitResult.success && gitResult.stdout.contains("git")

        // 检查Ruby
        val rubyResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v ruby"
                )
        rubyInstalled = rubyResult.success && rubyResult.stdout.contains("ruby")

        // 检查Go
        val goResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v go"
                )
        goInstalled = goResult.success && goResult.stdout.contains("go")

        // 检查Rust
        val rustResult =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "command -v rustc"
                )
        rustInstalled = rustResult.success && rustResult.stdout.contains("rustc")

        withContext(Dispatchers.Main) {
            onResult(
                    pythonInstalled,
                    pipInstalled,
                    nodeInstalled,
                    gitInstalled,
                    rubyInstalled,
                    goInstalled,
                    rustInstalled
            )
        }
    }
}

// 检查是否已启用清华源
suspend fun checkTunaSourceEnabled(context: Context, onResult: (Boolean) -> Unit) {
    withContext(Dispatchers.IO) {
        // 直接检查文件内容
        val result =
                TermuxCommandExecutor.executeCommand(
                        context = context,
                        command = "cat \$PREFIX/etc/apt/sources.list"
                )

        // 检查文件内容中是否包含清华源URL
        val containsTuna =
                result.success &&
                        (result.stdout.contains("mirrors.tuna.tsinghua.edu.cn") ||
                                result.stdout.contains("tsinghua.edu.cn"))

        Log.d(TAG, "源文件内容: ${result.stdout}")
        Log.d(TAG, "检查清华源: $containsTuna")

        withContext(Dispatchers.Main) { onResult(containsTuna) }
    }
}

// 切换至清华源
suspend fun switchToTunaMirror(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始切换到清华源镜像...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        // 修改源地址的命令 - 分步执行更可靠
        val backupCommand = "cp \$PREFIX/etc/apt/sources.list \$PREFIX/etc/apt/sources.list.bak"
        val sedCommand =
                "sed -i 's@^\\(deb.*stable main\\)$@#\\1\\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main stable main@' \$PREFIX/etc/apt/sources.list"
        val updateCommand = "apt update"

        if (session != null) {
            // 分步执行命令更可靠
            var allCommandsSucceeded = true

            // 1. 备份原始sources.list
            onOutput("正在备份原始软件源配置...")
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    backupCommand,
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        Log.d(TAG, "备份命令完成: 退出码=$exitCode, 成功=$success")
                        allCommandsSucceeded = allCommandsSucceeded && success

                        if (!success) {
                            onOutput("备份原始软件源失败，退出码: $exitCode")
                        }
                    }
            )

            // 2. 修改sources.list
            onOutput("正在修改软件源配置...")
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    sedCommand,
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        Log.d(TAG, "修改软件源命令完成: 退出码=$exitCode, 成功=$success")
                        allCommandsSucceeded = allCommandsSucceeded && success

                        if (!success) {
                            onOutput("修改软件源配置失败，退出码: $exitCode")
                        }
                    }
            )

            // 3. 更新软件包信息
            onOutput("正在更新软件包信息...")
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    updateCommand,
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        Log.d(TAG, "更新软件包命令完成: 退出码=$exitCode, 成功=$success")
                        allCommandsSucceeded = allCommandsSucceeded && success

                        if (!success) {
                            onOutput("更新软件包信息失败，退出码: $exitCode")
                        }
                    }
            )

            // 检查命令执行后是否成功切换
            val checkResult =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command =
                                    "grep -q 'mirrors.tuna.tsinghua.edu.cn' \$PREFIX/etc/apt/sources.list && echo 'SUCCESS'"
                    )

            withContext(Dispatchers.Main) {
                val success =
                        allCommandsSucceeded &&
                                checkResult.success &&
                                checkResult.stdout.contains("SUCCESS")
                onOutput("清华源设置" + if (success) "成功" else "失败")

                // 如果成功，给出一些有用的信息
                if (success) {
                    onOutput("清华源设置成功！软件包下载速度将大幅提升。")
                    onOutput(
                            "如果需要恢复默认源，可以执行: cp \$PREFIX/etc/apt/sources.list.bak \$PREFIX/etc/apt/sources.list"
                    )
                }

                onComplete(success)
            }
        } else {
            // 回退到直接执行命令的逻辑保持不变
            val fullCommand = "$backupCommand && $sedCommand && $updateCommand"
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = fullCommand,
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    // 检查执行结果
                                    val checkResult =
                                            TermuxCommandExecutor.executeCommand(
                                                    context = context,
                                                    command =
                                                            "grep -q 'mirrors.tuna.tsinghua.edu.cn' \$PREFIX/etc/apt/sources.list && echo 'SUCCESS'"
                                            )

                                    val success =
                                            checkResult.success &&
                                                    checkResult.stdout.contains("SUCCESS")
                                    onOutput("清华源设置" + if (success) "成功" else "失败")

                                    // 如果成功，给出一些有用的信息
                                    if (success) {
                                        onOutput("清华源设置成功！软件包下载速度将大幅提升。")
                                        onOutput(
                                                "如果需要恢复默认源，可以执行: cp \$PREFIX/etc/apt/sources.list.bak \$PREFIX/etc/apt/sources.list"
                                        )
                                    }

                                    onComplete(success)
                                }
                            },
                            outputReceiver =
                                    object : TermuxCommandOutputReceiver {
                                        override fun onStdout(output: String, isComplete: Boolean) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onOutput(output)
                                            }
                                        }

                                        override fun onStderr(error: String, isComplete: Boolean) {
                                            // 检查是否是交互式提示
                                            if (error.startsWith("INTERACTIVE_PROMPT:")) {
                                                val promptText =
                                                        error.substringAfter("INTERACTIVE_PROMPT:")
                                                                .trim()
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    onOutput("[需要输入] $promptText")
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    onOutput(error)
                                                }
                                            }
                                        }

                                        override fun onComplete(result: CommandResult) {
                                            // 已在resultCallback中处理
                                        }

                                        override fun onError(error: String, exitCode: Int) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onOutput("错误: $error (退出码: $exitCode)")
                                            }
                                        }
                                    }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 安装Python
suspend fun installPython(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始安装 Python...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg install python -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("Python 安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg install python -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("Python 安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            },
                            outputReceiver =
                                    object : TermuxCommandOutputReceiver {
                                        override fun onStdout(output: String, isComplete: Boolean) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onOutput(output)
                                            }
                                        }

                                        override fun onStderr(error: String, isComplete: Boolean) {
                                            // 检查是否是交互式提示
                                            if (error.startsWith("INTERACTIVE_PROMPT:")) {
                                                val promptText =
                                                        error.substringAfter("INTERACTIVE_PROMPT:")
                                                                .trim()
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    onOutput("[需要输入] $promptText")
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    onOutput(error)
                                                }
                                            }
                                        }

                                        override fun onComplete(result: CommandResult) {
                                            // 已在resultCallback中处理
                                        }

                                        override fun onError(error: String, exitCode: Int) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onOutput("错误: $error (退出码: $exitCode)")
                                            }
                                        }
                                    }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 安装Node.js
suspend fun installNodejs(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始安装 Node.js...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg install nodejs -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("Node.js 安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg install nodejs -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("Node.js 安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 安装Git
suspend fun installGit(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始安装 Git...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg install git -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("Git 安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg install git -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("Git 安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 更新软件包
suspend fun updatePackages(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始更新软件包...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg upgrade -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("软件包更新完成，请检查结果")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg upgrade -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("软件包更新" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 添加 PIP 包选择对话框
@Composable
fun PipPackageSelectionDialog(
        onDismissRequest: () -> Unit,
        onPackagesSelected: (List<String>) -> Unit
) {
    val commonPipPackages =
            listOf(
                    "numpy",
                    "pandas",
                    "matplotlib",
                    "scikit-learn",
                    "tensorflow",
                    "pytorch",
                    "flask",
                    "django",
                    "requests",
                    "beautifulsoup4",
                    "selenium",
                    "pillow",
                    "openpyxl",
                    "pymongo",
                    "sqlalchemy"
            )

    val packageSelections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            commonPipPackages.forEach { this[it] = false }
        }
    }

    var customPackage by remember { mutableStateOf("") }

    AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("选择要安装的PIP包") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("从清华源安装以下PIP包:", modifier = Modifier.padding(bottom = 8.dp))

                    // 常用包列表
                    commonPipPackages.forEach { pkg ->
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                    checked = packageSelections[pkg] == true,
                                    onCheckedChange = { selected ->
                                        packageSelections[pkg] = selected
                                    }
                            )
                            Text(pkg)
                        }
                    }

                    // 自定义包输入
                    OutlinedTextField(
                            value = customPackage,
                            onValueChange = { customPackage = it },
                            label = { Text("其他包 (用空格分隔多个包)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            val selectedPackages =
                                    commonPipPackages
                                            .filter { packageSelections[it] == true }
                                            .toMutableList()

                            if (customPackage.isNotEmpty()) {
                                selectedPackages.addAll(
                                        customPackage.split(" ").filter { it.isNotEmpty() }
                                )
                            }

                            onPackagesSelected(selectedPackages)
                        }
                ) { Text("安装") }
            },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
    )
}

// 安装 PIP 包函数
suspend fun installPipPackages(
        context: Context,
        sessionId: String?,
        packages: List<String>,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        if (packages.isEmpty()) {
            withContext(Dispatchers.Main) {
                onOutput("未选择任何包")
                onComplete(false)
            }
            return@withContext
        }

        val packagesList = packages.joinToString(" ")
        onOutput("开始安装PIP包: $packagesList")

        // 首先配置PIP清华源
        val configPipMirrorCommand =
                """
            mkdir -p ~/.pip
            echo '[global]
            index-url = https://pypi.tuna.tsinghua.edu.cn/simple
            trusted-host = pypi.tuna.tsinghua.edu.cn' > ~/.pip/pip.conf
        """.trimIndent()

        // 构建安装命令
        val installCommand = "pip install --upgrade $packagesList"
        val fullCommand = "$configPipMirrorCommand && $installCommand"

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    fullCommand,
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("PIP包安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = fullCommand,
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("PIP包安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            },
                            outputReceiver =
                                    object : TermuxCommandOutputReceiver {
                                        override fun onStdout(output: String, isComplete: Boolean) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onOutput(output)
                                            }
                                        }

                                        override fun onStderr(error: String, isComplete: Boolean) {
                                            // 检查是否是交互式提示
                                            if (error.startsWith("INTERACTIVE_PROMPT:")) {
                                                val promptText =
                                                        error.substringAfter("INTERACTIVE_PROMPT:")
                                                                .trim()
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    onOutput("[需要输入] $promptText")
                                                }
                                            } else {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    onOutput(error)
                                                }
                                            }
                                        }

                                        override fun onComplete(result: CommandResult) {
                                            // 已在resultCallback中处理
                                        }

                                        override fun onError(error: String, exitCode: Int) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onOutput("错误: $error (退出码: $exitCode)")
                                            }
                                        }
                                    }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 安装 Ruby 环境
suspend fun installRuby(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始安装 Ruby...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg install ruby -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("Ruby 安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg install ruby -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("Ruby 安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 安装 Go 环境
suspend fun installGo(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始安装 Go...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg install golang -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("Go 安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg install golang -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("Go 安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}

// 安装 Rust 环境
suspend fun installRust(
        context: Context,
        sessionId: String?,
        onOutput: (String) -> Unit,
        onInteractivePrompt: (String, Int) -> Unit,
        onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        onOutput("开始安装 Rust...")

        // 获取会话
        val session = sessionId?.let { TerminalSessionManager.sessions.find { s -> s.id == it } }

        if (session != null) {
            // 使用终端会话执行命令
            TerminalSessionManager.executeSessionCommand(
                    context,
                    session,
                    "pkg update -y && pkg install rust -y",
                    onOutput,
                    onInteractivePrompt,
                    onComplete = { exitCode, success ->
                        onOutput("Rust 安装" + if (success) "成功" else "失败")
                        onComplete(success)
                    }
            )
        } else {
            // 回退到直接执行命令
            val result =
                    TermuxCommandExecutor.executeCommand(
                            context = context,
                            command = "pkg update -y && pkg install rust -y",
                            resultCallback = { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    onOutput("Rust 安装" + if (result.success) "成功" else "失败")
                                    onComplete(result.success)
                                }
                            }
                    )

            if (!result.success) {
                CoroutineScope(Dispatchers.Main).launch {
                    onOutput("命令发送失败: ${result.stderr}")
                    onComplete(false)
                }
            }
        }
    }
}
