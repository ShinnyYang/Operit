package com.ai.assistance.operit.core.tools.automatic

import android.content.Context
import android.util.Log
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.core.tools.UIPageResultData
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolParameter
import com.ai.assistance.operit.util.map.*
import org.json.JSONObject
import kotlinx.coroutines.delay

/**
 * UI自动化路由器 - 系统的核心。
 * 负责路径规划、参数收集和执行协调。
 */
class UIRouter(
    private val context: Context,
    private val toolHandler: AIToolHandler
) {
    private val operationExecutor by lazy { UIOperationExecutor(context, toolHandler, routeConfig) }
    private var graph: StatefulGraph = StatefulGraph()
    private var pathFinder: StatefulPathFinder = StatefulPathFinder(graph)
    private var routeConfig: UIRouteConfig? = null

    companion object {
        private const val TAG = "UIRouter"
        private const val MAX_PAGE_INFO_RETRY = 3
        private const val PAGE_INFO_RETRY_DELAY = 500L
    }

    /**
     * 加载路由配置来构建图。
     * @param config UIRouteConfig的实例。
     * @param merge 如果为true，则将新配置合并到现有配置中，而不是替换它。
     */
    fun loadConfig(config: UIRouteConfig, merge: Boolean = false) {
        if (!merge || this.routeConfig == null) {
            Log.d(TAG, "Loading new config. Merge is false or current config is null.")
            this.routeConfig = config
        } else {
            Log.d(TAG, "Merging new config into existing config.")
            val currentConfig = this.routeConfig!!
            Log.d(TAG, "Before merge: ${currentConfig.functionDefinitions.size} functions. Merging ${config.functionDefinitions.size} new functions.")

            // 合并配置
            config.nodeDefinitions.forEach { (name, node) ->
                if (!currentConfig.nodeDefinitions.containsKey(name)) {
                    currentConfig.defineNode(node)
                    Log.d(TAG, "Merged node: $name")
                }
            }
            config.edgeDefinitions.forEach { (from, edges) ->
                edges.forEach { edge ->
                    // 在合并时也需要考虑多操作
                    currentConfig.defineEdge(from, edge.toNodeName, edge.operations, edge.validation, edge.conditions, edge.weight)
                    Log.d(TAG, "Merged edge: from $from to ${edge.toNodeName}")
                }
            }
            config.functionDefinitions.forEach { (name, function) ->
                currentConfig.defineFunction(function)
                Log.d(TAG, "Merged function: $name")
            }
            Log.d(TAG, "After merge: ${currentConfig.functionDefinitions.size} functions. Keys: ${currentConfig.functionDefinitions.keys.joinToString()}")
        }

        // 重新构建图
        Log.d(TAG, "Rebuilding stateful graph from config.")
        val builder = StatefulGraphBuilder.create()
        this.routeConfig!!.nodeDefinitions.values.forEach { builder.addNode(it.name, it.name) }
        this.routeConfig!!.edgeDefinitions.forEach { (from, edges) ->
            edges.forEach { edgeDef ->
                val operationToUse = if (edgeDef.operations.size == 1) {
                    edgeDef.operations.first()
                } else {
                    UIOperation.Sequential(edgeDef.operations, "Sequential actions from $from to ${edgeDef.toNodeName}")
                }

                builder.addStatefulEdge(
                    from = from,
                    to = edgeDef.toNodeName,
                    action = operationToUse.description,
                    stateTransform = operationToUse,
                    conditions = edgeDef.conditions,
                    weight = edgeDef.weight
                )
            }
        }
        graph = builder.build()
        pathFinder = StatefulPathFinder(graph)
        Log.d(TAG, "Graph rebuilt. Contains ${graph.getAllNodes().size} nodes.")
    }

    fun getAvailableFunctions(): List<UIFunction> {
        val functions = routeConfig?.functionDefinitions?.values?.toList() ?: emptyList()
        Log.d(TAG, "Getting available functions. Found ${functions.size} functions: ${functions.joinToString { it.name }}")
        return functions
    }

    /**
     * 规划一个完整的端到端功能执行路径。
     *
     * @param functionName 要执行的功能的名称。
     * @param initialParams 在规划阶段就已经明确的参数。
     * @return 一个包含完整导航+执行路径的 [RoutePlan]，如果找不到路径则返回null。
     */
    suspend fun planFunction(
        functionName: String,
        initialParams: Map<String, Any> = emptyMap()
    ): RoutePlan? {
        Log.d(TAG, "Planning function '$functionName' with initial params: $initialParams")
        val function = routeConfig?.functionDefinitions?.get(functionName)
        if (function == null) {
            Log.e(TAG, "Function '$functionName' not found in route config.")
            return null
        }

        try {
            // 1. 获取当前UI状态作为起点
            val startState = getCurrentUIState()
            if (startState == null) {
                Log.e(TAG, "Planning failed: Could not get current UI state.")
                return null
            }
            Log.d(TAG, "Current UI state determined as: ${startState.nodeId} (${startState.packageName})")

            val targetNodeName = function.targetNodeName
            val targetNode = routeConfig?.nodeDefinitions?.get(targetNodeName)
            if (targetNode == null) {
                Log.e(TAG, "Target node '$targetNodeName' not found in route config.")
                return null
            }
            val targetPackageName = targetNode.packageName

            var navPath: StatefulPath?
            val launchOperations = mutableListOf<StatefulEdge>()
            var pathStartState = startState.nodeState.withVariables(initialParams)

            // 2. 检查是否需要切换应用
            if (startState.packageName != targetPackageName) {
                Log.i(TAG, "Cross-application plan required. From '${startState.packageName}' to '$targetPackageName'.")
                
                val targetAppHomeNodeName = routeConfig?.nodeDefinitions?.values?.find {
                    it.packageName == targetPackageName && it.nodeType == UINodeType.APP_HOME
                }?.name

                if (targetAppHomeNodeName == null) {
                    Log.e(TAG, "Cannot find APP_HOME node for package '$targetPackageName'. Cannot plan cross-app.")
                    return null
                }

                val launchOperation = UIOperation.LaunchApp(targetPackageName)
                val launchEdge = StatefulEdge(
                    from = startState.nodeId,
                    to = targetAppHomeNodeName,
                    action = launchOperation.description,
                    stateTransform = launchOperation
                )
                launchOperations.add(launchEdge)

                pathStartState = NodeState(targetAppHomeNodeName).withVariables(initialParams)
                Log.d(TAG, "Planning path within target app, from '$targetAppHomeNodeName' to '$targetNodeName'")
            } else {
                Log.d(TAG, "Same-application plan. Finding path from '${startState.nodeId}' to '$targetNodeName'")
            }

            // 3. 搜索导航路径
            val navResult = pathFinder.findPath(
                startState = pathStartState,
                targetNodeId = targetNodeName,
                runtimeContext = initialParams
            )

            if (!navResult.success || navResult.path == null) {
                Log.w(TAG, "Path finding failed from ${pathStartState.nodeId} to $targetNodeName. Result: ${navResult.message}")
                return null
            }
            navPath = navResult.path
            Log.d(TAG, "Path found with ${navPath.edges.size} edges. Total weight: ${navPath.totalWeight}")
            
            val fullPath = if (launchOperations.isNotEmpty()) {
                // 如果有启动操作，我们需要构建一个全新的路径
                navPath.copy(
                    states = listOf(startState.nodeState) + navPath.states,
                    edges = launchOperations + navPath.edges,
                    totalWeight = navPath.totalWeight + launchOperations.sumOf { it.weight }
                )
            } else {
                // 如果在同一个应用内，直接使用找到的路径
                navPath
            }

            // 5. 将导航路径和功能操作合并成一个完整的路径
            Log.d(TAG, "Appending final function operation: ${function.operation.description}")
            val finalEdge = StatefulEdge(
                from = fullPath.endState.nodeId,
                to = function.targetNodeName, // or a new 'end' node if needed
                action = function.operation.description,
                stateTransform = function.operation
            )
            val finalState = function.operation.apply(fullPath.endState, initialParams)
            if (finalState == null) {
                Log.e(TAG, "Failed to apply final function operation. Path planning failed.")
                return null
            }
            
            val completePath = fullPath.copy(
                states = fullPath.states + finalState,
                edges = fullPath.edges + finalEdge,
                totalWeight = fullPath.totalWeight + finalEdge.weight
            )

            Log.d(TAG, "Full path created with ${completePath.edges.size} total steps.")

            // 6. 从完整路径中分析并提取所有需要的参数
            val allRequiredParams = analyzeParametersFromPath(completePath)
            Log.d(TAG, "Analyzed parameters from path. Required params: ${allRequiredParams.joinToString { it.key }}")

            // 7. 创建并返回执行计划
            val plan = RoutePlan(
                path = completePath,
                requiredParameters = allRequiredParams,
                executor = operationExecutor
            )
            Log.d(TAG, "RoutePlan created successfully for '$functionName'.")
            return plan
        } catch (e: Exception) {
            Log.e(TAG, "Exception during function planning for '$functionName'.", e)
            return null
        }
    }

    /**
     * 分析路径，提取所有依赖的参数。
     */
    private fun analyzeParametersFromPath(path: StatefulPath): List<RouteParameter<*>> {
        val params = mutableSetOf<RouteParameter<*>>()
        val allOperations = path.edges.map { it.stateTransform }

        allOperations.forEach { transform ->
            if (transform is UIOperation) {
                extractParamsFromOperation(transform, params)
            }
        }
        return params.toList()
    }

    /**
     * 递归地从单个操作及其子操作中提取参数。
     */
    private fun extractParamsFromOperation(operation: UIOperation, params: MutableSet<RouteParameter<*>>) {
        when (operation) {
            is UIOperation.Input -> {
                params.add(
                    RouteParameter(
                        key = operation.textVariableKey,
                        description = "需要为 '${operation.description}' 操作提供文本内容",
                        type = String::class.java
                    )
                )
            }
            is UIOperation.Click -> {
                if (operation.selector is UISelector.ByText) {
                    val templateVars = extractTemplateVariables(operation.selector.text)
                    templateVars.forEach { varName ->
                        params.add(
                            RouteParameter(
                                key = varName,
                                description = "需要为 '${operation.description}' 操作提供目标：$varName",
                                type = String::class.java
                            )
                        )
                    }
                }
            }
            is UIOperation.Sequential -> {
                // 递归分析序列中的每个操作
                operation.operations.forEach { subOperation ->
                    extractParamsFromOperation(subOperation, params)
                }
            }
            // 可以在这里添加更多对其他类型操作的参数分析
            else -> {}
        }
    }

    /**
     * 从文本中提取模板变量，例如从 "{{target_user}}" 中提取 "target_user"
     */
    private fun extractTemplateVariables(text: String): List<String> {
        val regex = Regex("\\{\\{([^}]+)\\}\\}")
        return regex.findAll(text).map { it.groupValues[1] }.toList()
    }
    
    /**
     * 获取当前的UI状态。
     * 使用AITool的get_page_info来获取真实的页面信息。
     * @return 当前的UIState，如果获取失败则返回null。
     */
    private suspend fun getCurrentUIState(): UIState? {
        var retryCount = 0
        while (retryCount < MAX_PAGE_INFO_RETRY) {
            try {
                Log.d(TAG, "正在获取当前页面信息 (尝试 ${retryCount + 1}/$MAX_PAGE_INFO_RETRY)")
                
                // 调用get_page_info工具, 请求详细信息
                val pageInfoTool = AITool(
                    name = "get_page_info",
                    parameters = listOf(
                        ToolParameter("format", "json"),
                        ToolParameter("detail", "detail") // 请求详细信息
                    )
                )
                
                val result = toolHandler.executeTool(pageInfoTool)
                
                if (result.success && result.result != null) {
                    return parseUIStateFromPageInfo(result.result)
                } else {
                    Log.w(TAG, "获取页面信息失败: ${result.error}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取页面信息时发生异常 (尝试 ${retryCount + 1})", e)
            }
            
            retryCount++
            if (retryCount < MAX_PAGE_INFO_RETRY) {
                delay(PAGE_INFO_RETRY_DELAY)
            }
        }
        
        Log.e(TAG, "多次尝试后仍无法获取页面信息，返回默认状态")
        // 如果所有尝试都失败，返回一个默认的系统主页状态
        return UIState(
            nodeId = "system_home",
            currentActivity = "Unknown",
            packageName = "android"
        )
    }

    /**
     * 从页面信息结果中解析出UIState。
     */
    private fun parseUIStateFromPageInfo(pageInfoResult: Any): UIState? {
        try {
            when (pageInfoResult) {
                is UIPageResultData -> {
                    val packageName = pageInfoResult.packageName
                    val activityName = pageInfoResult.activityName
                    
                    Log.d(TAG, "解析页面信息: packageName=$packageName, activityName=$activityName")
                    
                    val nodeId = findNodeIdForState(packageName, activityName, pageInfoResult) ?: "unknown_page"
                    
                    return UIState(
                        nodeId = nodeId,
                        currentActivity = activityName,
                        packageName = packageName,
                        uiElements = pageInfoResult.uiElements
                    )
                }
                is String -> {
                    // JSON字符串的解析逻辑可能需要调整以支持完整的UIPageResultData结构
                    // 为了简化, 我们假设这里也能得到一个可以解析为UIPageResultData的结构
                    // 在实际应用中, 可能需要更复杂的JSON解析
                    val jsonObject = JSONObject(pageInfoResult)
                    val packageName = jsonObject.optString("packageName", "unknown")
                    val activityName = jsonObject.optString("activityName", "unknown")
                    
                    Log.d(TAG, "从JSON解析页面信息: packageName=$packageName, activityName=$activityName")
                    
                    // 注意: 从纯JSON字符串可能无法重建完整的uiElements树, 除非JSON结构与UIPageResultData完全一致
                    // 这里的特征匹配可能会受限
                    val nodeId = findNodeIdForState(packageName, activityName, null) ?: "unknown_page"
                    
                    return UIState(
                        nodeId = nodeId,
                        currentActivity = activityName,
                        packageName = packageName
                    )
                }
                else -> {
                    Log.w(TAG, "未知的页面信息结果类型: ${pageInfoResult::class.java}")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析页面信息时发生错误", e)
            return null
        }
    }

    /**
     * 根据包名和Activity名查找对应的节点ID。
     * 新逻辑: 优先使用matchCriteria进行特征匹配, 然后回退到activityName匹配。
     */
    private fun findNodeIdForState(
        packageName: String,
        activityName: String?,
        uiElements: UIPageResultData?
    ): String? {
        val config = routeConfig ?: return null
        
        // 1. 优先使用特征匹配
        if (uiElements != null) {
            val matchedNode = config.nodeDefinitions.values.find { node ->
                node.packageName == packageName &&
                node.matchCriteria.isNotEmpty() &&
                node.matchCriteria.all { selector ->
                    // 检查页面上是否存在所有必需的元素
                    UIElementFinder.findElement(uiElements.uiElements, selector) != null
                }
            }
            if (matchedNode != null) {
                Log.d(TAG, "通过特征匹配找到节点: ${matchedNode.name}")
                return matchedNode.name
            }
        }

        // 2. 如果特征匹配失败, 回退到Activity名匹配
        // 精确匹配：包名和Activity名都匹配
        var matchedNode = config.nodeDefinitions.values.find { node ->
            node.packageName == packageName &&
            (node.activityName != null && node.activityName == activityName)
        }
        
        // 如果精确匹配失败，尝试只匹配包名且activityName为空的节点 (通常是App Home)
        if (matchedNode == null) {
            matchedNode = config.nodeDefinitions.values.find { node ->
                node.packageName == packageName && node.activityName == null
            }
        }

        if (matchedNode != null) {
            Log.d(TAG, "通过Activity/Package名匹配找到节点: ${matchedNode.name}")
        } else {
            Log.w(TAG, "无法为状态 $packageName/$activityName 找到匹配的节点")
        }
        
        return matchedNode?.name
    }

    /**
     * 使用实际参数替换操作中的模板变量
     */
    fun substituteTemplateVariables(operation: UIOperation, variables: Map<String, Any>): UIOperation {
        return when (operation) {
            is UIOperation.Click -> {
                when (val selector = operation.selector) {
                    is UISelector.ByText -> {
                        var substitutedText = selector.text
                        variables.forEach { (key, value) ->
                            substitutedText = substitutedText.replace("{{$key}}", value.toString())
                        }
                        operation.copy(selector = UISelector.ByText(substitutedText))
                    }
                    else -> operation
                }
            }
            is UIOperation.Sequential -> {
                val substitutedOps = operation.operations.map { 
                    substituteTemplateVariables(it, variables) 
                }
                operation.copy(operations = substitutedOps)
            }
            else -> operation
        }
    }
}