/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.os.Build
import android.os.StrictMode
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.leakcanary2.FlipperLeakEventListener
import com.facebook.flipper.plugins.leakcanary2.LeakCanary2FlipperPlugin
import com.facebook.flipper.plugins.navigation.NavigationFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import leakcanary.LeakCanary
import timber.log.Timber
import timber.log.Timber.DebugTree

class DebugApp : App() {
    override fun initializeOverrideWhenDebug() {
        setUpTimber()
        setUpStrictMode()
        setUpFlipper()
    }

    private fun setUpTimber() {
        Timber.plant(DebugTree())
    }

    private fun setUpStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectDefault().penaltyLog().build(),
        )
    }

    private fun StrictMode.VmPolicy.Builder.detectDefault(): StrictMode.VmPolicy.Builder = apply {
        detectActivityLeaks()
        detectLeakedClosableObjects()
        detectLeakedRegistrationObjects()
        detectFileUriExposure()
        detectCleartextNetwork()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detectContentUriWithoutPermission()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            detectCredentialProtectedWhileLocked()
        }
    }

    private fun setUpFlipper() {
        LeakCanary.config = LeakCanary.config.run {
            copy(eventListeners = eventListeners + FlipperLeakEventListener())
        }
        SoLoader.init(this, false)
        if (!FlipperUtils.shouldEnableFlipper(this)) return
        val client = AndroidFlipperClient.getInstance(this)
        client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
        client.addPlugin(NavigationFlipperPlugin.getInstance())
        client.addPlugin(NetworkFlipperPlugin())
        client.addPlugin(DatabasesFlipperPlugin(this))
        client.addPlugin(SharedPreferencesFlipperPlugin(this))
        client.addPlugin(LeakCanary2FlipperPlugin())
        client.addPlugin(CrashReporterPlugin.getInstance())
        client.start()
    }
}
