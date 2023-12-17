/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.setting

import net.mm2d.codereader.BuildConfig

interface Key {

    enum class Main : Key {
        PREFERENCES_VERSION_INT,
        APP_VERSION_AT_INSTALL_INT,
        APP_VERSION_AT_LAST_LAUNCHED_INT,
        TIME_FIRST_USE_LONG,
        REVIEW_INTERVAL_RANDOM_FACTOR_LONG,
        COUNT_DETECT_VALUE_ACTION_INT,
        REVIEW_REVIEWED_BOOLEAN,
        VIBRATE_BOOLEAN,
        ;

        companion object {
            const val FILE_NAME: String = "Main"
        }
    }
}

private const val SUFFIX_BOOLEAN = "_BOOLEAN"
private const val SUFFIX_INT = "_INT"
private const val SUFFIX_LONG = "_LONG"
private const val SUFFIX_FLOAT = "_FLOAT"
private const val SUFFIX_STRING = "_STRING"
private val SUFFIXES =
    listOf(
        SUFFIX_BOOLEAN,
        SUFFIX_INT,
        SUFFIX_LONG,
        SUFFIX_FLOAT,
        SUFFIX_STRING,
    )

internal fun Collection<Enum<*>>.checkSuffix() {
    if (!BuildConfig.DEBUG) return
    forEach { key ->
        require(SUFFIXES.any { key.name.endsWith(it) }) { "$key has no type suffix." }
    }
}

internal fun Enum<*>.checkSuffix(value: Any) {
    if (!BuildConfig.DEBUG) return
    when (value) {
        is Boolean -> require(name.endsWith(SUFFIX_BOOLEAN)) {
            "$this is used for Boolean, suffix \"$SUFFIX_BOOLEAN\" is required."
        }

        is Int -> require(name.endsWith(SUFFIX_INT)) {
            "$this is used for Int, suffix \"$SUFFIX_INT\" is required."
        }

        is Long -> require(name.endsWith(SUFFIX_LONG)) {
            "$this is used for Long, suffix \"$SUFFIX_LONG\" is required."
        }

        is Float -> require(name.endsWith(SUFFIX_FLOAT)) {
            "$this is used for Float, suffix \"$SUFFIX_FLOAT\" is required."
        }

        is String -> require(name.endsWith(SUFFIX_STRING)) {
            "$this is used for String, suffix \"$SUFFIX_STRING\" is required."
        }
    }
}
