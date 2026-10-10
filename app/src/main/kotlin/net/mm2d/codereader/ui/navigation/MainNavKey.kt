/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface MainNavKey : NavKey {
    @Serializable
    data object Main : MainNavKey

    @Serializable
    data object Settings : MainNavKey

    @Serializable
    data object License : MainNavKey
}
