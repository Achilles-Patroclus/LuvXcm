package com.fenji.scoretrace.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 把相册图片压缩为 base64 data URL，供 GLM-4V 直接读取（无需上传）。
 *
 * 限制：智谱要求单图 < 5MB、像素 ≤ 6000×6000，因此先按最长边 [maxDim] 采样压缩，
 * 再以 JPEG [quality] 编码，通常可稳定压到 1MB 以内。
 */
object ImageEncoder {

    fun uriToDataUrl(
        context: Context,
        uri: Uri,
        maxDim: Int = 1600,
        quality: Int = 88,
    ): String? = runCatching {
        val bitmap = decodeScaled(context, uri, maxDim) ?: return null
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    private fun decodeScaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sample = 1
        while (width / (sample * 2) >= maxDim || height / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= maxDim) return decoded
        val scale = maxDim.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled != decoded) decoded.recycle()
        return scaled
    }
}
