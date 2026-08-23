package com.ai.assistance.operit.plugins.toolpkg

import com.ai.assistance.operit.core.tools.packTool.PackageManager
import com.ai.assistance.operit.core.tools.packTool.TOOLPKG_EVENT_CHAT_VIEW_SLOT
import com.ai.assistance.operit.core.tools.packTool.ToolPkgContainerRuntime
import com.ai.assistance.operit.plugins.chatview.ChatViewSlotPlugin
import com.ai.assistance.operit.plugins.chatview.ChatViewSlotPluginRegistry
import com.ai.assistance.operit.plugins.chatview.ChatViewSlotRenderParams
import com.ai.assistance.operit.plugins.chatview.ChatViewSlotRenderResult
import com.ai.assistance.operit.util.AppLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ToolPkgChatViewSlotBridge"

internal object ToolPkgChatViewSlotBridge : ChatViewSlotPlugin {
    private val installed = AtomicBoolean(false)
    @Volatile
    private var hooks: List<ToolPkgChatViewSlotHookRegistration> = emptyList()
    private val runtimeChangeListener =
        PackageManager.ToolPkgRuntimeChangeListener { activeContainers ->
            syncToolPkgRegistrations(activeContainers)
        }

    override val id: String = "builtin.toolpkg.chat-view-slot-bridge"

    fun register() {
        if (!installed.compareAndSet(false, true)) {
            return
        }
        ChatViewSlotPluginRegistry.register(this)

        val manager = toolPkgPackageManager()
        manager.addToolPkgRuntimeChangeListener(runtimeChangeListener)
        syncToolPkgRegistrations(manager.getEnabledToolPkgContainerRuntimes())
    }

    override fun supports(slot: String): Boolean {
        val slotName = slot.trim().lowercase()
        return slotName.isNotBlank() && hooks.any { hook -> hook.slot == slotName }
    }

    override suspend fun resolve(params: ChatViewSlotRenderParams): List<ChatViewSlotRenderResult> {
        val slotName = params.slot.trim().lowercase()
        val matchedHooks = hooks.filter { hook -> hook.slot == slotName }
        if (matchedHooks.isEmpty()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            val manager = toolPkgPackageManager()
            val budget = ToolPkgHookExecutionBudget.create()
            val results = mutableListOf<ChatViewSlotRenderResult>()
            matchedHooks.forEach { hook ->
                val timeoutMillis = budget.remainingMillis()
                if (timeoutMillis == null) {
                    budget.logDeadlineReached(
                        tag = TAG,
                        stage = "render",
                        containerPackageName = hook.containerPackageName,
                        hookId = hook.pluginId
                    )
                    return@forEach
                }
                val rawResult =
                    manager.runToolPkgMainHook(
                        containerPackageName = hook.containerPackageName,
                        functionName = hook.functionName,
                        event = TOOLPKG_EVENT_CHAT_VIEW_SLOT,
                        eventName = "render",
                        pluginId = hook.pluginId,
                        inlineFunctionSource = hook.functionSource,
                        eventPayload = buildChatViewSlotEventPayload(params.copy(slot = slotName)),
                        timeoutMillis = timeoutMillis
                    )
                if (
                    budget.logTimeoutIfPresent(
                        result = rawResult,
                        tag = TAG,
                        stage = "render",
                        containerPackageName = hook.containerPackageName,
                        hookId = hook.pluginId
                    )
                ) {
                    return@forEach
                }
                val rawValue =
                    rawResult.getOrElse { error ->
                        AppLogger.e(
                            TAG,
                            "ToolPkg chat view slot hook failed: ${hook.containerPackageName}:${hook.pluginId}",
                            error
                        )
                        return@getOrElse null
                    } ?: return@forEach
                val decoded =
                    runCatching { decodeToolPkgHookResult(rawValue) }
                        .getOrElse { error ->
                            AppLogger.e(
                                TAG,
                                "ToolPkg chat view slot hook decode failed: ${hook.containerPackageName}:${hook.pluginId}",
                                error
                            )
                            null
                        }
                parseChatViewSlotResult(
                    decoded = decoded,
                    containerPackageName = hook.containerPackageName
                )?.let { result -> results.add(result) }
            }
            results
        }
    }

    private fun syncToolPkgRegistrations(activeContainers: List<ToolPkgContainerRuntime>) {
        val nextHooks =
            activeContainers.flatMap { runtime ->
                runtime.chatViewSlotPlugins.map { hook ->
                    ToolPkgChatViewSlotHookRegistration(
                        containerPackageName = runtime.packageName,
                        pluginId = hook.id,
                        slot = hook.slot,
                        functionName = hook.function,
                        functionSource = hook.functionSource
                    )
                }
            }.sortedWith(
                compareBy(
                    ToolPkgChatViewSlotHookRegistration::slot,
                    ToolPkgChatViewSlotHookRegistration::containerPackageName,
                    ToolPkgChatViewSlotHookRegistration::pluginId
                )
            )
        if (hooks == nextHooks) {
            return
        }
        hooks = nextHooks
        ChatViewSlotPluginRegistry.notifyChanged()
    }

    private fun buildChatViewSlotEventPayload(params: ChatViewSlotRenderParams): Map<String, Any?> =
        mapOf(
            "slot" to params.slot,
            "chatId" to params.chatId,
            "runtime" to params.runtime,
            "inputStyle" to params.inputStyle,
            "isProcessing" to params.isProcessing,
            "isInputFocused" to params.isInputFocused,
            "inputText" to params.inputText
        )

    private fun parseChatViewSlotResult(
        decoded: Any?,
        containerPackageName: String
    ): ChatViewSlotRenderResult? {
        return when (decoded) {
            is String -> {
                val text = decoded.trim()
                if (text.isBlank()) null else ChatViewSlotRenderResult.Text(text)
            }

            is JSONObject -> {
                if (!decoded.optBoolean("handled", true)) {
                    return null
                }
                val composeDsl = parseComposeDslResult(decoded.opt("composeDsl"), containerPackageName)
                if (composeDsl != null) {
                    return composeDsl
                }
                val text = decoded.optString("text").ifBlank { decoded.optString("content") }.trim()
                if (text.isBlank()) null else ChatViewSlotRenderResult.Text(text)
            }

            else -> null
        }
    }

    private fun parseComposeDslResult(
        raw: Any?,
        containerPackageName: String
    ): ChatViewSlotRenderResult.ComposeDslScreen? {
        val map =
            when (raw) {
                is JSONObject -> raw
                is Map<*, *> -> JSONObject(raw)
                else -> null
            } ?: return null
        val screen = map.optString("screen").trim()
        if (screen.isBlank()) {
            return null
        }

        val state = asMap(map.opt("state"))
        val memo = asMap(map.opt("memo"))
        val moduleSpec = asMap(map.opt("moduleSpec"))
        return ChatViewSlotRenderResult.ComposeDslScreen(
            containerPackageName = containerPackageName,
            screenPath = screen,
            state = state,
            memo = memo,
            moduleSpec = moduleSpec.takeIf { it.isNotEmpty() }
        )
    }

    private fun asMap(value: Any?): Map<String, Any?> {
        return when (value) {
            is JSONObject -> {
                val map = linkedMapOf<String, Any?>()
                value.keys().forEach { key ->
                    map[key] = normalizeValue(value.opt(key))
                }
                map
            }

            is Map<*, *> -> {
                value.entries.associate { entry ->
                    entry.key.toString() to normalizeValue(entry.value)
                }
            }

            else -> emptyMap()
        }
    }

    private fun asList(value: Any?): List<Any?> {
        return when (value) {
            is JSONArray -> {
                buildList {
                    for (index in 0 until value.length()) {
                        add(normalizeValue(value.opt(index)))
                    }
                }
            }

            is List<*> -> value.map { normalizeValue(it) }
            else -> emptyList()
        }
    }

    private fun normalizeValue(value: Any?): Any? {
        return when (value) {
            null, JSONObject.NULL -> null
            is JSONObject -> asMap(value)
            is JSONArray -> asList(value)
            else -> value
        }
    }
}
