/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.util

import android.content.Context
import android.content.Intent

object CustomTabsHelper {
    private val PREFERRED_PACKAGES = listOf(
        "com.android.chrome", // Chrome
        "org.mozilla.firefox", // Firefox
        "com.microsoft.emmx", // Microsoft Edge
    )
    private const val ACTION_CUSTOM_TABS_CONNECTION =
        "android.support.customtabs.action.CustomTabsService"

    private var packageNameToBind: String? = null

    fun findPackageNameToUse(context: Context): String? {
        packageNameToBind = findPackageNameToUseInner(context)
        return packageNameToBind
    }

    private fun findPackageNameToUseInner(context: Context): String? {
        val browsers = BrowserPackageHelper.getBrowserPackages(context)
        val candidate = context.packageManager
            .queryIntentServices(Intent(ACTION_CUSTOM_TABS_CONNECTION), 0)
            .mapNotNull { it.serviceInfo?.packageName }
            .filter { browsers.contains(it) }
        if (candidate.isEmpty()) {
            return null
        }
        if (candidate.size == 1) {
            return candidate[0]
        }
        BrowserPackageHelper.getDefaultBrowserPackage(context).let {
            if (candidate.contains(it)) {
                return it
            }
        }
        return PREFERRED_PACKAGES.find { candidate.contains(it) } ?: candidate[0]
    }
}