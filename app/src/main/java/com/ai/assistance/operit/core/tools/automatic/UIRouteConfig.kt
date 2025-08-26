package com.ai.assistance.operit.core.tools.automatic

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * UI功能定义。
 * 代表一个高阶的用户目标，例如“发送消息”或“发朋友圈”。
 *
 * @property name 人类可读的功能名称，作为其唯一标识符。
 * @property description 功能的详细描述。
 * @property targetNodeName 执行此功能前必须到达的UI节点的名称。
 * @property operation 到达目标节点后，为完成此功能需要执行的UI操作。
 */
data class UIFunction(
    val name: String,
    val description: String,
    val targetNodeName: String,
    val operation: UIOperation
)

/**
 * UI路由配置中心。
 * 在这里定义所有支持的应用的节点（页面）、边（操作）和功能。
 * 整个配置使用人类可读的名称作为ID，更易于维护。
 */
class UIRouteConfig {
    // 存储节点定义，Key是节点的name
    val nodeDefinitions = mutableMapOf<String, UINode>()
    // 存储边定义，Key是起点的name
    val edgeDefinitions = mutableMapOf<String, MutableList<UIEdgeDefinition>>()
    // 存储功能定义，Key是功能的name
    val functionDefinitions = mutableMapOf<String, UIFunction>()

    /**
     * 定义一个UI节点（页面/状态），使用其name作为唯一标识。
     */
    fun defineNode(node: UINode) {
        nodeDefinitions[node.name] = node
    }

    /**
     * 定义一条边，即一个从起点到终点的UI操作。
     * @param fromNodeName 起始节点的名称。
     * @param toNodeName 目标节点的名称。
     */
    fun defineEdge(fromNodeName: String, toNodeName: String, operation: UIOperation, validation: UIOperation.ValidateElement? = null, conditions: Set<String> = emptySet(), weight: Double = 1.0) {
        val edge = UIEdgeDefinition(toNodeName, listOf(operation), validation, conditions, weight)
        edgeDefinitions.computeIfAbsent(fromNodeName) { mutableListOf() }.add(edge)
    }

    /**
     * 定义一条支持多步操作的边。
     */
    fun defineEdge(fromNodeName: String, toNodeName: String, operations: List<UIOperation>, validation: UIOperation.ValidateElement? = null, conditions: Set<String> = emptySet(), weight: Double = 1.0) {
        val edge = UIEdgeDefinition(toNodeName, operations, validation, conditions, weight)
        edgeDefinitions.computeIfAbsent(fromNodeName) { mutableListOf() }.add(edge)
    }

    /**
     * 定义一个高阶功能，使用其name作为唯一标识。
     */
    fun defineFunction(function: UIFunction) {
        functionDefinitions[function.name] = function
    }

    companion object {
        private const val TAG = "UIRouteConfig"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

        fun loadFromJson(jsonString: String): UIRouteConfig {
            Log.d(TAG, "Attempting to load UIRouteConfig from JSON string.")
            val config = UIRouteConfig()
            try {
                val jsonConfig = json.decodeFromString<JsonUIRouteConfig>(jsonString)
                Log.d(TAG, "Successfully decoded JSON for app: ${jsonConfig.appName}")
                Log.d(TAG, "Found ${jsonConfig.nodes.size} nodes, ${jsonConfig.edges.size} edges, ${jsonConfig.functions.size} functions in JSON.")


                jsonConfig.nodes.forEach { jsonNode ->
                    config.defineNode(UINode(
                        name = jsonNode.name,
                        packageName = jsonConfig.packageName,
                        activityName = jsonNode.activityName,
                        nodeType = UINodeType.valueOf(jsonNode.nodeType),
                        matchCriteria = jsonNode.matchCriteria?.mapNotNull { convertJsonSelector(it) } ?: emptyList()
                    ))
                }

                jsonConfig.edges.forEach { jsonEdge ->
                    val operations = if (jsonEdge.operations.isNotEmpty()) {
                        jsonEdge.operations.mapNotNull { convertJsonOperation(it) }
                    } else {
                        // 兼容旧的单个operation字段
                        jsonEdge.operation?.let { op -> convertJsonOperation(op)?.let { listOf(it) } } ?: emptyList()
                    }

                    if (operations.isEmpty()) {
                        Log.w(TAG, "Edge from '${jsonEdge.from}' to '${jsonEdge.to}' has no valid operations after filtering. Skipping.")
                        return@forEach
                    }

                    val validation = jsonEdge.validation?.let { convertJsonOperation(it) as? UIOperation.ValidateElement }

                    val edge = UIEdgeDefinition(
                        toNodeName = jsonEdge.to,
                        operations = operations,
                        validation = validation,
                        conditions = jsonEdge.conditions,
                        weight = jsonEdge.weight
                    )
                    config.edgeDefinitions.computeIfAbsent(jsonEdge.from) { mutableListOf() }.add(edge)
                }

                jsonConfig.functions.forEach { jsonFunction ->
                    Log.d(TAG, "Processing function from JSON: ${jsonFunction.name}")
                    val operation = jsonFunction.operation?.let { convertJsonOperation(it) }
                    config.defineFunction(UIFunction(
                        name = jsonFunction.name,
                        description = jsonFunction.description,
                        targetNodeName = jsonFunction.targetNodeName,
                        operation = operation ?: UIOperation.Sequential(emptyList(), "Empty function operation")
                    ))
                }

                Log.d(TAG, "Finished loading from JSON. Final config has ${config.functionDefinitions.size} functions: ${config.functionDefinitions.keys.joinToString()}")
                return config
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load UIRouteConfig from JSON. Error: ${e.message}", e)
                return config // return empty config on failure
            }
        }

        private fun convertJsonOperation(jsonOp: JsonUIOperation): UIOperation? {
            return try {
                when (jsonOp) {
                    is JsonUIOperation.Click -> {
                        val selector = convertJsonSelector(jsonOp.selector)
                        selector?.let {
                            UIOperation.Click(
                                selector = it,
                                description = jsonOp.description ?: "Click",
                                relativeX = jsonOp.relativeX,
                                relativeY = jsonOp.relativeY
                            )
                        }
                    }
                    is JsonUIOperation.Input -> {
                        val selector = convertJsonSelector(jsonOp.selector)
                        selector?.let {
                            UIOperation.Input(
                                selector = it,
                                textVariableKey = jsonOp.textVariableKey,
                                description = jsonOp.description ?: "Input text"
                            )
                        }
                    }
                    is JsonUIOperation.LaunchApp -> UIOperation.LaunchApp(
                        packageName = jsonOp.packageName,
                        description = jsonOp.description ?: "Launch app"
                    )
                    is JsonUIOperation.PressKey -> UIOperation.PressKey(
                        keyCode = jsonOp.keyCode,
                        description = jsonOp.description ?: "Press key ${jsonOp.keyCode}"
                    )
                    is JsonUIOperation.Wait -> UIOperation.Wait(
                        durationMs = jsonOp.durationMs,
                        description = jsonOp.description ?: "Wait for ${jsonOp.durationMs}ms"
                    )
                    is JsonUIOperation.Sequential -> UIOperation.Sequential(
                        operations = jsonOp.operations.mapNotNull { convertJsonOperation(it) },
                        description = jsonOp.description ?: "Sequential operations"
                    )
                    is JsonUIOperation.ValidateElement -> {
                        val selector = convertJsonSelector(jsonOp.selector)
                        selector?.let {
                            UIOperation.ValidateElement(
                                selector = it,
                                expectedValueKey = jsonOp.expectedValueKey,
                                validationType = ValidationType.valueOf(jsonOp.validationType),
                                description = jsonOp.description ?: "Validate element"
                            )
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Skipping invalid operation due to error: ${e.message}")
                null
            }
        }

        private fun convertJsonSelector(jsonSelector: JsonUISelector): UISelector? {
            return when (jsonSelector.type) {
                "Compound" -> {
                    val selectors = jsonSelector.selectors?.mapNotNull { convertJsonSelector(it) } ?: emptyList()
                    if (selectors.isEmpty() && jsonSelector.selectors?.isNotEmpty() == true) {
                        Log.w(TAG, "Compound selector for app has valid sub-selectors, but all were filtered out.")
                        null
                    } else {
                        val operator = jsonSelector.operator ?: "AND"
                        UISelector.Compound(selectors, operator)
                    }
                }
                else -> {
                    val value = jsonSelector.value
                    if (value == null) {
                        Log.w(TAG, "Selector of type ${jsonSelector.type} is missing a 'value' field. Skipping.")
                        null
                    } else {
                        when (jsonSelector.type) {
                            "ByText" -> UISelector.ByText(value)
                            "ByResourceId" -> UISelector.ByResourceId(value)
                            "ByClassName" -> UISelector.ByClassName(value)
                            "ByContentDesc" -> UISelector.ByContentDesc(value)
                            "ByBounds" -> UISelector.ByBounds(value)
                            "ByXPath" -> UISelector.ByXPath(value)
                            else -> {
                                Log.w(TAG, "Unknown selector type: ${jsonSelector.type}. Skipping.")
                                null
                            }
                        }
                    }
                }
            }
        }
        
    }
}

/**
 * UI节点定义。
 *
 * @property name 节点的唯一名称，例如 "微信主页"。
 * @property packageName 所属应用的包名。
 * @property activityName 关联的Activity名称，可选。
 * @property nodeType 节点的类型。
 */
data class UINode(
    val name: String,
    val packageName: String,
    val activityName: String? = null,
    val nodeType: UINodeType,
    val matchCriteria: List<UISelector> = emptyList()
)

enum class UINodeType {
    APP_HOME, LIST_PAGE, DETAIL_PAGE, DIALOG, INPUT_PAGE, SYSTEM_PAGE
}

/**
 * 边（操作）的定义。
 * @param toNodeName 目标节点的名称。
 */
data class UIEdgeDefinition(
    val toNodeName: String,
    val operations: List<UIOperation>, // 支持多步操作
    val validation: UIOperation.ValidateElement? = null,
    val conditions: Set<String>,
    val weight: Double
) 