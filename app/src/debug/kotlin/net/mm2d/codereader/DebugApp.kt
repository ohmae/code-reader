/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.os.Build
import android.os.StrictMode

class DebugApp : App() {
    override fun initializeOverrideWhenDebug() {
        setUpStrictMode()
    }

    private fun setUpStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectDefault().penaltyLog().build(),
        )
    }

    private fun StrictMode.VmPolicy.Builder.detectDefault(): StrictMode.VmPolicy.Builder =
        apply {
            detectActivityLeaks()
            detectLeakedClosableObjects()
            detectLeakedRegistrationObjects()
            detectFileUriExposure()
            detectCleartextNetwork()
            detectContentUriWithoutPermission()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                detectCredentialProtectedWhileLocked()
            }
        }
}
