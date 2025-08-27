package com.ai.assistance.operit.ui.features.packages.screens.mcp.model

/** Data class representing an MCP (Mobile Code Plugin) server. */
data class MCPServer(
        val id: String,
        val name: String,
        val description: String,
        val logoUrl: String?,
        val stars: Int,
        val category: String,
        val requiresApiKey: Boolean = false,
        val author: String = "Unknown",
        val isVerified: Boolean = false,
        val isInstalled: Boolean = false,
        val version: String = "1.0.0",
        val updatedAt: String = "",
        val longDescription: String = "",
        val repoUrl: String = "",

        // 新增字段以支持远程服务
        val type: String = "local", // "local" or "remote"
        val endpoint: String? = null,
        val connectionType: String? = null,

        // 新增字段，支持标准格式
        val isTested: Boolean = false, // 是否通过测试
        val isStable: Boolean = false, // 是否标记为稳定
        val submissionDate: String = "", // 提交日期
        val tags: List<String> = emptyList(), // 标签列表
        val logoStandardSize: Boolean = false // logo是否符合标准尺寸
) {
        // Add a computed property to access updatedAt as lastUpdated for UI compatibility
        val lastUpdated: String
                get() = updatedAt

        // 判断是否为Beta状态 (未测试或未标记为稳定)
        val isBeta: Boolean
                get() = !isTested || !isStable

        // 是否完全符合提交标准
        val isStandardCompliant: Boolean
                get() = repoUrl.isNotEmpty() && logoUrl != null && isTested && isStable
}
