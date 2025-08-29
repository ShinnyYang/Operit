
package com.ai.assistance.operit.ui.features.chat.components.style.bubble

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import android.util.Log
import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.data.preferences.UserPreferencesManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BubbleUserMessageComposable(
    message: ChatMessage,
    backgroundColor: Color,
    textColor: Color
) {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager(context) }
    val customUserAvatarUri by preferencesManager.customUserAvatarUri.collectAsState(initial = null)
    val globalUserAvatarUri by preferencesManager.globalUserAvatarUri.collectAsState(initial = null)
    val avatarShapePref by preferencesManager.avatarShape.collectAsState(initial = UserPreferencesManager.AVATAR_SHAPE_CIRCLE)
    val avatarCornerRadius by preferencesManager.avatarCornerRadius.collectAsState(initial = 8f)
    val clipboardManager = LocalClipboardManager.current

    // 头像回退逻辑：优先使用角色卡专属头像，为空时使用全局头像
    val avatarUri = remember(customUserAvatarUri, globalUserAvatarUri) {
        when {
            !customUserAvatarUri.isNullOrEmpty() -> customUserAvatarUri
            !globalUserAvatarUri.isNullOrEmpty() -> globalUserAvatarUri
            else -> null
        }
    }
    val avatarShape = remember(avatarShapePref, avatarCornerRadius) {
        if (avatarShapePref == UserPreferencesManager.AVATAR_SHAPE_SQUARE) {
            RoundedCornerShape(avatarCornerRadius.dp)
        } else {
            CircleShape
        }
    }

    // 添加状态控制内容预览
    val showContentPreview = remember { mutableStateOf(false) }
    val selectedAttachmentContent = remember { mutableStateOf("") }
    val selectedAttachmentName = remember { mutableStateOf("") }

    // Parse message content to separate text and attachments
    val parseResult = remember(message.content) { parseMessageContent(message.content) }
    val textContent = parseResult.processedText
    val trailingAttachments = parseResult.trailingAttachments

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Display trailing attachments above the message bubble
        if (trailingAttachments.isNotEmpty()) {
            // Display attachment row above the bubble
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailingAttachments.forEach { attachment ->
                    AttachmentTag(
                        filename = attachment.filename,
                        type = attachment.type,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        content = attachment.content,
                        onClick = { content, name ->
                            // 当点击附件标签时，显示内容预览
                            if (content.isNotEmpty()) {
                                selectedAttachmentContent.value = content
                                selectedAttachmentName.value = name
                                showContentPreview.value = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        // Message bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            // Message bubble
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(start = 64.dp)
                    .defaultMinSize(minHeight = 44.dp),
                shape = RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp),
                color = backgroundColor,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = textContent,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Avatar
            if (!avatarUri.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = Uri.parse(avatarUri)),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(avatarShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(avatarShape),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // 内容预览对话框
    if (showContentPreview.value) {
        Dialog(onDismissRequest = { showContentPreview.value = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 头部
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedAttachmentName.value,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                showContentPreview.value = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 内容区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top)
                            .weight(1f, fill = false)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = selectedAttachmentContent.value,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 复制按钮
                    Button(
                        onClick = {
                            clipboardManager.setText(
                                AnnotatedString(selectedAttachmentContent.value)
                            )
                            showContentPreview.value = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { 
                        Text("复制内容") 
                    }
                }
            }
        }
    }
}

/** Result of parsing message content, containing processed text and trailing attachments */
data class MessageParseResult(
    val processedText: String,
    val trailingAttachments: List<AttachmentData>
)

/**
 * Parses the message content to extract text and attachments Keeps inline attachments as @filename
 * in the text Extracts trailing attachments that appear at the end of the message
 */
private fun parseMessageContent(content: String): MessageParseResult {
    // First, strip out any <memory> tags so they are not displayed in the UI.
    val cleanedContent = content.replace(Regex("<memory>.*?</memory>", RegexOption.DOT_MATCHES_ALL), "").trim()

    val attachments = mutableListOf<AttachmentData>()
    val trailingAttachments = mutableListOf<AttachmentData>()
    val messageText = StringBuilder()

    // 先用简单的分割方式检测有没有附件标签
    if (!cleanedContent.contains("<attachment")) {
        return MessageParseResult(cleanedContent, emptyList())
    }

    try {
        // Enhanced regex pattern to find attachments with optional content attribute
        // 注意：由于content属性可能包含json数据，我们用非贪婪匹配确保正确解析
        val attachmentPattern =
            "<attachment\\s+id=\"([^\"]+)\"\\s+filename=\"([^\"]+)\"\\s+type=\"([^\"]+)\"(?:\\s+size=\"([^\"]+)\")?(?:\\s+content=\"(.*?)\")?\\s*/>".toRegex(
                RegexOption.DOT_MATCHES_ALL
            )

        // Get all matches
        val matches = attachmentPattern.findAll(cleanedContent).toList()
        if (matches.isEmpty()) {
            return MessageParseResult(cleanedContent, emptyList())
        }

        // Find the last non-whitespace character after the last attachment
        val lastMatch = matches.last()
        val contentAfterLastMatch = cleanedContent.substring(lastMatch.range.last + 1).trim()

        // Process all attachments
        var lastIndex = 0
        matches.forEachIndexed { index, matchResult ->
            // Add text before this attachment
            val startIndex = matchResult.range.first
            if (startIndex > lastIndex) {
                messageText.append(cleanedContent.substring(lastIndex, startIndex))
            }

            // Extract attachment data
            val id = matchResult.groupValues[1]
            val filename = matchResult.groupValues[2]
            val type = matchResult.groupValues[3]
            val size = matchResult.groupValues[4].toLongOrNull() ?: 0L
            val attachmentContent = matchResult.groupValues[5]

            // Create attachment data object, including content if available
            val attachment =
                AttachmentData(
                    id = id,
                    filename = filename,
                    type = type,
                    size = size,
                    content = attachmentContent
                )

            // Determine if this is a trailing attachment
            val isLastAttachment = index == matches.size - 1
            val isTrailingAttachment =
                isLastAttachment && contentAfterLastMatch.isEmpty()

            // 特殊处理屏幕内容附件，始终将其作为trailing attachment
            val isScreenContent =
                (type == "text/json" && filename == "screen_content.json")

            if (isTrailingAttachment || isScreenContent) {
                // This is a trailing attachment, extract it
                trailingAttachments.add(attachment)
            } else {
                // This is an inline attachment, keep it in the text as @filename
                messageText.append("@${filename}")
                // Also add to general attachments list for reference
                attachments.add(attachment)
            }

            lastIndex = matchResult.range.last + 1
        }

        // Add any remaining text
        if (lastIndex < cleanedContent.length) {
            messageText.append(cleanedContent.substring(lastIndex))
        }

        return MessageParseResult(messageText.toString(), trailingAttachments)
    } catch (e: Exception) {
        // 如果解析失败，返回原始内容
        android.util.Log.e("BubbleUserMessageComposable", "解析消息内容失败", e)
        return MessageParseResult(cleanedContent, emptyList())
    }
}

/** Data class for attachment information */
data class AttachmentData(
    val id: String,
    val filename: String,
    val type: String,
    val size: Long = 0,
    val content: String = "" // Added content field
)

/** Compact attachment tag component for displaying in user messages */
@Composable
private fun AttachmentTag(
    filename: String,
    type: String,
    textColor: Color,
    backgroundColor: Color,
    content: String = "",
    onClick: (String, String) -> Unit = { _, _ -> }
) {
    // 根据附件类型选择图标
    val icon: ImageVector =
        when {
            type.startsWith("image/") -> Icons.Default.Image
            type == "text/json" && filename == "screen_content.json" ->
                Icons.Default.ScreenshotMonitor
            else -> Icons.Default.Description
        }

    // 根据附件类型调整显示标签
    val displayLabel =
        when {
            type == "text/json" && filename == "screen_content.json" -> "屏幕内容"
            else -> filename
        }

    Surface(
        modifier = Modifier
            .height(24.dp)
            .padding(vertical = 2.dp)
            .clickable(
                enabled = content.isNotEmpty(),
                onClick = { onClick(content, filename) }
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = textColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
} 