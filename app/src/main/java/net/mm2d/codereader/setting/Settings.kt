/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.setting

import android.content.Context
import net.mm2d.codereader.setting.Key.Main

class Settings private constructor(
    private val preferences: Preferences<Main>
) {
    var reviewIntervalRandomFactor: Long
        get() = preferences.readLong(Main.REVIEW_INTERVAL_RANDOM_FACTOR_LONG, 0L)
        set(value) = preferences.writeLong(Main.REVIEW_INTERVAL_RANDOM_FACTOR_LONG, value)

    var firstUseTime: Long
        get() = preferences.readLong(Main.TIME_FIRST_USE_LONG, 0L)
        set(value) = preferences.writeLong(Main.TIME_FIRST_USE_LONG, value)

    var firstReviewTime: Long
        get() = preferences.readLong(Main.TIME_FIRST_REVIEW_LONG, 0L)
        set(value) = preferences.writeLong(Main.TIME_FIRST_REVIEW_LONG, value)

    var detectValueActionCount: Int
        get() = preferences.readInt(Main.COUNT_DETECT_VALUE_ACTION_INT, 0)
        set(value) = preferences.writeInt(Main.COUNT_DETECT_VALUE_ACTION_INT, value)

    var reviewCancelCount: Int
        get() = preferences.readInt(Main.COUNT_REVIEW_DIALOG_CANCELED_INT, 0)
        set(value) = preferences.writeInt(Main.COUNT_REVIEW_DIALOG_CANCELED_INT, value)

    var reviewed: Boolean
        get() = preferences.readBoolean(Main.REVIEW_REVIEWED_BOOLEAN, false)
        set(value) = preferences.writeBoolean(Main.REVIEW_REVIEWED_BOOLEAN, value)

    var reported: Boolean
        get() = preferences.readBoolean(Main.REVIEW_REPORTED_BOOLEAN, false)
        set(value) = preferences.writeBoolean(Main.REVIEW_REPORTED_BOOLEAN, value)

    companion object {
        private lateinit var settings: Settings

        fun initialize(context: Context) {
            Preferences<Main>(context, Main.FILE_NAME).also {
                Maintainer.maintain(it)
                settings = Settings(it)
            }
        }
    }
}
