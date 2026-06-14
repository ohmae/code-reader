package net.mm2d.codereader.util

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <reified T : Any> Bundle.getParcelableSafely(
    key: String,
): T? =
    runCatching {
        BundleCompat.getParcelable(this, key, T::class.java)
    }.getOrNull()

@OptIn(ExperimentalContracts::class)
inline fun buildBundle(
    action: Bundle.() -> Unit,
): Bundle {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return Bundle().apply(action)
}

fun stringBundle(
    key: String,
    value: String,
): Bundle = buildBundle { putString(key, value) }

fun parcelableBundle(
    key: String,
    value: Parcelable,
): Bundle = buildBundle { putParcelable(key, value) }
