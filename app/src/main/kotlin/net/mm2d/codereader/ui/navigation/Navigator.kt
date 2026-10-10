/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlin.reflect.KClass

@Composable
fun <T : NavKey> rememberNavigator(
    initialKey: T,
    navGraph: NavGraph<T>,
    onExit: () -> Unit,
): Navigator<T> {
    @Suppress("UNCHECKED_CAST")
    val backStack = rememberNavBackStack(initialKey) as NavBackStack<T>
    return rememberNavigator(
        backStack = backStack,
        navGraph = navGraph,
        onExit = onExit,
    )
}

@Composable
fun <T : NavKey> rememberNavigator(
    initialKeys: List<T>,
    navGraph: NavGraph<T>,
    onExit: () -> Unit,
): Navigator<T> {
    require(initialKeys.isNotEmpty()) { "initialKeys must not be empty" }
    @Suppress("UNCHECKED_CAST")
    val backStack =
        if (initialKeys.size == 1) {
            rememberNavBackStack(initialKeys[0])
        } else {
            rememberNavBackStack(*initialKeys.toTypedArray<NavKey>())
        } as NavBackStack<T>
    return rememberNavigator(
        backStack = backStack,
        navGraph = navGraph,
        onExit = onExit,
    )
}

/**
 * [backStack]はコピーせず保存・復元と共有するため、渡した後の変更はNavigatorに限定する。
 */
@Composable
fun <T : NavKey> rememberNavigator(
    backStack: NavBackStack<T>,
    navGraph: NavGraph<T>,
    onExit: () -> Unit,
): Navigator<T> {
    val currentOnExit by rememberUpdatedState(onExit)
    return remember(backStack, navGraph) {
        Navigator(
            backStack = backStack,
            navGraph = navGraph,
            onExit = { currentOnExit() },
        )
    }
}

@Composable
fun <T : NavKey> rememberNavigatorNavEntryDecorator(
    navigator: Navigator<T>,
): NavEntryDecorator<T> =
    remember(navigator) {
        NavEntryDecorator { entry ->
            NavEntry(
                navEntry = entry,
                content = { key ->
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    DisposableEffect(navigator, key, lifecycle) {
                        navigator.attachLifecycle(key, lifecycle)
                        val observer = LifecycleEventObserver { _, _ ->
                            navigator.updateNavigationReadiness()
                        }
                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
                            navigator.detachLifecycle(key, lifecycle)
                        }
                    }
                    entry.Content()
                },
            ).Content()
        }
    }

/**
 * @param backStack 保存・復元に使用するバックスタック。渡した後の変更はNavigatorに限定する。
 */
class Navigator<T : NavKey>(
    backStack: NavBackStack<T>,
    private val navGraph: NavGraph<T>,
    private val onExit: () -> Unit,
) {
    private val mutableBackStack = backStack
    val backStack: List<T> = mutableBackStack

    private val entryLifecycles = mutableMapOf<T, Lifecycle>()
    private var pendingNavigation: (() -> Unit)? = null
    internal var isNavigationReady by mutableStateOf(false)
        private set

    fun canNavigate(
        to: T,
    ): Boolean {
        val from = backStack.lastOrNull() ?: return false
        return from != to && navGraph.canNavigate(from, to)
    }

    fun canGoBack(
        from: T? = null,
    ): Boolean {
        val current = backStack.lastOrNull() ?: return false
        if (from != null && current != from) return false
        return backStack.size > 1
    }

    fun goBack(
        from: T? = null,
    ) {
        navigateBack(from)
    }

    internal fun onSystemBack(
        ignoreNavigationReady: Boolean,
        popCount: Int = 1,
    ) {
        navigateBack(from = null, ignoreNavigationReady = ignoreNavigationReady, popCount = popCount)
    }

    private fun navigateBack(
        from: T?,
        ignoreNavigationReady: Boolean = false,
        popCount: Int = 1,
    ) {
        if (popCount <= 0) return
        if (from != null && backStack.lastOrNull() != from) {
            Log.v("Navigator", "goBack from $from was ignored: current top is ${backStack.lastOrNull()}")
            return
        }
        if (!ignoreNavigationReady && !isNavigationReady) {
            enqueueNavigation { navigateBack(from, popCount = popCount) }
            return
        }
        // 予測型戻るが保留操作に先行した場合、戻り先で古い操作を実行しない。
        pendingNavigation = null
        if (backStack.size > 1) {
            // Scene単位の戻る操作は、途中で遷移ガードを再評価せず一括で処理する。
            repeat(popCount.coerceAtMost(backStack.size - 1)) {
                mutableBackStack.removeLastOrNull()
            }
            updateNavigationReadiness()
        } else {
            onExit()
        }
    }

    fun navigate(
        to: T,
    ) {
        val from = backStack.lastOrNull() ?: return
        if (from == to) {
            Log.v("Navigator", "from: $from, to: $to is same.")
            return // 連打無効
        }
        if (!navGraph.canNavigate(from, to)) {
            Log.e("Navigator", "from: $from, to: $to is not allowed.")
            return
        }
        if (!isNavigationReady) {
            enqueueNavigation { navigate(to) }
            return
        }
        mutableBackStack.add(to)
        updateNavigationReadiness()
    }

    private fun enqueueNavigation(
        action: () -> Unit,
    ) {
        // 遷移中の操作は先着1件だけ保留する。
        if (pendingNavigation == null) {
            pendingNavigation = action
        }
    }

    internal fun attachLifecycle(
        key: T,
        lifecycle: Lifecycle,
    ) {
        entryLifecycles[key] = lifecycle
        updateNavigationReadiness()
    }

    internal fun detachLifecycle(
        key: T,
        lifecycle: Lifecycle,
    ) {
        entryLifecycles.remove(key, lifecycle)
        updateNavigationReadiness()
    }

    internal fun updateNavigationReadiness() {
        // 現在の画面がRESUMEDになったら保留していた操作を実行する。
        val current = backStack.lastOrNull()
        isNavigationReady = entryLifecycles[current]?.currentState == Lifecycle.State.RESUMED
        if (isNavigationReady) {
            val action = pendingNavigation
            pendingNavigation = null
            action?.invoke()
        }
    }
}

interface NavGraph<T : NavKey> {
    fun canNavigate(
        from: T,
        to: T,
    ): Boolean
}

fun <T : NavKey> navGraph(
    action: NavGraphBuilder<T>.() -> Unit,
): NavGraph<T> = NavGraphImpl(NavGraphBuilder<T>().apply(action).build())

private class NavGraphImpl<T : NavKey>(
    private val graph: Map<KClass<out T>, Set<KClass<out T>>>,
) : NavGraph<T> {
    override fun canNavigate(
        from: T,
        to: T,
    ): Boolean = graph[from::class]?.contains(to::class) == true
}

@DslMarker
annotation class NavGraphDsl

@NavGraphDsl
class NavGraphBuilder<T : NavKey> {
    private val graph = mutableMapOf<KClass<out T>, MutableSet<KClass<out T>>>()
    internal fun build(): Map<KClass<out T>, Set<KClass<out T>>> = graph.mapValues { it.value.toSet() }

    @PublishedApi
    internal fun addTransition(
        from: KClass<out T>,
        to: KClass<out T>,
    ) {
        graph.getOrPut(from) { mutableSetOf() }.add(to)
    }

    inline fun <reified From : T> from(
        action: NavGraphNodeScope<T>.() -> Unit,
    ) {
        NavGraphNodeScope(this, From::class).apply(action)
    }
}

@NavGraphDsl
class NavGraphNodeScope<T : NavKey> @PublishedApi internal constructor(
    @PublishedApi internal val builder: NavGraphBuilder<T>,
    @PublishedApi internal val fromClass: KClass<out T>,
) {
    inline fun <reified To : T> to() {
        builder.addTransition(fromClass, To::class)
    }
}
