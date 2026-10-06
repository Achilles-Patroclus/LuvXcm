package com.fenji.scoretrace.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 头像存储：把用户选中的图片复制到 App 私有目录并返回本地路径。
 *
 * 不持久化外部 Uri（避免重启后授权失效），也不申请存储权限——只读写自身私有目录。
 * 每次保存生成新文件名，旧的会被清理，保证 Coil 按路径缓存不会拿到旧图。
 */
object AvatarStore {

    private const val DIR = "avatars"

    suspend fun save(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            dir.listFiles()
                ?.filter { it.isFile && it.name != file.name }
                ?.forEach { it.delete() }
            file.absolutePath
        }.getOrNull()
    }
}
