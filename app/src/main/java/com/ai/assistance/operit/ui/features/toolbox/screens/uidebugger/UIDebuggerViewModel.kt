package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.core.tools.UIPageResultData
import com.ai.assistance.operit.core.tools.automatic.UIEdgeDefinition
import com.ai.assistance.operit.core.tools.automatic.config.AutomationPackageManager
import com.ai.assistance.operit.core.tools.automatic.UIRouteConfig
import com.ai.assistance.operit.core.tools.automatic.config.AutomationPackageInfo
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.core.tools.system.action.ActionListener
import com.ai.assistance.operit.core.tools.system.action.ActionListenerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.util.UUID
import com.ai.assistance.operit.core.tools.automatic.UINode
import com.ai.assistance.operit.core.tools.automatic.UINodeType
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import com.ai.assistance.operit.core.tools.automatic.UIFunction
import com.ai.assistance.operit.core.tools.automatic.ValidationType

/** UI调试工具的ViewModel，负责处理与AITool的交互 */
class UIDebuggerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UIDebuggerState())
    val uiState: StateFlow<UIDebuggerState> = _uiState.asStateFlow()

    private lateinit var toolHandler: AIToolHandler
    private lateinit var packageManager: AutomationPackageManager
    private var statusBarHeight: Int = 0
    private val TAG = "UIDebuggerViewModel"
    private var windowInteractionController: ((Boolean) -> Unit)? = null
    private lateinit var context: Context
    
    // Activity监听相关
    private var currentActionListener: ActionListener? = null
    private var lastEventTimestamp: Long = 0
    private var connectionCheckJob: kotlinx.coroutines.Job? = null

    companion object {
        @Volatile
        private var INSTANCE: UIDebuggerViewModel? = null
        
        /**
         * 获取单例实例，确保主应用和悬浮窗使用同一个ViewModel
         */
        fun getInstance(): UIDebuggerViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UIDebuggerViewModel().also { INSTANCE = it }
            }
        }
        
        /**
         * 清除单例实例
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }

    /**
     * 设置窗口交互控制器
     */
    fun setWindowInteractionController(controller: ((Boolean) -> Unit)?) {
        this.windowInteractionController = controller
    }

    /** 初始化ViewModel */
    fun initialize(context: Context) {
        this.context = context
        toolHandler = AIToolHandler.getInstance(context)
        packageManager = AutomationPackageManager.getInstance(context)
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        loadAvailablePackages()
    }

    /** 加载可用的自动化配置包 */
    fun loadAvailablePackages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPackages = true) }
                val packages = withContext(Dispatchers.IO) {
                    packageManager.getAllPackageInfo()
                }
                _uiState.update { 
                    it.copy(
                        builtInPackages = packages.filter { it.isBuiltIn },
                        externalPackages = packages.filter { !it.isBuiltIn },
                        isLoadingPackages = false,
                        errorMessage = null
                    )
                }
                Log.d(TAG, "加载了 ${packages.size} 个配置包")
            } catch (e: Exception) {
                Log.e(TAG, "加载配置包失败", e)
                _uiState.update { 
                    it.copy(
                        isLoadingPackages = false,
                        errorMessage = "加载配置包失败"
                    )
                }
            }
        }
    }

    /** 选择配置包并加载其节点信息 */
    fun selectPackage(packageInfo: AutomationPackageInfo) {
        viewModelScope.launch {
            try {
                val config = packageManager.getConfigByAppPackageName(packageInfo.packageName)
                if (config != null) {
                    _uiState.update {
                        it.copy(
                            selectedPackage = packageInfo,
                            packageConfig = config,
                            packageNodes = config.nodeDefinitions.values.toList(),
                            // 默认选中第一个节点
                            selectedNodeName = config.nodeDefinitions.values.firstOrNull()?.name,
                            errorMessage = null
                        )
                    }
                    // No need for feedback here, the dialog opening is the feedback
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "无法加载 ${packageInfo.name} 的配置。",
                            selectedPackage = packageInfo, // Still show it's selected
                            packageConfig = null,
                            packageNodes = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "选择配置包失败", e)
                showActionFeedback("选择配置包失败")
            }
        }
    }

    /** 清除所选的配置包 */
    fun clearSelectedPackage() {
        _uiState.update {
            it.copy(
                selectedPackage = null,
                packageConfig = null,
                packageNodes = emptyList(),
                selectedNodeName = null
            )
        }
    }

    /** 显示/隐藏配置包选择对话框 */
    fun togglePackageDialog() {
        _uiState.update { it.copy(showPackageDialog = !it.showPackageDialog) }
    }

    /** 显示/隐藏导入导出对话框 */
    fun toggleImportExportDialog() {
        _uiState.update { it.copy(showImportExportDialog = !it.showImportExportDialog) }
    }

    /** 设置导入导出模式 */
    fun setImportExportMode(mode: ImportExportMode) {
        _uiState.update { it.copy(importExportMode = mode) }
    }

    /** 从外部文件导入配置包 */
    fun importPackageFromFile(filePath: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    packageManager.importPackage(filePath)
                }
                showActionFeedback(result)
                // 重新加载配置包列表
                loadAvailablePackages()
            } catch (e: Exception) {
                Log.e(TAG, "导入配置包失败", e)
                showActionFeedback("导入失败")
            }
        }
    }

    /** 准备导出，将包信息存储到State中 */
    fun prepareExport(packageInfo: AutomationPackageInfo) {
        _uiState.update { it.copy(selectedPackageForExport = packageInfo) }
    }

    /** 导出配置包到文件 */
    fun exportPackageToFile(packageInfo: AutomationPackageInfo, uriString: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    packageManager.exportPackage(packageInfo, uriString)
                }
                showActionFeedback("导出成功: $uriString")
            } catch (e: Exception) {
                Log.e(TAG, "导出配置包失败", e)
                showActionFeedback("导出失败: ${e.message}")
            }
        }
    }

    /** 选择UI元素 */
    fun selectElement(elementId: String?) {
        _uiState.update { it.copy(selectedElementId = elementId) }
    }

    /** 执行元素操作 */
    fun performElementAction(element: UIElement, action: UIElementAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    UIElementAction.CLICK -> {
                        val clickTool = AITool(
                            name = "click_element",
                            parameters = listOf(
                                com.ai.assistance.operit.data.model.ToolParameter(
                                    name = "selector_type",
                                    value = if (element.resourceId != null) "ByResourceId" else "ByText"
                                ),
                                com.ai.assistance.operit.data.model.ToolParameter(
                                    name = "selector_value",
                                    value = element.resourceId ?: element.text
                                )
                            )
                        )
                        val result = withContext(Dispatchers.IO) {
                            toolHandler.executeTool(clickTool)
                        }
                        showActionFeedback(if (result.success) "点击成功" else "点击失败")
                    }
                    UIElementAction.HIGHLIGHT -> {
                        selectElement(element.id)
                        showActionFeedback("已高亮元素")
                    }
                    UIElementAction.INSPECT -> {
                        selectElement(element.id)
                        showActionFeedback("已选择元素")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "执行元素操作失败", e)
                showActionFeedback("操作失败")
            }
        }
    }

    /** 显示操作反馈 */
    private fun showActionFeedback(message: String) {
        _uiState.update { 
            it.copy(
                showActionFeedback = true, 
                actionFeedbackMessage = message
            ) 
        }
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(showActionFeedback = false) }
        }
    }

    /** 刷新UI元素 */
    fun refreshUI() {
        viewModelScope.launch {
            try {
                windowInteractionController?.invoke(false)
                delay(300)

                val (elements, activityInfo) = withContext(Dispatchers.IO) {
                    val pageInfoTool = AITool(name = "get_page_info", parameters = listOf())
                    val result = toolHandler.executeTool(pageInfoTool)
                    if (result.success) {
                        val resultData = result.result
                        if (resultData is UIPageResultData) {
                            val currentActivityName = resultData.activityName
                            val currentPackageName = resultData.packageName
                            val elements = convertToUIElements(resultData.uiElements, currentActivityName, currentPackageName)
                            Pair(elements, Pair(currentActivityName, currentPackageName))
                        } else {
                            throw Exception("返回数据类型错误")
                        }
                    } else {
                        throw Exception("获取UI信息失败")
                    }
                }

                _uiState.update { 
                    it.copy(
                        elements = elements, 
                        errorMessage = null,
                        currentAnalyzedActivityName = activityInfo.first,
                        currentAnalyzedPackageName = activityInfo.second
                    ) 
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新UI元素失败", e)
                _uiState.update { it.copy(errorMessage = "刷新失败") }
            } finally {
                windowInteractionController?.invoke(true)
            }
        }
    }

    /**
     * 设置当前选中的节点。
     * @param nodeName 要选中的节点的名称。
     */
    fun selectNode(nodeName: String) {
        _uiState.update { it.copy(selectedNodeName = nodeName) }
    }

    /**
     * 设置视图模式（节点或功能）。
     * @param mode 要设置的视图模式。
     */
    fun setViewMode(mode: UIDebuggerViewMode) {
        _uiState.update { it.copy(currentViewMode = mode) }
    }

    /**
     * 显示操作详情对话框
     * @param edge 要显示详情的边
     * @param fromNodeName 起始节点名称
     */
    fun showOperationDetails(edge: UIEdgeDefinition, fromNodeName: String) {
        _uiState.update { 
            it.copy(
                showOperationDetailsDialog = true,
                selectedEdgeForDetails = edge,
                selectedFromNodeForDetails = fromNodeName
            ) 
        }
    }

    /**
     * 隐藏操作详情对话框
     */
    fun hideOperationDetails() {
        _uiState.update { 
            it.copy(
                showOperationDetailsDialog = false,
                selectedEdgeForDetails = null,
                selectedFromNodeForDetails = null
            ) 
        }
    }

    /**
     * 处理功能点击，跳转到对应的节点
     * @param function 被点击的功能
     */
    fun onFunctionClick(function: com.ai.assistance.operit.core.tools.automatic.UIFunction) {
        // 切换到节点视图
        _uiState.update { it.copy(currentViewMode = UIDebuggerViewMode.NODES) }
        // 选择目标节点
        selectNode(function.targetNodeName)
    }

    /**
     * 获取指定节点的功能列表
     * @param nodeName 节点名称
     * @return 该节点的功能列表
     */
    fun getFunctionsForNode(nodeName: String): List<com.ai.assistance.operit.core.tools.automatic.UIFunction> {
        return _uiState.value.packageConfig?.functionDefinitions?.values?.filter { function ->
            function.targetNodeName == nodeName
        } ?: emptyList()
    }

    /**
     * 显示功能详情对话框
     * @param function 要显示详情的功能
     */
    fun showFunctionDetails(function: com.ai.assistance.operit.core.tools.automatic.UIFunction) {
        _uiState.update { 
            it.copy(
                showFunctionDetailsDialog = true,
                selectedFunctionForDetails = function
            ) 
        }
    }

    /**
     * 隐藏功能详情对话框
     */
    fun hideFunctionDetails() {
        _uiState.update { 
            it.copy(
                showFunctionDetailsDialog = false,
                selectedFunctionForDetails = null
            ) 
        }
    }

    // 编辑相关方法

    /**
     * 显示/隐藏创建包对话框
     */
    fun toggleCreatePackageDialog() {
        _uiState.update { it.copy(showCreatePackageDialog = !it.showCreatePackageDialog) }
    }

    /**
     * 创建新的配置包
     */
    fun createNewPackage(appName: String, packageName: String, description: String) {
        viewModelScope.launch {
            try {
                val newPackageInfo = withContext(Dispatchers.IO) {
                    packageManager.createNewPackage(appName, packageName, description)
                }
                showActionFeedback("配置包创建成功")
                loadAvailablePackages()
                selectPackage(newPackageInfo)
            } catch (e: Exception) {
                Log.e(TAG, "创建配置包失败", e)
                showActionFeedback("创建失败: ${e.message}")
            }
        }
    }

    /**
     * 开始编辑节点
     */
    fun startEditNode(node: UINode? = null) {
        _uiState.update {
            it.copy(
                editMode = if (node == null) EditMode.ADD else EditMode.EDIT,
                editTarget = EditTarget.NODE,
                showEditDialog = true,
                editingNode = node
            )
        }
    }

    /**
     * 开始编辑边
     */
    fun startEditEdge(fromNodeName: String, edge: UIEdgeDefinition? = null) {
        val editingEdge = if (edge == null) {
            EditingEdge(fromNodeName = fromNodeName)
        } else {
            EditingEdge(
                fromNodeName = fromNodeName,
                originalEdge = edge,
                toNodeName = edge.toNodeName,
                operations = edge.operations,
                validation = edge.validation,
                conditions = edge.conditions,
                weight = edge.weight
            )
        }
        
        _uiState.update {
            it.copy(
                editMode = if (edge == null) EditMode.ADD else EditMode.EDIT,
                editTarget = EditTarget.EDGE,
                showEditDialog = true,
                editingEdge = editingEdge
            )
        }
    }

    /**
     * 开始编辑功能
     */
    fun startEditFunction(function: UIFunction? = null) {
        _uiState.update {
            it.copy(
                editMode = if (function == null) EditMode.ADD else EditMode.EDIT,
                editTarget = EditTarget.FUNCTION,
                showEditDialog = true,
                editingFunction = function
            )
        }
    }

    /**
     * 隐藏编辑对话框
     */
    fun hideEditDialog() {
        _uiState.update {
            it.copy(
                showEditDialog = false,
                editMode = EditMode.NONE,
                editingNode = null,
                editingEdge = null,
                editingFunction = null
            )
        }
    }

    /**
     * 保存节点（新建或更新）
     */
    fun saveNode(name: String, activityName: String?, nodeType: UINodeType, matchCriteria: List<UISelector>) {
        val currentState = _uiState.value
        val config = currentState.packageConfig ?: return
        val originalName = if (currentState.editMode == EditMode.EDIT) currentState.editingNode?.name else null
        val packageName = currentState.selectedPackage?.packageName ?: return

        val newNode = UINode(
            name = name,
            packageName = packageName,
            activityName = activityName,
            nodeType = nodeType,
            matchCriteria = matchCriteria
        )

        // 更新配置
        if (originalName != null && originalName != name) {
            // 如果重命名了节点，需要处理相关引用
            renameNodeInConfig(config, originalName, name)
            config.defineNode(newNode)
        } else {
            config.defineNode(newNode)
        }
        
        // 如果是新增，且是第一个节点，则自动选中
        val newSelectedNode = if (currentState.editMode == EditMode.ADD && currentState.packageNodes.isEmpty()) {
            name
        } else {
            currentState.selectedNodeName
        }

        _uiState.value = currentState.copy(
            packageConfig = config,
            packageNodes = config.nodeDefinitions.values.toList(),
            showEditDialog = false,
            editMode = EditMode.NONE,
            isConfigModified = true,
            selectedNodeName = newSelectedNode
        )

        viewModelScope.launch {
            saveConfig()
        }
    }

    /**
     * 删除节点
     */
    fun deleteNode(nodeName: String) {
        val currentState = _uiState.value
        val packageInfo = currentState.selectedPackage ?: return
        val config = currentState.packageConfig ?: return

        // 删除节点
        config.nodeDefinitions.remove(nodeName)
        
        // 删除相关的边
        config.edgeDefinitions.remove(nodeName)
        config.edgeDefinitions.values.forEach { edges ->
            edges.removeAll { it.toNodeName == nodeName }
        }
        
        // 删除相关的功能
        config.functionDefinitions.values.removeAll { it.targetNodeName == nodeName }

        saveConfigAndRefresh(config, packageInfo)
        showActionFeedback("节点已删除")
    }

    /**
     * 保存边
     */
    fun saveEdge(editingEdge: EditingEdge) {
        val currentState = _uiState.value
        val packageInfo = currentState.selectedPackage ?: return
        val config = currentState.packageConfig ?: return

        // 如果是编辑模式，先删除旧边
        if (currentState.editMode == EditMode.EDIT && editingEdge.originalEdge != null) {
            val edges = config.edgeDefinitions[editingEdge.fromNodeName]
            edges?.remove(editingEdge.originalEdge)
        }

        // 添加新边
        config.defineEdge(
            fromNodeName = editingEdge.fromNodeName,
            toNodeName = editingEdge.toNodeName,
            operations = editingEdge.operations,
            validation = editingEdge.validation,
            conditions = editingEdge.conditions,
            weight = editingEdge.weight
        )

        saveConfigAndRefresh(config, packageInfo)
        hideEditDialog()
    }

    /**
     * 删除边
     */
    fun deleteEdge(fromNodeName: String, edge: UIEdgeDefinition) {
        val currentState = _uiState.value
        val packageInfo = currentState.selectedPackage ?: return
        val config = currentState.packageConfig ?: return

        val edges = config.edgeDefinitions[fromNodeName]
        edges?.remove(edge)

        saveConfigAndRefresh(config, packageInfo)
        showActionFeedback("边已删除")
    }

    /**
     * 保存功能
     */
    fun saveFunction(name: String, description: String, targetNodeName: String, operation: UIOperation) {
        val currentState = _uiState.value
        val packageInfo = currentState.selectedPackage ?: return
        val config = currentState.packageConfig ?: return

        // 如果是编辑模式，删除旧功能
        if (currentState.editMode == EditMode.EDIT && currentState.editingFunction != null) {
            val oldFunctionName = currentState.editingFunction!!.name
            config.functionDefinitions.remove(oldFunctionName)
        }

        val newFunction = UIFunction(
            name = name,
            description = description,
            targetNodeName = targetNodeName,
            operation = operation
        )

        config.defineFunction(newFunction)
        saveConfigAndRefresh(config, packageInfo)
        hideEditDialog()
    }

    /**
     * 删除功能
     */
    fun deleteFunction(functionName: String) {
        val currentState = _uiState.value
        val packageInfo = currentState.selectedPackage ?: return
        val config = currentState.packageConfig ?: return

        config.functionDefinitions.remove(functionName)
        saveConfigAndRefresh(config, packageInfo)
        showActionFeedback("功能已删除")
    }

    /**
     * 更新节点引用
     */
    private fun updateReferencesToNode(config: UIRouteConfig, oldName: String, newName: String) {
        // 更新边中的引用
        config.edgeDefinitions[oldName]?.let { edges ->
            config.edgeDefinitions[newName] = edges
            config.edgeDefinitions.remove(oldName)
        }
        
        config.edgeDefinitions.values.forEach { edges ->
            val updatedEdges = edges.map { edge ->
                if (edge.toNodeName == oldName) {
                    edge.copy(toNodeName = newName)
                } else {
                    edge
                }
            }
            edges.clear()
            edges.addAll(updatedEdges)
        }
        
        // 更新功能中的引用
        val updatedFunctions = config.functionDefinitions.values.map { function ->
            if (function.targetNodeName == oldName) {
                function.copy(targetNodeName = newName)
            } else {
                function
            }
        }
        config.functionDefinitions.clear()
        updatedFunctions.forEach { config.defineFunction(it) }
    }

    /**
     * 保存配置并刷新
     */
    private fun saveConfigAndRefresh(config: UIRouteConfig, packageInfo: AutomationPackageInfo) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    packageManager.saveConfig(config, packageInfo)
                }
                showActionFeedback(result)
                // 重新选择包以刷新数据
                selectPackage(packageInfo)
                _uiState.update { it.copy(isConfigModified = false) }
            } catch (e: Exception) {
                Log.e(TAG, "保存配置失败", e)
                showActionFeedback("保存失败: ${e.message}")
            }
        }
    }

    /**
     * 标记配置已修改
     */
    fun markConfigModified() {
        _uiState.update { it.copy(isConfigModified = true) }
    }

    /**
     * 创建简单的点击操作
     */
    fun createClickOperation(selectorType: String, selectorValue: String, description: String): UIOperation.Click {
        val selector = when (selectorType) {
            "ByResourceId" -> UISelector.ByResourceId(selectorValue)
            "ByText" -> UISelector.ByText(selectorValue)
            "ByContentDesc" -> UISelector.ByContentDesc(selectorValue)
            "ByClassName" -> UISelector.ByClassName(selectorValue)
            "ByBounds" -> UISelector.ByBounds(selectorValue)
            "ByXPath" -> UISelector.ByXPath(selectorValue)
            else -> UISelector.ByText(selectorValue)
        }
        return UIOperation.Click(selector, description)
    }

    /**
     * 创建输入操作
     */
    fun createInputOperation(selectorType: String, selectorValue: String, textVariableKey: String): UIOperation.Input {
        val selector = when (selectorType) {
            "ByResourceId" -> UISelector.ByResourceId(selectorValue)
            "ByText" -> UISelector.ByText(selectorValue)
            "ByContentDesc" -> UISelector.ByContentDesc(selectorValue)
            "ByClassName" -> UISelector.ByClassName(selectorValue)
            "ByBounds" -> UISelector.ByBounds(selectorValue)
            "ByXPath" -> UISelector.ByXPath(selectorValue)
            else -> UISelector.ByText(selectorValue)
        }
        return UIOperation.Input(selector, textVariableKey)
    }

    private fun convertToUIElements(
        node: com.ai.assistance.operit.core.tools.SimplifiedUINode,
        activityName: String? = null,
        packageName: String? = null
    ): List<UIElement> {
        val elements = mutableListOf<UIElement>()
        processNode(node, elements, activityName, packageName)
        return elements
    }

    private fun processNode(
        node: com.ai.assistance.operit.core.tools.SimplifiedUINode,
        elements: MutableList<UIElement>,
        activityName: String? = null,
        packageName: String? = null
    ) {
        val element = createUiElement(node, activityName, packageName)
        elements.add(element)
        node.children.forEach { childNode -> processNode(childNode, elements, activityName, packageName) }
    }

    private fun createUiElement(
        node: com.ai.assistance.operit.core.tools.SimplifiedUINode,
        activityName: String? = null,
        packageName: String? = null
    ): UIElement {
        val bounds = node.bounds?.let {
            try {
                val coordsPattern = "\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]".toRegex()
                val matchResult = coordsPattern.find(it)
                if (matchResult != null) {
                    val (left, top, right, bottom) = matchResult.destructured
                    Rect(left.toInt(), top.toInt() - statusBarHeight, right.toInt(), bottom.toInt() - statusBarHeight)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析边界失败", e)
                null
            }
        }

        return UIElement(
            id = UUID.randomUUID().toString(),
            className = node.className ?: "Unknown",
            resourceId = node.resourceId,
            contentDesc = node.contentDesc,
            text = node.text ?: "",
            bounds = bounds,
            isClickable = node.isClickable,
            activityName = activityName,
            packageName = packageName
        )
    }

    // Activity监听相关方法

    /**
     * 启动Activity监听
     */
    fun startActivityListening() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "开始启动Activity监听...")
                
                // 如果已经在监听，直接返回
                if (currentActionListener?.isListening() == true) {
                    Log.d(TAG, "监听器已在运行，同步UI状态")
                    _uiState.update { it.copy(isActivityListening = true) } // 同步UI状态
                    showActionFeedback("监听已在运行中")
                    return@launch
                }

                // 先停止现有的监听器，避免重复监听
                currentActionListener?.let { existingListener ->
                    Log.d(TAG, "停止现有监听器")
                    existingListener.stopListening()
                    currentActionListener = null
                }

                Log.d(TAG, "获取最高权限的监听器...")
                val (listener, status) = ActionListenerFactory.getHighestAvailableListener(context)
                Log.d(TAG, "获取到监听器类型: ${listener::class.simpleName}, 权限状态: ${status.granted}")
                
                if (!status.granted) {
                    Log.w(TAG, "权限不足: ${status.reason}")
                    showActionFeedback("权限不足: ${status.reason}")
                    return@launch
                }

                currentActionListener = listener
                Log.d(TAG, "开始启动监听器...")
                val result = listener.startListening { event ->
                    Log.v(TAG, "监听器回调触发: ${event.actionType} - ${event.elementInfo?.packageName}")
                    // 处理监听到的事件
                    handleActionEvent(event)
                }

                if (result.success) {
                    Log.d(TAG, "监听器启动成功")
                    _uiState.update { 
                        it.copy(
                            isActivityListening = true,
                            showActivityMonitor = true,
                            activityEvents = emptyList() // 清空之前的事件
                        ) 
                    }
                    showActionFeedback("Activity监听已启动")
                } else {
                    Log.e(TAG, "监听器启动失败: ${result.message}")
                    showActionFeedback("启动监听失败: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动Activity监听失败", e)
                showActionFeedback("启动监听失败: ${e.message}")
            }
        }
    }

    /**
     * 停止Activity监听
     */
    fun stopActivityListening() {
        viewModelScope.launch {
            try {
                val stopped = currentActionListener?.stopListening() ?: false
                if (stopped) {
                    _uiState.update { 
                        it.copy(
                            isActivityListening = false
                        ) 
                    }
                    showActionFeedback("Activity监听已停止")
                } else {
                    showActionFeedback("停止监听失败")
                }
                currentActionListener = null
            } catch (e: Exception) {
                Log.e(TAG, "停止Activity监听失败", e)
                showActionFeedback("停止监听失败: ${e.message}")
            }
        }
    }

    /**
     * 切换自动构建图模式
     */
    fun toggleAutoGraphBuilding() {
        _uiState.update { currentState ->
            val newAutoMode = !currentState.autoGraphBuilding
            Log.d(TAG, "切换自动构建模式: $newAutoMode")
            
            if (newAutoMode && currentState.selectedPackage != null) {
                // 检测当前包名
                val detectedPackage = currentState.selectedPackage?.packageName
                Log.d(TAG, "自动构建模式启用，目标包名: $detectedPackage")
                
                currentState.copy(
                    autoGraphBuilding = newAutoMode,
                    detectedCurrentPackageName = detectedPackage,
                    autoGeneratedNodes = 0,
                    autoGeneratedEdges = 0,
                    lastActivityName = null
                )
            } else {
                currentState.copy(
                    autoGraphBuilding = newAutoMode,
                    // 关闭时不重置detectedCurrentPackageName，保留上次设置
                    lastActivityName = null,
                    autoGeneratedNodes = 0,
                    autoGeneratedEdges = 0,
                    lastClickEvent = null
                )
            }
        }
    }

    /**
     * 切换Activity监听显示状态
     */
    fun toggleActivityMonitor() {
        Log.d(TAG, "切换Activity监听面板显示状态")
        _uiState.update { currentState ->
            val newShowState = !currentState.showActivityMonitor
            Log.d(TAG, "面板显示状态从 ${currentState.showActivityMonitor} 变更为 $newShowState")
            
            // 如果要显示面板，同步检查实际的监听状态
            if (newShowState) {
                val actualListeningState = currentActionListener?.isListening() == true
                val eventsCount = currentState.activityEvents.size
                Log.d(TAG, "显示面板时同步状态: currentActionListener=${currentActionListener != null}, isListening=$actualListeningState, eventsCount=$eventsCount")
                
                // 检查AIDL连接状态
                if (currentActionListener != null && !actualListeningState) {
                    Log.w(TAG, "检测到监听器存在但未监听，可能AIDL连接断开")
                }
                
                currentState.copy(
                    showActivityMonitor = newShowState,
                    isActivityListening = actualListeningState
                )
            } else {
                Log.d(TAG, "隐藏面板，保持监听状态不变")
                currentState.copy(showActivityMonitor = newShowState)
            }
        }
    }

    /**
     * 清除Activity事件记录
     */
    fun clearActivityEvents() {
        _uiState.update { 
            it.copy(
                activityEvents = emptyList(),
                currentActivityName = null
            ) 
        }
    }

    /**
     * 处理监听到的Action事件
     */
    private fun handleActionEvent(event: ActionListener.ActionEvent) {
        viewModelScope.launch {
            // 过滤掉自己软件的事件
            val currentPackageName = context.packageName
            if (event.elementInfo?.packageName == currentPackageName) {
                return@launch
            }
            _uiState.update { state ->
                val newEvents = (state.activityEvents + event).takeLast(100) // 保留最近100个事件
                
                // 更新当前活动名称
                val currentActivity = event.elementInfo?.let { elementInfo ->
                    if (elementInfo.packageName != null && elementInfo.className != null) {
                        "${elementInfo.packageName}/${elementInfo.className}"
                    } else {
                        elementInfo.packageName
                    }
                }
                
                // 自动构建图逻辑
                var updatedState = state.copy(
                    activityEvents = newEvents,
                    currentActivityName = currentActivity ?: state.currentActivityName
                )
                
                // 如果启用了自动构建且当前事件是目标应用的事件
                Log.d(TAG, "自动构建检查: autoGraphBuilding=${state.autoGraphBuilding}, detectedPackage=${state.detectedCurrentPackageName}, eventPackage=${event.elementInfo?.packageName}")
                if (state.autoGraphBuilding && 
                    state.detectedCurrentPackageName != null &&
                    event.elementInfo?.packageName == state.detectedCurrentPackageName) {
                    
                    Log.d(TAG, "触发自动构建逻辑: ${event.actionType}")
                    // 监听页面切换和关键的点击事件
                    when (event.actionType) {
                        ActionListener.ActionType.SCREEN_CHANGE -> {
                            Log.d(TAG, "处理SCREEN_CHANGE事件进行自动构建")
                            updatedState = processAutoGraphBuilding(updatedState, event)
                        }
                        ActionListener.ActionType.CLICK -> {
                            Log.d(TAG, "记录CLICK事件为后续页面切换准备")
                            // 记录点击事件，为后续的页面切换做准备
                            updatedState = updatedState.copy(
                                lastClickEvent = event
                            )
                        }
                        else -> { 
                            Log.d(TAG, "忽略其他类型事件: ${event.actionType}")
                        }
                    }
                } else {
                    Log.d(TAG, "不满足自动构建条件，跳过")
                }
                
                updatedState
            }
        }
    }

    /**
     * 处理自动构建图逻辑
     */
    private fun processAutoGraphBuilding(
        state: UIDebuggerState,
        event: ActionListener.ActionEvent
    ): UIDebuggerState {
        val elementInfo = event.elementInfo ?: return state
        val currentConfig = state.packageConfig ?: return state
        val currentActivity = elementInfo.className ?: return state
        
        Log.d(TAG, "处理自动构建: 从 ${state.lastActivityName} 到 $currentActivity")
        
        // 生成节点名称（简化版本）
        val nodeName = currentActivity.substringAfterLast(".")
        var addedNodes = 0
        var addedEdges = 0
        
        // 检查节点是否已存在，如果不存在则创建
        if (!currentConfig.nodeDefinitions.containsKey(nodeName)) {
            val newNode = UINode(
                name = nodeName,
                packageName = elementInfo.packageName ?: "",
                activityName = currentActivity,
                nodeType = UINodeType.LIST_PAGE
            )
            currentConfig.defineNode(newNode)
            addedNodes++
            Log.d(TAG, "自动创建节点: $nodeName")
        }
        
        // 如果有上一个Activity，创建边
        if (state.lastActivityName != null) {
            val lastNodeName = state.lastActivityName.substringAfterLast(".")
            
            // 检查上一个节点是否存在，如果不存在则创建
            if (!currentConfig.nodeDefinitions.containsKey(lastNodeName)) {
                val lastNode = UINode(
                    name = lastNodeName,
                    packageName = elementInfo.packageName ?: "",
                    activityName = state.lastActivityName,
                    nodeType = UINodeType.LIST_PAGE
                )
                currentConfig.defineNode(lastNode)
                addedNodes++
                Log.d(TAG, "自动创建上一个节点: $lastNodeName")
            }
            
            // 检查边是否已存在
            val existingEdges = currentConfig.edgeDefinitions[lastNodeName] ?: mutableListOf()
            val edgeExists = existingEdges.any { it.toNodeName == nodeName }
            
            if (!edgeExists && lastNodeName != nodeName) {
                // 创建基于用户操作的边，优先使用点击事件
                val operations = if (state.lastClickEvent != null) {
                    createOperationsFromEvent(state.lastClickEvent)
                } else {
                    createOperationsFromEvent(event)
                }
                
                currentConfig.defineEdge(
                    fromNodeName = lastNodeName,
                    toNodeName = nodeName,
                    operations = operations,
                    validation = null,
                    conditions = emptySet(),
                    weight = 1.0
                )
                addedEdges++
                Log.d(TAG, "自动创建边: $lastNodeName -> $nodeName，基于${if (state.lastClickEvent != null) "点击" else "页面切换"}事件")
            }
        }
        
        // 如果有变化，保存配置
        if (addedNodes > 0 || addedEdges > 0) {
            state.selectedPackage?.let { packageInfo ->
                saveConfigSilently(currentConfig, packageInfo)
            }
        }
        
        return state.copy(
            lastActivityName = currentActivity,
            autoGeneratedNodes = state.autoGeneratedNodes + addedNodes,
            autoGeneratedEdges = state.autoGeneratedEdges + addedEdges,
            packageNodes = currentConfig.nodeDefinitions.values.toList(), // 更新节点列表
            lastClickEvent = null // 清除已使用的点击事件
        )
    }

    /**
     * 根据事件创建操作序列
     */
    private fun createOperationsFromEvent(event: ActionListener.ActionEvent): List<UIOperation> {
        val operations = mutableListOf<UIOperation>()
        
        when (event.actionType) {
            ActionListener.ActionType.CLICK -> {
                event.elementInfo?.let { elementInfo ->
                    val selector = when {
                        !elementInfo.resourceId.isNullOrBlank() -> 
                            UISelector.ByResourceId(elementInfo.resourceId)
                        !elementInfo.text.isNullOrBlank() -> 
                            UISelector.ByText(elementInfo.text)
                        !elementInfo.contentDescription.isNullOrBlank() -> 
                            UISelector.ByContentDesc(elementInfo.contentDescription)
                        else -> UISelector.ByText("未知元素")
                    }
                    
                    val description = when {
                        !elementInfo.text.isNullOrBlank() -> "点击 '${elementInfo.text}'"
                        !elementInfo.contentDescription.isNullOrBlank() -> "点击 '${elementInfo.contentDescription}'"
                        !elementInfo.resourceId.isNullOrBlank() -> "点击 ${elementInfo.resourceId}"
                        else -> "点击页面元素"
                    }
                    
                    operations.add(UIOperation.Click(selector, description))
                }
            }
            ActionListener.ActionType.TEXT_INPUT -> {
                operations.add(UIOperation.Wait(500L, "等待页面加载"))
            }
            else -> {
                operations.add(UIOperation.Wait(1000L, "等待页面切换"))
            }
        }
        
        return operations
    }

    /**
     * 静默保存配置（不显示反馈消息）
     */
    private fun saveConfigSilently(config: UIRouteConfig, packageInfo: AutomationPackageInfo) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    packageManager.saveConfig(config, packageInfo)
                }
                Log.d(TAG, "自动保存配置成功")
            } catch (e: Exception) {
                Log.e(TAG, "自动保存配置失败", e)
            }
        }
    }

    /**
     * 保存当前配置到文件
     */
    private suspend fun saveConfig() {
        val currentState = _uiState.value
        val config = currentState.packageConfig ?: return
        val packageInfo = currentState.selectedPackage ?: return
        try {
            packageManager.saveConfig(config, packageInfo)
            showActionFeedback("配置已保存")
            _uiState.value = _uiState.value.copy(isConfigModified = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "保存配置失败: ${e.message}")
        }
    }

    /**
     * 重命名节点时，更新所有对该节点的引用
     */
    private fun renameNodeInConfig(config: UIRouteConfig, oldName: String, newName: String) {
        // 更新边
        config.edgeDefinitions.forEach { (fromNode, edges) ->
            edges.forEach { edge ->
                if (edge.toNodeName == oldName) {
                    val newEdge = edge.copy(toNodeName = newName)
                    edges.remove(edge)
                    edges.add(newEdge)
                }
            }
            if (fromNode == oldName) {
                val newEdges = config.edgeDefinitions.remove(oldName)
                if (newEdges != null) {
                    config.edgeDefinitions[newName] = newEdges
                }
            }
        }
        
        // 更新功能
        config.functionDefinitions.forEach { (_, function) ->
            if (function.targetNodeName == oldName) {
                val newFunction = function.copy(targetNodeName = newName)
                config.functionDefinitions[function.name] = newFunction
            }
        }

        // 更新节点自身
        val node = config.nodeDefinitions.remove(oldName)
        if (node != null) {
            config.nodeDefinitions[newName] = node.copy(name = newName)
        }
    }

    /**
     * ViewModel清理时停止监听
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            currentActionListener?.stopListening()
            currentActionListener = null
        }
        // 注意：不要在这里清除单例实例，因为可能有其他地方还在使用
    }
}
