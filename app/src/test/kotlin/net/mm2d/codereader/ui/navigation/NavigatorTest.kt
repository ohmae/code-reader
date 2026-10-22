package net.mm2d.codereader.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation3.runtime.NavBackStack
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class)
class NavigatorTest {
    @Test
    fun navigationFromCurrentScreenAddsDestination() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main)
        val navigator = navigator(stack)
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.RESUMED))
        navigator.navigate(from = MainNavKey.Main, to = MainNavKey.Settings)
        assertEquals(listOf(MainNavKey.Main, MainNavKey.Settings), stack.toList())
    }

    @Test
    fun navigationFromDifferentScreenIsIgnored() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main, MainNavKey.Settings)
        val navigator = navigator(stack)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.RESUMED))
        navigator.navigate(from = MainNavKey.Main, to = MainNavKey.License)
        assertEquals(listOf(MainNavKey.Main, MainNavKey.Settings), stack.toList())
    }

    @Test
    fun navigationFromDifferentScreenDoesNotBlockPendingBack() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main, MainNavKey.Settings)
        val navigator = navigator(stack)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.STARTED))
        navigator.navigate(from = MainNavKey.Main, to = MainNavKey.License)
        navigator.goBack(MainNavKey.Settings)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main), stack.toList())
    }

    @Test
    fun navigationFromCurrentScreenWaitsUntilResumed() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main)
        val navigator = navigator(stack)
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.STARTED))
        navigator.navigate(from = MainNavKey.Main, to = MainNavKey.Settings)
        assertEquals(listOf(MainNavKey.Main), stack.toList())
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main, MainNavKey.Settings), stack.toList())
    }

    @Test
    fun pendingNavigationRechecksExplicitSource() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main)
        val navigator = navigator(stack)
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.STARTED))
        navigator.navigate(from = MainNavKey.Main, to = MainNavKey.License)
        // 保留中に先頭画面が変わった状態を再現する。
        stack.add(MainNavKey.Settings)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main, MainNavKey.Settings), stack.toList())
    }

    @Test
    fun pendingNavigationWithoutExplicitSourceKeepsOriginalSource() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main)
        val navigator = Navigator(
            backStack = stack,
            navGraph = navGraph {
                from<MainNavKey.Main> { to<MainNavKey.License>() }
                from<MainNavKey.Settings> { to<MainNavKey.License>() }
            },
            onExit = {},
        )
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.STARTED))
        navigator.navigate(MainNavKey.License)
        // 変更後の画面からも遷移可能にし、遷移元の保持だけを検証する。
        stack.add(MainNavKey.Settings)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main, MainNavKey.Settings), stack.toList())
    }

    @Test
    fun duplicateAndInvalidNavigationDoNotAddEntries() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main)
        val navigator = navigator(stack)
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.RESUMED))
        navigator.navigate(MainNavKey.Settings)
        navigator.navigate(MainNavKey.Settings)
        navigator.navigate(MainNavKey.License)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main, MainNavKey.Settings), stack.toList())
    }

    @Test
    fun repeatedToolbarBackDuringTransitionReturnsOnceWithoutExiting() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main, MainNavKey.Settings)
        var exited = false
        val navigator = navigator(stack) { exited = true }
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.STARTED))
        navigator.goBack(MainNavKey.Settings)
        navigator.goBack(MainNavKey.Settings)
        assertEquals(2, stack.size)
        navigator.attachLifecycle(MainNavKey.Settings, lifecycle(Lifecycle.State.RESUMED))
        navigator.goBack(MainNavKey.Settings)
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main), stack.toList())
        assertFalse(exited)
    }

    @Test
    fun predictiveBackClearsPendingBackBeforeMainResumes() {
        val stack = NavBackStack<MainNavKey>(MainNavKey.Main, MainNavKey.License)
        var exited = false
        val navigator = navigator(stack) { exited = true }
        navigator.attachLifecycle(MainNavKey.License, lifecycle(Lifecycle.State.STARTED))
        navigator.goBack()
        navigator.onSystemBack(ignoreNavigationReady = true)
        navigator.attachLifecycle(MainNavKey.Main, lifecycle(Lifecycle.State.RESUMED))
        assertEquals(listOf(MainNavKey.Main), stack.toList())
        assertFalse(exited)
    }

    private fun navigator(
        stack: NavBackStack<MainNavKey>,
        onExit: () -> Unit = {},
    ): Navigator<MainNavKey> =
        Navigator(
            backStack = stack,
            navGraph = navGraph {
                from<MainNavKey.Main> {
                    to<MainNavKey.Settings>()
                    to<MainNavKey.License>()
                }
            },
            onExit = onExit,
        )

    private fun lifecycle(
        state: Lifecycle.State,
    ): Lifecycle {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
        owner.lifecycle.currentState = state
        return owner.lifecycle
    }
}
