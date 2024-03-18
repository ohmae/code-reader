package net.mm2d.codereader.util

import android.os.Bundle
import androidx.core.os.BundleCompat

inline fun <reified T : Any> Bundle.getParcelableSafely(key: String): T? =
    runCatching {
        BundleCompat.getParcelable(this, key, T::class.java)
    }.getOrNull()
