package com.ai.assistance.operit.api.chat

import java.net.URL

/**
 * 用于自动补全API端点URL的工具类。
 */
object EndpointCompleter {

    /**
     * 为类似OpenAI的服务自动补全API端点URL。
     * 如果端点是一个基础URL（例如 https://api.example.com），它会自动附加通用的路径，如 /v1/chat/completions。
     * 用户可以在URL末尾添加 '#' 来禁用此功能。
     *
     * @param endpoint 用户提供的端点URL。
     * @return 补全后的或原始的端点URL。
     */
    fun completeEndpoint(endpoint: String): String {
        val trimmedEndpoint = endpoint.trim()
        if (trimmedEndpoint.endsWith("#")) {
            return trimmedEndpoint.removeSuffix("#")
        }

        val endpointWithoutSlash = trimmedEndpoint.removeSuffix("/")

        // 尝试解析URL并判断它是否为一个需要补全的URL
        try {
            val url = URL(endpointWithoutSlash)
            val path = url.path

            // 如果URL路径为空、为"/"或为"/v1"，则进行补全
            if (path.isNullOrEmpty() || path == "/" || path.equals("/v1", ignoreCase = true)) {
                val baseUrl = "${url.protocol}://${url.authority}"
                return "$baseUrl/v1/chat/completions"
            }
        } catch (e: Exception) {
            // 如果不是一个有效的URL，则不进行任何操作
        }
        
        // 如果不符合补全特征，则返回原始输入
        return endpoint
    }
} 