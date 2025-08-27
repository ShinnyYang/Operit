package com.ai.assistance.operit.util

import java.io.File

/**
 * Waifu模式消息处理器
 * 负责将AI回复按句号分割并模拟逐句发送
 */
object WaifuMessageProcessor {
    
    /**
     * 将完整的消息按句号分割成句子
     * @param content 完整的消息内容
     * @param removePunctuation 是否移除标点符号
     * @return 分割后的句子列表
     */
    fun splitMessageBySentences(content: String, removePunctuation: Boolean = false): List<String> {
        if (content.isBlank()) return emptyList()
        
        // 首先分离表情包和文本内容
        val separatedContent = separateEmotionAndText(content)
        
        // 处理每个分离后的内容
        val result = mutableListOf<String>()
        
        for (item in separatedContent) {
            // 如果这个item是表情包（包含![开头的），直接添加
            if (item.startsWith("![")) {
                result.add(item)
                continue
            }
            
            // 对于文本内容，进行正常的清理和分句处理
            val cleanedContent = cleanContentForWaifu(item)
            
            if (cleanedContent.isBlank()) continue
            
                            // 按句号、问号、感叹号、省略号、波浪号分割，但保留标点符号
        // 使用更精确的正则表达式，避免分割不完整
        // 更简单直接的方法：使用不同的分割策略
        val splitRegex = Regex("(?<=[。！？~～])|(?<=[.!?]{1}(?![.]))|(?<=\\.{3})|(?<=[…](?![…]))")


        android.util.Log.d("WaifuMessageProcessor", "分割正则: $splitRegex")
        android.util.Log.d("WaifuMessageProcessor", "待分割内容: '$cleanedContent'")
        
        var sentences = cleanedContent.split(splitRegex)
            .filter { it.isNotBlank() }
            .map { it.trim() }
            
            // 如果需要移除标点符号，则处理每个句子
            if (removePunctuation) {
                sentences = sentences.map { sentence ->
                    // 移除句末标点，但保留省略号"..."
                    if (sentence.endsWith("...")) {
                        sentence.trim()
                    } else {
                        sentence.replace(Regex("[。！？.!?]+$"), "").trim()
                    }
                }.filter { it.isNotBlank() }
            }
            
            result.addAll(sentences)
        }
        
        android.util.Log.d("WaifuMessageProcessor", "分割出${result.size}个结果")
        
        return result
    }
    
    /**
     * 清理内容中的状态标签和XML标签，只保留纯文本
     */
    private fun cleanContentForWaifu(content: String): String {
        return content
            // 移除状态标签
            .replace(Regex("<status[^>]*>.*?</status>"), "")
            .replace(Regex("<status[^>]*/>"), "")
            // 移除思考标签
            .replace(Regex("<think[^>]*>.*?</think>"), "")
            .replace(Regex("<think[^>]*/>"), "")
            // 移除工具标签
            .replace(Regex("<tool[^>]*>.*?</tool>"), "")
            .replace(Regex("<tool[^>]*/>"), "")
            // 移除工具结果标签
            .replace(Regex("<tool_result[^>]*>.*?</tool_result>"), "")
            .replace(Regex("<tool_result[^>]*/>"), "")
            // 移除计划项标签
            .replace(Regex("<plan_item[^>]*>.*?</plan_item>"), "")
            .replace(Regex("<plan_item[^>]*/>"), "")
            // 移除emotion标签（因为已经在processEmotionTags中处理过了）
            .replace(Regex("<emotion[^>]*>.*?</emotion>"), "")
            // 移除其他常见的XML标签
            .replace(Regex("<[^>]*>"), "")
            // 清理多余的空白
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    

    
    /**
     * 根据字符数计算句子延迟时间
     * @param characterCount 字符数
     * @param baseDelayMs 基础延迟（毫秒/字符）
     * @return 计算后的延迟时间（毫秒）
     */
    fun calculateSentenceDelay(characterCount: Int, baseDelayMs: Long): Long {
        // 基础计算：字符数 * 基础延迟
        val baseDelay = characterCount * baseDelayMs
        
        // 添加一些变化和限制：
        // 1. 短句子（<5字符）最少延迟300ms
        // 2. 长句子（>20字符）有上限3000ms
        // 3. 添加一些随机变化（±20%）使延迟更自然
        
        val minDelay = 300L
        val maxDelay = 3000L
        
        val adjustedDelay = when {
            characterCount <= 5 -> minDelay
            baseDelay > maxDelay -> maxDelay
            else -> baseDelay
        }
        
        // 添加±20%的随机变化
        val variance = (adjustedDelay * 0.2).toLong()
        val randomAdjustment = (-variance..variance).random()
        
        return (adjustedDelay + randomAdjustment).coerceAtLeast(minDelay)
    }
    
    /**
     * 检查内容是否适合进行分句处理
     * @param content 消息内容
     * @return 是否适合分句
     */
    fun shouldSplitMessage(content: String): Boolean {
        if (content.isBlank()) return false
        
        // 检查是否包含表情包标签
        val hasEmotionTags = content.contains(Regex("<emotion[^>]*>.*?</emotion>"))
        
        // 首先清理内容
        val cleanedContent = cleanContentForWaifu(content)
        if (cleanedContent.isBlank()) return false
        
        // 检查是否包含句号、问号、感叹号、波浪号或省略号（与splitMessageBySentences保持一致）
        val hasSentenceEnders = cleanedContent.contains(Regex("[。！？.!?~～…]|\\Q...\\E"))
        
        // 检查内容长度是否足够长（至少10个字符）
        val isLongEnough = cleanedContent.length >= 10
        
        // 检查是否包含多个句子 (这里不考虑标点符号移除，因为是判断是否需要分句)
        val sentences = splitMessageBySentences(content, removePunctuation = false) // 这里传入原始内容，因为splitMessageBySentences内部会清理
        val hasMultipleSentences = sentences.size > 1
        
        // 如果有表情包标签，或者满足其他条件，就进行分句处理
        val shouldSplit = hasEmotionTags || (hasSentenceEnders && isLongEnough && hasMultipleSentences)
        
        // 添加调试日志
        android.util.Log.d("WaifuMessageProcessor", 
            "shouldSplitMessage - 包含表情包: $hasEmotionTags, 句子数: ${sentences.size}, 结果: $shouldSplit")
        
        return shouldSplit
    }
    
    /**
     * 处理表情包标签，将<emotion>标签替换为对应的表情图片
     * @param content 包含emotion标签的内容
     * @return 处理后的内容，emotion标签被替换为表情图片
     */
    fun processEmotionTags(content: String): String {
        if (content.isBlank()) return content
        
        // 匹配<emotion>标签的正则表达式
        val emotionRegex = Regex("<emotion>([^<]+)</emotion>")
        
        return emotionRegex.replace(content) { matchResult ->
            val emotion = matchResult.groupValues[1].trim()
            val emojiPath = getRandomEmojiPath(emotion)
            
            if (emojiPath != null) {
                // 返回Markdown格式的图片链接，使用正确的Android assets路径
                // 对文件名进行URL编码，处理空格和特殊字符
                val encodedPath = emojiPath.replace(" ", "%20").replace("(", "%28").replace(")", "%29")
                "![$emotion](file:///android_asset/emoji/$encodedPath)"
            } else {
                // 如果找不到对应的表情，返回原始文本
                matchResult.value
            }
        }
    }
    
    /**
     * 分离表情包和文本内容
     * @param content 包含emotion标签的内容
     * @return 包含文本内容和表情包内容的列表，表情包会单独作为一个元素
     */
    fun separateEmotionAndText(content: String): List<String> {
        if (content.isBlank()) return listOf(content)
        
        val result = mutableListOf<String>()
        val emotionRegex = Regex("<emotion>([^<]+)</emotion>")
        
        // 找到所有emotion标签的位置
        val matches = emotionRegex.findAll(content)
        var lastEnd = 0
        
        for (match in matches) {
            // 添加emotion标签之前的文本（如果有的话）
            val beforeText = content.substring(lastEnd, match.range.first).trim()
            if (beforeText.isNotEmpty()) {
                result.add(beforeText)
            }
            
            // 处理emotion标签
            val emotion = match.groupValues[1].trim()
            val emojiPath = getRandomEmojiPath(emotion)
            
            if (emojiPath != null) {
                // 添加表情包作为单独的元素，对文件名进行URL编码
                val encodedPath = emojiPath.replace(" ", "%20").replace("(", "%28").replace(")", "%29")
                result.add("![$emotion](file:///android_asset/emoji/$encodedPath)")
            }
            
            lastEnd = match.range.last + 1
        }
        
        // 添加最后一个emotion标签之后的文本（如果有的话）
        val afterText = content.substring(lastEnd).trim()
        if (afterText.isNotEmpty()) {
            result.add(afterText)
        }
        
        // 如果没有找到任何emotion标签，返回原始内容
        if (result.isEmpty()) {
            result.add(content)
        }
        
        android.util.Log.d("WaifuMessageProcessor", "分离表情包和文本: ${result.size}个元素")
        return result
    }
    
    /**
     * 根据情绪名称获取随机的表情图片路径
     * @param emotion 情绪名称（如：开心、难过、想你等）
     * @return 表情图片的相对路径，如果找不到则返回null
     */
    private fun getRandomEmojiPath(emotion: String): String? {
        try {
            // 由于在Android运行时无法直接访问assets目录的文件系统，
            // 我们直接返回相对路径，让UI层去处理图片加载
            // 根据assets目录结构，我们知道存在这些情绪文件夹
            val supportedEmotions = listOf("哭泣", "喜欢你", "开心", "惊讶", "想你", "无语", "生气", "疑惑", "难过")
            
            if (emotion !in supportedEmotions) {
                android.util.Log.w("WaifuMessageProcessor", "不支持的情绪类型: $emotion")
                return null
            }
            
            // 根据情绪类型返回对应的图片文件名（与JavaScript代码中的EMOJI_CATALOG保持一致）
            val imageFiles = when (emotion) {
                "开心" -> listOf("157.webp", "161.webp", "1.jpg", "2.jpg", "下载 (3).jpg")
                "难过" -> listOf("82.webp", "12.gif", "170.gif", "下载.jpg", "下载 (1).jpg", "下载 (2).jpg")
                "无语" -> listOf("57.webp", "69.jpg", "89f302f0fea4784c3fdb74f128fccd948e9247ae.jpg", "下载.jpg", "下载 (1).jpg", "下载 (2).jpg")
                "生气" -> listOf("39.webp", "60.gif", "e1823fbd2683c131043368449b08801b75105700.gif", "下载.jpg", "下载 (1).jpg", "下载 (3).jpg")
                "惊讶" -> listOf("204.webp", "9.webp", "006GJQvhgy1fyua0uy5blg3092084qh6.gif", "006APoFYly8gptue9vpruj30u00u00ue.jpg")
                "疑惑" -> listOf("186.webp", "61.png", "def14c7633eead05047b1526e25520161527419a.jpg", "下载.jpg", "下载 (1).jpg", "下载 (4).jpg")
                "想你" -> listOf("157.webp", "177.webp", "005XSXmNgy1gqfodb9znwj30i30hsq3t.jpg", "b64da6adly1gk2nb435pbj20j60j6abw.jpg")
                "喜欢你" -> listOf("147.webp", "38.gif", "下载.jpg", "下载 (1).jpg", "下载 (2).jpg", "下载 (3).jpg")
                "哭泣" -> listOf("141.webp", "54.webp", "97.png", "下载.jpg", "下载 (1).jpg", "下载 (2).jpg")
                else -> emptyList()
            }
            
            if (imageFiles.isEmpty()) {
                android.util.Log.w("WaifuMessageProcessor", "情绪文件夹中没有图片文件: $emotion")
                return null
            }
            
            // 随机选择一张图片
            val randomImage = imageFiles.random()
            val relativePath = "$emotion/$randomImage"
            
            android.util.Log.d("WaifuMessageProcessor", "选择表情图片: $relativePath")
            return relativePath
            
        } catch (e: Exception) {
            android.util.Log.e("WaifuMessageProcessor", "获取表情图片失败: $emotion", e)
            return null
        }
    }
} 