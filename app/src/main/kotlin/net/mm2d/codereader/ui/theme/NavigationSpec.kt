/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.SwipeEdge

typealias TransitionSpec<T> = AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform
typealias PredictiveTransitionSpec<T> = AnimatedContentTransitionScope<Scene<T>>.(@SwipeEdge Int) -> ContentTransform

@OptIn(ExperimentalAnimationApi::class)
object NavigationSpec {
    fun <T : Any> push(): TransitionSpec<T> = { pushTransform() }
    fun <T : Any> pop(): TransitionSpec<T> = { popTransform() }
    fun <T : Any> predictivePop(): PredictiveTransitionSpec<T> = { popTransform(it.toDirection()) }

    private fun foregroundOffset(
        direction: Int = 1,
    ): (Int) -> Int = { it * direction }

    private fun backgroundOffset(
        direction: Int = 1,
    ): (Int) -> Int = { -it / 5 * direction }

    private fun @SwipeEdge Int.toDirection(): Int = if (this == NavigationEvent.EDGE_RIGHT) -1 else 1
    private fun <T> animationSpec(): FiniteAnimationSpec<T> = tween(durationMillis = 300)

    private fun pushTransform(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = animationSpec(),
                initialOffsetX = foregroundOffset(),
            ),
            initialContentExit = slideOutHorizontally(
                animationSpec = animationSpec(),
                targetOffsetX = backgroundOffset(),
            ) + veilOut(animationSpec = tween()),
        )

    private fun popTransform(
        direction: Int = 1,
    ): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = animationSpec(),
                initialOffsetX = backgroundOffset(direction),
            ) + unveilIn(animationSpec = animationSpec()),
            initialContentExit = slideOutHorizontally(
                animationSpec = animationSpec(),
                targetOffsetX = foregroundOffset(direction),
            ),
        )
}
