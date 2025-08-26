package com.ai.assistance.operit.core.tools.automatic

import com.ai.assistance.operit.core.tools.SimplifiedUINode
import com.ai.assistance.operit.util.map.NodeState

/**
 * 代表UI界面的特定状态。
 * 它包装了通用的NodeState，并添加了UI自动化相关的特定上下文变量。
 *
 * @property nodeState 底层的通用状态对象。
 * @property currentActivity 当前界面的Activity名称。
 * @property packageName 当前界面所属的应用包名。
 * @property uiElements 当前页面的UI元素结构信息。
 * @property targetUser 目标用户或聊天对象，用于需要指定交互对象的场景。
 * @property inputText 待输入的文本内容。
 * @property lastError 上一次操作发生的错误信息，用于重试和恢复。
 * @property retryCount 当前操作的重试次数。
 * @property customData 一个灵活的map，用于存储任何其他的自定义状态数据。
 */
class UIState(
    nodeId: String,
    val currentActivity: String? = null,
    val packageName: String? = null,
    val uiElements: SimplifiedUINode? = null,
    val targetUser: String? = null,
    val inputText: String? = null,
    val lastError: String? = null,
    val retryCount: Int = 0,
    val customData: Map<String, Any> = emptyMap()
) {
    val nodeState: NodeState = NodeState(nodeId, buildVariableMap())

    // 代理NodeState的属性
    val nodeId: String get() = nodeState.nodeId
    val variables: Map<String, Any> get() = nodeState.variables

    /**
     * 检查当前状态是否匹配指定的包名和Activity
     */
    fun matchesApp(packageName: String, activityName: String? = null): Boolean {
        val packageMatches = this.packageName == packageName
        val activityMatches = activityName == null || this.currentActivity == activityName
        return packageMatches && activityMatches
    }

    /**
     * Finds a UI element by its text content.
     */
    fun findElementByText(text: String): SimplifiedUINode? {
        return uiElements?.let {
            UIElementFinder.findElement(it, UISelector.ByText(text))
        }
    }

    /**
     * Finds a UI element by its resource ID.
     */
    fun findElementByResourceId(resourceId: String): SimplifiedUINode? {
        return uiElements?.let {
            UIElementFinder.findElement(it, UISelector.ByResourceId(resourceId))
        }
    }

    /**
     * 创建一个新的UIState实例，并用新的变量更新其内部的NodeState
     */
    fun copyWith(
        nodeId: String = this.nodeId,
        currentActivity: String? = this.currentActivity,
        packageName: String? = this.packageName,
        uiElements: SimplifiedUINode? = this.uiElements,
        targetUser: String? = this.targetUser,
        inputText: String? = this.inputText,
        lastError: String? = this.lastError,
        retryCount: Int = this.retryCount,
        customData: Map<String, Any> = this.customData
    ): UIState {
        return UIState(
            nodeId = nodeId,
            currentActivity = currentActivity,
            packageName = packageName,
            uiElements = uiElements,
            targetUser = targetUser,
            inputText = inputText,
            lastError = lastError,
            retryCount = retryCount,
            customData = customData
        )
    }

    /**
     * 一个辅助函数，用于将UIState的属性转换为NodeState所需的variables map。
     */
    private fun buildVariableMap(): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()
        currentActivity?.let { variables["currentActivity"] = it }
        packageName?.let { variables["packageName"] = it }
        targetUser?.let { variables["target_user"] = it }
        inputText?.let { variables["input_text"] = it }
        lastError?.let { variables["last_error"] = it }
        variables["retry_count"] = retryCount
        variables.putAll(customData)
        return variables
    }
}

/**
 * SimplifiedUINode的扩展函数，用于递归查找元素
 */
private fun SimplifiedUINode.findElementByText(text: String): SimplifiedUINode? {
    // 检查当前节点
    if (this.text?.contains(text) == true) {
        return this
    }

    // 递归检查子节点
    for (child in this.children) {
        val found = child.findElementByText(text)
        if (found != null) {
            return found
        }
    }

    return null
}

private fun SimplifiedUINode.findElementByResourceId(resourceId: String): SimplifiedUINode? {
    // 检查当前节点
    if (this.resourceId?.contains(resourceId) == true) {
        return this
    }

    // 递归检查子节点
    for (child in this.children) {
        val found = child.findElementByResourceId(resourceId)
        if (found != null) {
            return found
        }
    }

    return null
} 