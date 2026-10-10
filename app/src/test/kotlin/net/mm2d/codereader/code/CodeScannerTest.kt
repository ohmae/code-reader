package net.mm2d.codereader.code

import android.content.pm.ProviderInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.internal.MlKitInitProvider
import com.google.mlkit.common.sdkinternal.MlKitContext
import net.mm2d.codereader.App
import net.mm2d.codereader.BuildConfig
import net.mm2d.codereader.SettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class)
class CodeScannerTest {
    @Before
    fun initializeMlKit() {
        if (runCatching { MlKitContext.getInstance() }.isSuccess) return
        MlKitInitProvider().attachInfo(
            ApplicationProvider.getApplicationContext<App>(),
            ProviderInfo().apply { authority = BuildConfig.APPLICATION_ID + ".mlkitinitprovider" },
        )
    }

    @Test
    fun startsOnceAndIgnoresProviderCompletionAfterRelease() {
        var requests = 0
        var providerReads = 0
        val listeners = mutableListOf<Runnable>()

        @Suppress("UNCHECKED_CAST")
        val future = Proxy.newProxyInstance(
            ListenableFuture::class.java.classLoader,
            arrayOf(ListenableFuture::class.java),
        ) { _, method, args ->
            when (method.name) {
                "addListener" -> {
                    listeners += args!![0] as Runnable
                    null
                }

                "get" -> {
                    providerReads++
                    null
                }

                else -> null
            }
        } as ListenableFuture<ProcessCameraProvider>
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity {
                val lifecycle = it.lifecycle as LifecycleRegistry
                val initialObservers = lifecycle.observerCount
                val scanner = CodeScanner(it, callback = { _, _ -> }, providerFactory = {
                    requests++
                    future
                })
                scanner.initialize()
                scanner.initialize()
                assertEquals(initialObservers + 1, lifecycle.observerCount)
                scanner.start()
                scanner.start()
                assertEquals(1, requests)
                assertEquals(1, listeners.size)
                scanner.destroy()
                scanner.destroy()
                assertEquals(initialObservers, lifecycle.observerCount)
                listeners.single().run()
                assertEquals(0, providerReads)
                scanner.start()
                scanner.initialize()
                assertEquals(1, requests)
                assertEquals(initialObservers, lifecycle.observerCount)
            }
        }
    }
}
