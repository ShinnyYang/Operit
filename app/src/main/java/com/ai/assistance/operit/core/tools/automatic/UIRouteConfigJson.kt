package com.ai.assistance.operit.core.tools.automatic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable data classes for parsing UI route configuration from JSON.
 * These classes mirror the structure of the JSON file and are used for deserialization.
 */

@Serializable
data class JsonUIRouteConfig(
    val appName: String,
    val packageName: String,
    val description: String = "No description provided.", // Add description field
    val nodes: List<JsonUINode>,
    val edges: List<JsonUIEdge>,
    val functions: List<JsonUIFunction>
)

@Serializable
data class JsonUINode(
    val name: String,
    val description: String,
    val activityName: String? = null,
    val nodeType: String, // e.g., "APP_HOME", "LIST_PAGE"
    val matchCriteria: List<JsonUISelector>? = null
)

@Serializable
data class JsonUIEdge(
    val from: String,
    val to: String,
    val operation: JsonUIOperation? = null, // 保持对旧格式的兼容
    val operations: List<JsonUIOperation> = emptyList(), // 新增，支持多步操作
    val validation: JsonUIOperation? = null,
    val conditions: Set<String> = emptySet(),
    val weight: Double = 1.0
)

@Serializable
data class JsonUIFunction(
    val name: String,
    val description: String,
    val targetNodeName: String,
    val operation: JsonUIOperation? = null // Functions might not have a direct operation
)

@Serializable
data class JsonUISelector(
    val type: String, // "ByText", "ByResourceId", "ByClassName", "ByContentDescription", "ByBounds"
    val value: String? = null,
    val partialMatch: Boolean = false,
    val index: Int? = null, // 当多个元素匹配时，指定索引
    val selectors: List<JsonUISelector>? = null, // 复合选择器
    val operator: String? = null // 复合选择器的操作符："AND", "OR"
)

/**
 * Defines a polymorphic base class for UI operations.
 * The @SerialName annotation maps the "type" field in JSON to the corresponding subclass.
 */
@Serializable
sealed class JsonUIOperation {
    abstract val description: String?

    @Serializable
    @SerialName("Click")
    data class Click(
        val selector: JsonUISelector,
        override val description: String? = null,
        val relativeX: Float? = null,
        val relativeY: Float? = null
    ) : JsonUIOperation()

    @Serializable
    @SerialName("Input")
    data class Input(
        val selector: JsonUISelector,
        val textVariableKey: String,
        override val description: String? = null
    ) : JsonUIOperation()

    @Serializable
    @SerialName("LaunchApp")
    data class LaunchApp(
        val packageName: String,
        override val description: String? = null
    ) : JsonUIOperation()

    @Serializable
    @SerialName("PressKey")
    data class PressKey(
        val keyCode: String,
        override val description: String? = null
    ) : JsonUIOperation()

    @Serializable
    @SerialName("Wait")
    data class Wait(
        val durationMs: Long,
        override val description: String? = null
    ) : JsonUIOperation()

    @Serializable
    @SerialName("Sequential")
    data class Sequential(
        val operations: List<JsonUIOperation>,
        override val description: String? = null
    ) : JsonUIOperation()

    @Serializable
    @SerialName("ValidateElement")
    data class ValidateElement(
        val selector: JsonUISelector,
        val expectedValueKey: String,
        val validationType: String, // "TEXT_EQUALS", "CONTAINS", etc.
        override val description: String? = null
    ) : JsonUIOperation()
} 