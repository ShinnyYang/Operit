package com.ai.assistance.operit.ui.features.toolbox.screens.tooltester

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ai.assistance.operit.ui.components.CustomScaffold
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.core.tools.StringResultData
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolParameter
import com.ai.assistance.operit.data.model.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/** 工具测试屏幕 - 最终版网格布局 + 中间弹窗详情 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTesterScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val aiToolHandler = remember { AIToolHandler.getInstance(context) }
    val focusRequester = remember { FocusRequester() }

    var testResults by remember { mutableStateOf<Map<String, ToolTestResult>>(emptyMap()) }
    var isTestingAll by remember { mutableStateOf(false) }
    var selectedTestForDetails by remember { mutableStateOf<ToolTest?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var testInputText by remember { mutableStateOf("") }

    val toolGroups = remember { getFinalToolTestGroups() }

    suspend fun runTest(toolTest: ToolTest) {
        // UI preparation phase on Main thread
        if (toolTest.id == "set_input_text" || toolTest.id == "click_element") {
            // If the dialog is showing for a single test run, dismiss it first.
            if (showDialog) {
                showDialog = false
                delay(300) // Wait for dialog animation to finish.
            }
            focusRequester.requestFocus()
            delay(100) // Wait for focus to be processed by the system.
        }

        // Mark test as running on Main thread
        testResults = testResults.toMutableMap().apply {
            put(toolTest.id, ToolTestResult(TestStatus.RUNNING, null))
        }

        // Execution phase on IO thread
        val result = withContext(Dispatchers.IO) {
            try {
                aiToolHandler.executeTool(AITool(toolTest.id, toolTest.parameters))
            } catch (e: Exception) {
                ToolResult(toolName = toolTest.id, success = false, result = StringResultData(""), error = e.message ?: "Unknown error")
            }
        }

        // Update UI with result on Main thread
        testResults = testResults.toMutableMap().apply {
            put(toolTest.id, ToolTestResult(if (result.success) TestStatus.SUCCESS else TestStatus.FAILED, result))
        }
    }

    CustomScaffold() { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header Section
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI工具可用性测试", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("所有AI工具已按功能分组，点击格子查看详情和操作。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    label = { Text("测试输入框 (用于UI测试)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("tool_tester_input")
                )

                Button(
                    onClick = {
                        scope.launch {
                            isTestingAll = true
                            testResults = emptyMap()
                            for (group in toolGroups.filter { !it.isManual }) {
                                if (group.sequential) {
                                    for (test in group.tests) { runTest(test) }
                                } else {
                                    val jobs = group.tests.map { test -> launch { runTest(test) } }
                                    jobs.forEach { it.join() }
                                }
                            }
                            isTestingAll = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTestingAll
                ) {
                    if (isTestingAll) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("正在执行批量测试...")
                        }
                    } else {
                        Text("开始全面自动化测试")
                    }
                }
            }
            Divider()
            // Grid Body
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 75.dp),
                modifier = Modifier.fillMaxWidth().height(800.dp), // 设定一个足够的高度以避免嵌套滚动问题
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                toolGroups.forEach { group ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(group.tests) { toolTest ->
                        ToolTestGridItem(
                            toolTest = toolTest,
                            testResult = testResults[toolTest.id]
                        ) {
                            selectedTestForDetails = it
                            showDialog = true
                        }
                    }
                }
            }
        }
    }

    if (showDialog && selectedTestForDetails != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                ToolDetailsSheet(
                    toolTest = selectedTestForDetails!!,
                    testResult = testResults[selectedTestForDetails!!.id],
                    scope = scope,
                    onTest = { test -> scope.launch { runTest(test); } },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

@Composable
fun ToolTestGridItem(toolTest: ToolTest, testResult: ToolTestResult?, onClick: (ToolTest) -> Unit) {
    val status = testResult?.status
    val color = when (status) {
        TestStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        TestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        TestStatus.RUNNING -> MaterialTheme.colorScheme.tertiaryContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        TestStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        TestStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        TestStatus.RUNNING -> MaterialTheme.colorScheme.onTertiaryContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.size(65.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = { onClick(toolTest) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(toolTest.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = contentColor, lineHeight = MaterialTheme.typography.labelSmall.fontSize * 1.1)
        }
    }
}

@Composable
fun ToolDetailsSheet(
    toolTest: ToolTest,
    testResult: ToolTestResult?,
    scope: CoroutineScope,
    onTest: (ToolTest) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val (icon, color) = when(testResult?.status) {
                TestStatus.SUCCESS -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
                TestStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
                TestStatus.RUNNING -> Icons.Default.HourglassTop to MaterialTheme.colorScheme.tertiary
                null -> Icons.Default.HelpOutline to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(icon, contentDescription = "Status", tint = color, modifier = Modifier.size(32.dp))
            Column {
                Text(toolTest.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(toolTest.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Divider()
        Text(toolTest.description, style = MaterialTheme.typography.bodyMedium)
        
        if (toolTest.parameters.isNotEmpty()) {
            Text("参数:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(8.dp)) {
                toolTest.parameters.forEach { Text(" • ${it.name}: ${it.value}", style = MaterialTheme.typography.bodyMedium) }
            }
        }

        testResult?.result?.let { result ->
            Text("详细结果:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            val resultText = (if (result.success) result.result.toString() else result.error ?: "未知错误").take(1000)
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(8.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = onDismiss) { Text("关闭") }
            Button(onClick = { onTest(toolTest) }, enabled = testResult?.status != TestStatus.RUNNING) {
                Text("重新测试")
            }
        }
    }
}


private fun getFinalToolTestGroups(): List<ToolGroup> {
    val testBaseDir = "/sdcard/Download/Operit/test"
    val testFile = "$testBaseDir/test_file.txt"
    val testFileCopy = "$testBaseDir/test_file_copy.txt"
    val testZip = "$testBaseDir/test.zip"
    val testUnzipDir = "$testBaseDir/unzipped"
    val testImage = "$testBaseDir/test_image.png"

    return listOf(
        ToolGroup("环境准备 (顺序执行)", true, false, listOf(
            ToolTest("make_directory", "创建测试目录", "创建所有测试文件的根目录。", listOf(ToolParameter("path", testBaseDir), ToolParameter("create_parents", "true"))),
            ToolTest("download_file", "下载测试图片", "下载一张图片用于后续测试。", listOf(ToolParameter("url", "https://picsum.photos/100"), ToolParameter("destination", testImage))),
            ToolTest("write_file", "创建文本文件", "创建一个用于测试的文本文件。", listOf(ToolParameter("path", testFile), ToolParameter("content", "This is a test file for Operit tool testing.")))
        )),
        ToolGroup("基础与HTTP (并行)", false, false, listOf(
            ToolTest("sleep", "延时", "测试工具调用延时", listOf(ToolParameter("duration_ms", "1000"))),
            ToolTest("device_info", "设备信息", "获取设备详细信息", emptyList()),
            ToolTest("http_request", "HTTP GET", "测试GET请求", listOf(ToolParameter("url", "https://httpbin.org/get"), ToolParameter("method", "GET"))),
            ToolTest("multipart_request", "文件上传", "测试上传文件到httpbin", listOf(ToolParameter("url", "https://httpbin.org/post"), ToolParameter("method", "POST"), ToolParameter("files", testFile))),
            ToolTest("manage_cookies", "管理Cookie", "获取google.com的Cookie", listOf(ToolParameter("action", "get"), ToolParameter("domain", "google.com"))),
            ToolTest("visit_web", "访问网页", "访问Baidu并提取内容", listOf(ToolParameter("url", "https://www.baidu.com"))),
            ToolTest("use_package", "使用包", "测试激活一个不存在的包", listOf(ToolParameter("package_name", "non_existent_package"))),
            ToolTest("query_knowledge_library", "查询问题库", "测试查询问题库", listOf(ToolParameter("query", "test")))
        )),
        ToolGroup("文件只读 (并行)", false, false, listOf(
            ToolTest("list_files", "列出文件", "列出测试目录的文件", listOf(ToolParameter("path", testBaseDir))),
            ToolTest("file_exists", "文件是否存在", "检查测试文件是否存在", listOf(ToolParameter("path", testFile))),
            ToolTest("read_file", "OCR读取", "读取测试图片内容", listOf(ToolParameter("path", testImage))),
            ToolTest("read_file_part", "分块读取", "读取测试文件的第一部分", listOf(ToolParameter("path", testFile), ToolParameter("partIndex", "0"))),
            ToolTest("file_info", "文件信息", "获取测试文件的信息", listOf(ToolParameter("path", testFile))),
            ToolTest("find_files", "查找文件", "在测试目录中查找文件", listOf(ToolParameter("path", testBaseDir), ToolParameter("pattern", "*.txt")))
        )),
        ToolGroup("文件写入 (顺序)", true, false, listOf(
            ToolTest("copy_file", "复制文件", "复制测试文件", listOf(ToolParameter("source", testFile), ToolParameter("destination", testFileCopy))),
            ToolTest("move_file", "移动文件", "移动并重命名文件", listOf(ToolParameter("source", testFileCopy), ToolParameter("destination", "$testBaseDir/moved_file.txt"))),
            ToolTest("zip_files", "压缩文件", "压缩测试目录", listOf(ToolParameter("source", testBaseDir), ToolParameter("destination", testZip))),
            ToolTest("unzip_files", "解压文件", "解压测试文件", listOf(ToolParameter("source", testZip), ToolParameter("destination", testUnzipDir))),
            ToolTest("convert_file", "格式转换", "将PNG转换为JPG", listOf(ToolParameter("source_path", testImage), ToolParameter("target_path", "$testBaseDir/converted.jpg")))
        )),
        ToolGroup("系统 (并行)", false, false, listOf(
            ToolTest("list_installed_apps", "列出应用", "获取用户安装的应用列表", listOf(ToolParameter("include_system_apps", "false"))),
            ToolTest("get_notifications", "获取通知", "获取最新的5条通知", listOf(ToolParameter("limit", "5"))),
            ToolTest("get_device_location", "设备位置", "获取低精度设备位置", listOf(ToolParameter("high_accuracy", "false"))),
            ToolTest("get_system_setting", "读系统设置", "获取屏幕关闭超时时间", listOf(ToolParameter("setting", "screen_off_timeout"))),
            ToolTest("modify_system_setting", "写系统设置", "尝试写入一个无害的设置项", listOf(ToolParameter("setting", "test_setting"), ToolParameter("value", "1"), ToolParameter("namespace", "system")))
        )),
        ToolGroup("UI自动化 (并行, 可能失败)", false, false, listOf(
            ToolTest("get_page_info", "页面信息", "获取当前屏幕的UI结构", emptyList()),
            ToolTest("press_key", "模拟按键", "模拟按下音量+", listOf(ToolParameter("key_code", "KEYCODE_VOLUME_UP"))),
            ToolTest("set_input_text", "文本输入", "在上方测试框输入文本", listOf(ToolParameter("text", "Hello from Operit!"))),
            ToolTest("click_element", "点击输入框(ID)", "通过为输入框设置的testTag作为ID进行点击，预期成功。", listOf(ToolParameter("resourceId", "tool_tester_input"))),
            ToolTest("tap", "模拟点击", "在(1,1)处点击，预期失败", listOf(ToolParameter("x", "1"), ToolParameter("y", "1"))),
            ToolTest("swipe", "模拟滑动", "在屏幕中央短距离滑动", listOf(ToolParameter("start_x", "500"), ToolParameter("start_y", "1000"), ToolParameter("end_x", "500"), ToolParameter("end_y", "1200")))
        )),
        ToolGroup("清理环境 (自动执行)", true, false, listOf(
            ToolTest("delete_file", "清理测试目录", "删除所有测试文件和目录。", listOf(ToolParameter("path", testBaseDir), ToolParameter("recursive", "true")))
        ))
    )
}


data class ToolTest(val id: String, val name: String, val description: String, val parameters: List<ToolParameter>)
data class ToolTestResult(val status: TestStatus, val result: ToolResult?)
enum class TestStatus { SUCCESS, FAILED, RUNNING }
data class ToolGroup(val name: String, val sequential: Boolean, val isManual: Boolean, val tests: List<ToolTest>)
