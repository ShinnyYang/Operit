package com.ai.assistance.operit.data.mcp

/**
 * MCP 服务器数据模型类 - 数据层版本
 */
data class MCPServer(
    val id: String,
    val name: String,
    val description: String,
    val logoUrl: String? = null,
    val stars: Int = 0,
    val category: String = "未分类",
    val requiresApiKey: Boolean = false,
    val author: String = "Unknown",
    val isVerified: Boolean = false,
    val isInstalled: Boolean = false,
    val version: String = "",
    val updatedAt: String = "",
    val longDescription: String = "",
    val repoUrl: String = "",
    // 新增字段以支持远程服务
    val type: String = "local", // "local" or "remote"
    val endpoint: String? = null,
    val connectionType: String? = "httpStream"
) 