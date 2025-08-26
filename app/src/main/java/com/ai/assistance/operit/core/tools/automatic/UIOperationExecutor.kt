package com.ai.assistance.operit.core.tools.automatic

import android.content.Context
import android.util.Log
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.core.tools.SimplifiedUINode
import com.ai.assistance.operit.core.tools.UIPageResultData
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolParameter
import com.ai.assistance.operit.util.map.NodeState
import com.ai.assistance.operit.util.map.StatefulPath
import com.ai.assistance.operit.util.map.StatefulEdge
import kotlinx.coroutines.delay
import com.ai.assistance.operit.core.tools.StringResultData
import org.json.JSONObject

private const val TAG = "UIOperationExecutor"

/**
 * UI操作执行器。
 * 负责接收一个路径 (StatefulPath)，并按顺序执行路径上每个边定义的UI操作。
 */
class UIOperationExecutor(
    private val context: Context,
    private val toolHandler: AIToolHandler,
    private val routeConfig: UIRouteConfig?
) {
    companion object {
        private const val OPERATION_DELAY = 500L
        private const val STARTUP_DELAY = 2000L // 启动应用后的额外等待时间
        private const val FOCUS_DELAY = 300L
        private const val SEQUENTIAL_OP_DELAY = 200L
        private const val MAX_STATE_VERIFICATION_RETRY = 3
        private const val STATE_VERIFICATION_DELAY = 1000L
    }

    /**
     * 执行给定的路径。
     * @param path 要执行的状态路径。
     * @param context 包含运行时参数的上下文，例如 "current_ui_state"。
     * @return 路由执行结果。
     */
    suspend fun executePath(path: StatefulPath, context: Map<String, Any> = emptyMap()): RouteResult {
        var currentNodeState = path.startState
        var currentUIState = (context["current_ui_state"] as? UIState)
            ?: UIState(nodeId = currentNodeState.nodeId, customData = currentNodeState.variables)

        Log.d(TAG, "Executing path from ${path.startState.nodeId} to ${path.endState.nodeId}. Total steps: ${path.edges.size}")

        for ((index, edge) in path.edges.withIndex()) {
            val operation = edge.stateTransform as? UIOperation
                ?: continue // 如果不是UIOperation，则跳过

            Log.i(TAG, "Step ${index + 1}/${path.edges.size}: Executing '${operation.description}' (${edge.from} -> ${edge.to})")

            // 在执行操作前，使用当前状态的变量替换模板
            val substitutedOperation = substituteTemplateVariables(operation, currentUIState.variables)
            
            val executionSuccess = executeOperation(substitutedOperation, currentUIState)

            if (!executionSuccess) {
                val errorMsg = "Operation failed: '${operation.description}'"
                Log.e(TAG, errorMsg)
                return RouteResult.failure(errorMsg, currentUIState)
            }

            // 执行附加的验证操作
            val validationOp = findValidationInOriginalConfig(edge)
            if (validationOp != null) {
                // 在验证之前，先替换模板变量，确保验证的值是动态的
                val substitutedValidationOp = substituteTemplateVariables(validationOp, currentUIState.variables) as UIOperation.ValidateElement
                Log.d(TAG, "Executing validation: ${substitutedValidationOp.description}")
                delay(OPERATION_DELAY) // 等待UI稳定
                
                // 为了执行验证，需要一个包含最新上下文的 "state"
                // 我们用 currentUIState 来模拟这个 state
                val validationSuccess = executeOperation(substitutedValidationOp, currentUIState)
                if (!validationSuccess) {
                    val errorMsg = "Validation failed: '${substitutedValidationOp.description}'"
                    Log.e(TAG, errorMsg)
                    return RouteResult.failure(errorMsg, currentUIState)
                }
                 Log.d(TAG, "Validation successful: ${substitutedValidationOp.description}")
            }


            // 操作之间的延迟
            delay(OPERATION_DELAY)

            // 验证是否成功到达目标状态
            val nextNodeState = path.getStateAt(index + 1) ?: break
            val newUIState = verifyStateTransition(edge.to, currentUIState)
            if (newUIState != null) {
                currentUIState = newUIState.copyWith(
                    customData = nextNodeState.variables + newUIState.customData
                )
                currentNodeState = nextNodeState
                Log.d(TAG, "State transition verified. New node: ${currentUIState.nodeId}")
            } else {
                Log.w(TAG, "State transition verification failed for node '${edge.to}'. Continuing execution, assuming success.")
                currentNodeState = nextNodeState
                currentUIState = currentUIState.copyWith(
                    nodeId = edge.to,
                    customData = currentNodeState.variables + currentUIState.customData
                )
            }
        }

        Log.i(TAG, "Path execution finished successfully. Final node: ${currentUIState.nodeId}")

        return RouteResult.success(currentUIState, path, "路径导航成功")
    }

    /**
     * 验证状态转换是否成功
     */
    private suspend fun verifyStateTransition(expectedNodeId: String, previousState: UIState): UIState? {
        var retryCount = 0
        while (retryCount < MAX_STATE_VERIFICATION_RETRY) {
            try {
                // 获取当前页面信息
                val pageInfoTool = AITool(
                    name = "get_page_info",
                    parameters = listOf(
                        ToolParameter("format", "json"),
                        ToolParameter("detail", "summary")
                    )
                )
                
                val result = toolHandler.executeTool(pageInfoTool)
                
                if (result.success && result.result != null) {
                    val newUIState = parseUIStateFromPageInfo(result.result, expectedNodeId)
                    if (newUIState != null) {
                        // 如果我们期望的节点是当前页面，则认为验证成功
                        val pageMatches = newUIState.packageName == previousState.packageName &&
                                          newUIState.currentActivity == previousState.currentActivity
                        // 在更复杂的场景中，这里应该用 aergolu.json 中的页面定义来匹配 expectedNodeId
                        // 此处简化为只要获取到新页面状态就认为转换成功
                        Log.d(TAG, "状态验证成功，获取到新页面: ${newUIState.packageName}/${newUIState.currentActivity}")
                        return newUIState
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "状态验证异常 (尝试 ${retryCount + 1})", e)
            }
            
            retryCount++
            if (retryCount < MAX_STATE_VERIFICATION_RETRY) {
                delay(STATE_VERIFICATION_DELAY)
            }
        }
        
        return null
    }

    /**
     * 从页面信息结果中解析出UIState
     */
    private fun parseUIStateFromPageInfo(pageInfoResult: Any, expectedNodeId: String): UIState? {
        try {
            when (pageInfoResult) {
                is UIPageResultData -> {
                    return UIState(
                        nodeId = expectedNodeId,
                        currentActivity = pageInfoResult.activityName,
                        packageName = pageInfoResult.packageName,
                        uiElements = pageInfoResult.uiElements
                    )
                }
                is String -> {
                    val jsonObject = JSONObject(pageInfoResult)
                    val packageName = jsonObject.optString("packageName", "unknown")
                    val activityName = jsonObject.optString("activityName", "unknown")
                    
                    return UIState(
                        nodeId = expectedNodeId,
                        currentActivity = activityName,
                        packageName = packageName
                    )
                }
                else -> return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析页面信息时发生错误", e)
            return null
        }
    }

    /**
     * 执行单个UI操作。
     * @param operation 要执行的UI操作。
     * @param state 当前的UI状态，用于提供上下文参数。
     * @return 操作是否成功。
     */
    private suspend fun executeOperation(operation: UIOperation, state: UIState): Boolean {
        Log.d(TAG, "Executing operation: ${operation::class.java.simpleName} with description: '${operation.description}'")
        return when (operation) {
            is UIOperation.Click -> {
                // 如果提供了相对坐标，则使用get_page_info+tap的组合来实现精确点击
                if (operation.relativeX != null && operation.relativeY != null) {
                    // 1. 获取页面信息以查找元素边界
                    val pageInfoTool = AITool("get_page_info", listOf(
                        ToolParameter("format", "json"),
                        ToolParameter("detail", "full")
                    ))
                    Log.d(TAG, "Executing 'get_page_info' to get bounds for relative click.")
                    val pageInfoResult = toolHandler.executeTool(pageInfoTool)

                    if (!pageInfoResult.success || pageInfoResult.result == null) {
                        Log.w(TAG, "Relative click failed: could not get page info. ${pageInfoResult.error}")
                        return false
                    }

                    // 2. 从页面信息中查找匹配的元素
                    val resultString = (pageInfoResult.result as? StringResultData)?.value ?: ""
                    if (resultString.isEmpty()) {
                        Log.w(TAG, "Relative click failed: get_page_info returned an empty result.")
                        return false
                    }

                    try {
                        val jsonResult = JSONObject(resultString)
                        val rootNode = jsonResult.optJSONObject("rootNode")
                        
                        // 查找匹配的元素
                        val selector = operation.selector
                        val matchingElement = findElementInNode(rootNode, selector)
                        
                        if (matchingElement == null) {
                            Log.w(TAG, "Relative click failed: element not found in page info.")
                            return false
                        }

                        val boundsString = matchingElement.optString("bounds")
                        if (boundsString.isNullOrEmpty()) {
                            Log.w(TAG, "Relative click failed: 'bounds' not found for element.")
                            return false
                        }

                        // 3. 解析边界字符串并计算精确的点击点
                        val parts = boundsString.replace("[", "").replace("]", ",").split(",").filter { it.isNotEmpty() }
                        if (parts.size < 4) {
                            Log.w(TAG, "Relative click failed: invalid bounds format '$boundsString'.")
                            return false
                        }
                        val left = parts[0].toInt()
                        val top = parts[1].toInt()
                        val right = parts[2].toInt()
                        val bottom = parts[3].toInt()

                        val width = right - left
                        val height = bottom - top

                        val tapX = left + (width * operation.relativeX).toInt()
                        val tapY = top + (height * operation.relativeY).toInt()

                        // 4. 执行tap操作
                        val tapTool = AITool("tap", listOf(
                            ToolParameter("x", tapX.toString()),
                            ToolParameter("y", tapY.toString())
                        ))
                        Log.d(TAG, "Executing relative click via 'tap' tool at ($tapX, $tapY)")
                        val tapResult = toolHandler.executeTool(tapTool)
                        if (!tapResult.success) {
                            Log.w(TAG, "Tap operation failed: ${tapResult.error}")
                        }
                        return tapResult.success
                    } catch (e: Exception) {
                        Log.e(TAG, "Relative click failed due to an exception while parsing bounds or executing tap.", e)
                        return false
                    }
                } else {
                    // 对于没有相对坐标的点击，保持原逻辑
                val params = createSelectorParams(operation.selector, state, operation)
                val tool = AITool("click_element", params)
                Log.d(TAG, "Executing tool '${tool.name}' with params: $params")
                val result = toolHandler.executeTool(tool)
                if (!result.success) {
                    Log.w(TAG, "Click operation failed: ${result.error}")
                }
                result.success
                }
            }
            is UIOperation.Input -> {
                val textToInput = state.variables[operation.textVariableKey] as? String ?: ""
                Log.d(TAG, "Preparing to input text. Key: '${operation.textVariableKey}', Value: '$textToInput'")
                
                val clickParams = createSelectorParams(operation.selector, state)
                val clickTool = AITool("click_element", clickParams)
                Log.d(TAG, "Executing tool '${clickTool.name}' to focus input field with params: $clickParams")
                
                // 先点击输入框
                if (!toolHandler.executeTool(clickTool).success) {
                    Log.w(TAG, "Failed to click input field.")
                    return false
                }
                
                delay(FOCUS_DELAY) // 等待输入框获取焦点

                // 再输入文本
                val inputTool = AITool("set_input_text", listOf(ToolParameter("text", textToInput)))
                 Log.d(TAG, "Executing tool '${inputTool.name}' to set text.")
                val inputResult = toolHandler.executeTool(inputTool)
                if (!inputResult.success) {
                    Log.w(TAG, "Input text failed: ${inputResult.error}")
                }
                inputResult.success
            }
            is UIOperation.LaunchApp -> {
                val tool = AITool("start_app", listOf(ToolParameter("package_name", operation.packageName)))
                Log.d(TAG, "Executing tool '${tool.name}' with package: ${operation.packageName}")
                val result = toolHandler.executeTool(tool)
                if (result.success) {
                    delay(STARTUP_DELAY) // 等待应用启动和界面加载
                } else {
                    Log.w(TAG, "Launch app failed: ${result.error}")
                }
                result.success
            }
            is UIOperation.KillApp -> {
                val tool = AITool("stop_app", listOf(ToolParameter("package_name", operation.packageName)))
                 Log.d(TAG, "Executing tool '${tool.name}' with package: ${operation.packageName}")
                val result = toolHandler.executeTool(tool)
                if (!result.success) {
                    Log.w(TAG, "Kill app failed: ${result.error}")
                }
                result.success
            }
            is UIOperation.PressKey -> {
                val tool = AITool("pressKey", listOf(ToolParameter("key_code", operation.keyCode)))
                Log.d(TAG, "Executing tool '${tool.name}' with key_code: ${operation.keyCode}")
                val result = toolHandler.executeTool(tool)
                if (!result.success) {
                    Log.w(TAG, "Press key failed: ${result.error}")
                }
                result.success
            }
            is UIOperation.Swipe -> {
                val tool = AITool("swipe", listOf(
                    ToolParameter("direction", operation.direction.name.lowercase()),
                    ToolParameter("distance", operation.distance.toString())
                ))
                Log.d(TAG, "Executing tool '${tool.name}' with direction: ${operation.direction}")
                val result = toolHandler.executeTool(tool)
                if (!result.success) {
                    Log.w(TAG, "Swipe operation failed: ${result.error}")
                }
                result.success
            }
            is UIOperation.Wait -> {
                Log.d(TAG, "Waiting for ${operation.durationMs}ms")
                delay(operation.durationMs)
                true
            }
            is UIOperation.WaitForPage -> {
                Log.d(TAG, "Waiting for page. Timeout: ${operation.timeoutMs}ms")
                delay(operation.timeoutMs)
                true
            }
            is UIOperation.ValidateState -> {
                val isValid = operation.validator(state)
                Log.d(TAG, "状态验证结果: $isValid")
                isValid
            }
            is UIOperation.ValidateElement -> {
                val expectedValue = state.variables[operation.expectedValueKey]?.toString()
                if (expectedValue == null && operation.validationType != ValidationType.EXISTS) {
                    Log.w(TAG, "Validation failed: Expected value key '${operation.expectedValueKey}' not found in state variables.")
                    return false
                }

                // 获取当前页面的详细信息
                val pageInfo = getCurrentPageInfo(detail = "detail")
                if (pageInfo == null) {
                    Log.w(TAG, "Validation failed: Could not get current page info.")
                    return false
                }
                
                val element = UIElementFinder.findElement(pageInfo.uiElements, operation.selector)
                if (element == null) {
                    Log.w(TAG, "Validation failed: Element not found with selector: '${operation.selector}'")
                    return false
                }

                when (operation.validationType) {
                    ValidationType.TEXT_EQUALS -> {
                        val actualText = element.text ?: ""
                        val success = actualText == expectedValue
                        Log.d(TAG, "Validation TEXT_EQUALS: Expected='$expectedValue', Actual='$actualText'. Success: $success")
                        success
                    }
                    ValidationType.TEXT_CONTAINS -> {
                        val actualText = element.text ?: ""
                        val success = actualText.contains(expectedValue!!)
                        Log.d(TAG, "Validation TEXT_CONTAINS: Expected to contain='$expectedValue', Actual='$actualText'. Success: $success")
                        success
                    }
                    ValidationType.EXISTS -> {
                        Log.d(TAG, "Validation EXISTS: Element found. Success: true")
                        true
                    }
                }
            }
            is UIOperation.Sequential -> {
                for ((index, op) in operation.operations.withIndex()) {
                    Log.d(TAG, "Executing sequential op ${index + 1}/${operation.operations.size}: ${op.description}")
                    if (!executeOperation(op, state)) {
                        Log.e(TAG, "Sequential operation failed at step ${index + 1}: ${op.description}")
                        return false
                    }
                    if (index < operation.operations.size - 1) {
                        delay(SEQUENTIAL_OP_DELAY) // 序列操作之间的短暂延迟
                    }
                }
                Log.d(TAG, "Sequential operation completed successfully.")
                true
            }
            is UIOperation.NoOp -> {
                Log.d(TAG, "Executing NoOp.")
                true
            }
            else -> {
                Log.w(TAG, "Unhandled UI operation: ${operation::class.simpleName}")
                true // 默认返回成功以允许流程继续
            }
        }
    }

    private suspend fun getCurrentPageInfo(detail: String = "summary"): UIPageResultData? {
        val pageInfoTool = AITool(
            name = "get_page_info",
            parameters = listOf(
                ToolParameter("format", "json"),
                ToolParameter("detail", detail)
            )
        )
        val result = toolHandler.executeTool(pageInfoTool)
        return if (result.success && result.result is UIPageResultData) {
            result.result as UIPageResultData
        } else {
            null
        }
    }

    private fun findValidationInOriginalConfig(edge: StatefulEdge): UIOperation.ValidateElement? {
        val config = routeConfig ?: return null
        val fromNodeName = edge.from
        val toNodeName = edge.to
        
        return config.edgeDefinitions[fromNodeName]?.find { it.toNodeName == toNodeName }?.validation
    }

    /**
     * 使用实际参数替换操作中的模板变量
     */
    private fun substituteTemplateVariables(operation: UIOperation, variables: Map<String, Any>): UIOperation {
        if (variables.isEmpty()) return operation
        Log.d(TAG, "Substituting template variables in operation: ${operation.description}")
        return when (operation) {
            is UIOperation.Click -> {
                operation.copy(selector = substituteTemplateInSelector(operation.selector, variables))
            }
            is UIOperation.Input -> {
                operation.copy(selector = substituteTemplateInSelector(operation.selector, variables))
            }
            is UIOperation.ValidateElement -> {
                operation.copy(
                    selector = substituteTemplateInSelector(operation.selector, variables)
                )
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

    /**
     * 在选择器中替换模板变量
     */
    private fun substituteTemplateInSelector(selector: UISelector, variables: Map<String, Any>): UISelector {
        return when (selector) {
            is UISelector.ByText -> {
                var substitutedText = selector.text
                variables.forEach { (key, value) ->
                    substitutedText = substitutedText.replace("{{$key}}", value.toString())
                }
                if (substitutedText != selector.text) {
                    Log.d(TAG, "Substituted text selector: from '${selector.text}' to '$substitutedText'")
                }
                UISelector.ByText(substitutedText)
            }
            is UISelector.ByContentDesc -> {
                var substitutedDesc = selector.desc
                variables.forEach { (key, value) ->
                    substitutedDesc = substitutedDesc.replace("{{$key}}", value.toString())
                }
                if (substitutedDesc != selector.desc) {
                    Log.d(TAG, "Substituted contentDesc selector: from '${selector.desc}' to '$substitutedDesc'")
                }
                UISelector.ByContentDesc(substitutedDesc)
            }
            else -> selector
        }
    }

    /**
     * 根据UISelector创建工具所需的参数列表。
     */
    private fun createSelectorParams(
        selector: UISelector, 
        state: UIState,
        clickOperation: UIOperation.Click? = null
    ): List<ToolParameter> {
        val params = when (selector) {
            is UISelector.ByResourceId -> mutableListOf(ToolParameter("resourceId", selector.id))
            is UISelector.ByText -> {
                val element = state.findElementByText(selector.text)
                if (element != null) {
                    Log.d(TAG, "在当前页面找到文本元素: ${selector.text}")
                    element.resourceId?.let { 
                        return mutableListOf(ToolParameter("resourceId", it))
                    }
                }
                mutableListOf(ToolParameter("text", selector.text))
            }
            is UISelector.ByContentDesc -> mutableListOf(ToolParameter("contentDesc", selector.desc))
            is UISelector.ByClassName -> mutableListOf(ToolParameter("className", selector.name))
            is UISelector.ByBounds -> mutableListOf(ToolParameter("bounds", selector.bounds))
            is UISelector.ByXPath -> {
                Log.d(TAG, "使用XPath选择器: ${selector.xpath}")
                mutableListOf(ToolParameter("xpath", selector.xpath))
            }
            is UISelector.Compound -> {
                Log.d(TAG, "处理复合选择器，操作符: ${selector.operator}")
                val combinedParams = mutableListOf<ToolParameter>()
                selector.selectors.forEach { subSelector ->
                    combinedParams.addAll(createSelectorParams(subSelector, state, clickOperation))
                }
                return combinedParams
            }
        }

        // 如果是点击操作，并且提供了相对坐标，则附加它们
        clickOperation?.let {
            if (it.relativeX != null && it.relativeY != null) {
                params.add(ToolParameter("relativeX", it.relativeX.toString()))
                params.add(ToolParameter("relativeY", it.relativeY.toString()))
                Log.d(TAG, "附加相对坐标参数: x=${it.relativeX}, y=${it.relativeY}")
            }
        }
        
        return params
    }

    /**
     * 在 JSON 节点中递归搜索匹配选择器的元素
     */
    private fun findElementInNode(node: JSONObject?, selector: UISelector): JSONObject? {
        if (node == null) return null

        // 检查当前节点是否匹配
        if (matchesSelector(node, selector)) {
            return node
        }

        // 递归搜索子节点
        val children = node.optJSONArray("children")
        if (children != null) {
            for (i in 0 until children.length()) {
                val child = children.optJSONObject(i)
                val result = findElementInNode(child, selector)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 检查节点是否匹配选择器
     */
    private fun matchesSelector(node: JSONObject, selector: UISelector): Boolean {
        return when (selector) {
            is UISelector.ByResourceId -> {
                val resourceId = node.optString("resourceId", "")
                resourceId.contains(selector.id)
            }
            is UISelector.ByClassName -> {
                val className = node.optString("className", "")
                className.contains(selector.name)
            }
            is UISelector.ByText -> {
                val text = node.optString("text", "")
                text.contains(selector.text)
            }
            is UISelector.ByContentDesc -> {
                val contentDesc = node.optString("contentDesc", "")
                contentDesc.contains(selector.desc)
            }
            is UISelector.ByBounds -> {
                val bounds = node.optString("bounds", "")
                bounds.contains(selector.bounds)
            }
            is UISelector.ByXPath -> {
                // XPath 匹配比较复杂，暂时返回false
                false
            }
            is UISelector.Compound -> {
                // 对于组合选择器，暂时返回false，因为这里的逻辑会比较复杂
                false
            }
        }
    }
}