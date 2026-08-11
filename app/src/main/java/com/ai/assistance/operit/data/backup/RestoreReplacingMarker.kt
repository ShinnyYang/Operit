package com.ai.assistance.operit.data.backup

import android.content.Context
import com.ai.assistance.operit.data.stats.TokenStatSpool
import com.ai.assistance.operit.util.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Raw restore 的 REPLACING 持久化提交标记（P1 终审）。
 *
 * [TokenStatSpool.withExclusiveRestoreAccess] 的 [commitReplacement] 必须在任何文件替换
 * （block）之前把“替换已开始”持久化：只有该标记成功落盘后 restore epoch 才会递增、
 * 本进程才停止接受新的统计事件——恢复前开始的旧请求绝不写入可能已被替换的 spool/Room。
 *
 * 持久化级别：文件内容 fd.sync，再同步父目录目录项（[TokenStatSpool.syncDir]，与 spool
 * 目录项协议一致）；任一非 OK 即抛 [IOException]（fail-closed），屏障视恢复未开始，
 * 旧/新请求均可继续。
 *
 * 标记只在恢复成功完成后删除；替换开始后失败的恢复保留标记（替换结果不确定，进程
 * 必须重启）。标记位于 filesDir 根：Raw restore 的 files 替换把标记加入 preserved 名单
 * （崩溃后启动路径 [TokenStatSpool.consumeAbandonedRestoreIfAny] 消费），Room restore
 * 不替换 files，由调用方成功后显式删除。
 */
internal object RestoreReplacingMarker {
    const val FILE_NAME = TokenStatSpool.RESTORE_REPLACING_MARKER_FILE_NAME
    private const val TAG = "RestoreReplacing"

    /**
     * 持久化 REPLACING 标记；失败抛 [IOException]（fail-closed）。调用方持
     * [TokenStatSpool] 的 lifecycleMutex（屏障内部），目录 sync seam 与 spool 一致。
     */
    fun persist(context: Context) {
        val flag = File(context.filesDir, FILE_NAME)
        try {
            FileOutputStream(flag).use { output ->
                output.write("REPLACING\n".toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            if (TokenStatSpool.syncDir(context.filesDir) != TokenStatSpool.DirSyncResult.OK) {
                throw IOException("restore REPLACING marker directory entry not durable")
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "REPLACING marker write failed", e)
            throw IOException("restore REPLACING marker could not be persisted", e)
        }
    }

    /**
     * 恢复成功完成后删除标记。删除必须持久化（同步父目录）；删除失败抛 [IOException]，
     * 使调用方把恢复报告为未确认完成——残留标记会由启动路径 [TokenStatSpool.consumeAbandonedRestoreIfAny]
     * 兜底消费，绝不让不确定状态静默通过。
     */
    fun delete(context: Context) {
        val flag = File(context.filesDir, FILE_NAME)
        if (!flag.exists()) return
        if (!flag.delete()) {
            throw IOException("restore REPLACING marker could not be deleted: ${flag.absolutePath}")
        }
        if (TokenStatSpool.syncDir(context.filesDir) != TokenStatSpool.DirSyncResult.OK) {
            throw IOException("restore REPLACING marker deletion not durable")
        }
    }
}
