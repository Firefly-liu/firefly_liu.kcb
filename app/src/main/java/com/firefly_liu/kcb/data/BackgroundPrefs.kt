package com.firefly_liu.kcb.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** 背景设置：图片存应用私有目录，透明度存 SharedPreferences */
class BackgroundPrefs(context: Context) {

    private val appContext = context.applicationContext
    private val sp = appContext.getSharedPreferences("kcb_bg", Context.MODE_PRIVATE)

    val file: File get() = File(appContext.filesDir, "background.jpg")

    var alpha: Float
        get() = sp.getFloat("alpha", 0.28f)
        set(value) = sp.edit().putFloat("alpha", value.coerceIn(0.05f, 0.9f)).apply()

    fun hasBackground(): Boolean = file.exists()

    fun clear() {
        if (file.exists()) file.delete()
    }

    /** 采样解码，避免大图 OOM */
    fun loadBitmap(maxWidth: Int = 1080): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxWidth) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }
}
