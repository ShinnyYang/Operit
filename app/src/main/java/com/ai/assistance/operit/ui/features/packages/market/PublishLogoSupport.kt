package com.ai.assistance.operit.ui.features.packages.market

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ai.assistance.operit.ui.common.icons.LogoBitmapLoader
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublishLogoReadException(cause: Throwable? = null) : IllegalArgumentException(cause)

class PublishLogoTooLargeException : IllegalArgumentException()

suspend fun readPublishLogoAsset(
    context: Context,
    uri: Uri
): PublishLogoAsset = withContext(Dispatchers.IO) {
    try {
        val fileName =
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0).orEmpty().trim() else ""
                }
                .orEmpty()
        if (fileName.isBlank()) {
            throw IllegalArgumentException("Logo file name is missing")
        }

        val contentType = logoContentTypeForFileName(fileName)
        val bytes =
            context.contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    if (output.size() + read > PUBLISH_LOGO_MAX_BYTES) {
                        throw PublishLogoTooLargeException()
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: throw IllegalArgumentException("Logo content is unavailable")

        val preview =
            LogoBitmapLoader.load(
                bytes = bytes,
                mimeType = contentType,
                fileName = fileName,
                sizePx = 96
            ) ?: throw IllegalArgumentException("Logo image cannot be decoded")
        preview.recycle()

        PublishLogoAsset(
            fileName = fileName,
            contentType = contentType,
            bytes = bytes
        )
    } catch (error: PublishLogoTooLargeException) {
        throw error
    } catch (error: Exception) {
        throw PublishLogoReadException(error)
    }
}

fun logoContentTypeForFileName(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> throw IllegalArgumentException("Unsupported logo format")
    }
}
