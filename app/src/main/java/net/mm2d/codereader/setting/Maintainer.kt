/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.setting

import net.mm2d.codereader.BuildConfig
import net.mm2d.codereader.setting.Key.Main

object Maintainer {
    private const val SETTINGS_VERSION = 1

    fun maintain(preferences: Preferences<Main>) {
        Main.values().checkSuffix()
        if (preferences.readInt(Main.APP_VERSION_AT_LAST_LAUNCHED_INT, 0)
            != BuildConfig.VERSION_CODE
        ) {
            preferences.writeInt(Main.APP_VERSION_AT_LAST_LAUNCHED_INT, BuildConfig.VERSION_CODE)
        }
        val settingsVersion = preferences.readInt(Main.PREFERENCES_VERSION_INT, 0)
        if (settingsVersion == SETTINGS_VERSION) {
            return
        }
        if (!preferences.contains(Main.APP_VERSION_AT_INSTALL_INT)) {
            preferences.writeInt(Main.APP_VERSION_AT_INSTALL_INT, BuildConfig.VERSION_CODE)
        }
        preferences.writeInt(Main.PREFERENCES_VERSION_INT, SETTINGS_VERSION)
        writeDefaultValue(preferences)
    }

    private fun writeDefaultValue(preferences: Preferences<Main>) {
        preferences.writeLong(Main.TIME_FIRST_USE_LONG, 0L)
        preferences.writeLong(Main.TIME_FIRST_REVIEW_LONG, 0L)
        preferences.writeInt(Main.COUNT_DETECT_VALUE_ACTION_INT, 0)
        preferences.writeInt(Main.COUNT_REVIEW_DIALOG_CANCELED_INT, 0)
        preferences.writeBoolean(Main.REVIEW_REPORTED_BOOLEAN, false)
        preferences.writeBoolean(Main.REVIEW_REVIEWED_BOOLEAN, false)
    }
}
