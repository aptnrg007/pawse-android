package com.pawse.app.picker

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * PackageManager label/icon lookups are slow enough that doing them on every
 * recomposition stutters the list — cache once per package for the process lifetime.
 */
object AppIconCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun get(context: Context, packageName: String): ImageBitmap =
        cache.getOrPut(packageName) {
            context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        }
}
