/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import net.mm2d.codereader.ui.license.LicenseScreen
import net.mm2d.codereader.ui.settings.SettingsScreen

private val navGraph: NavGraph<MainNavKey> = navGraph {
    from<MainNavKey.Main> {
        to<MainNavKey.Settings>()
        to<MainNavKey.License>()
    }
}

@Composable
fun NavigationRoot(
    mainContent: @Composable (navigateToSettings: () -> Unit, navigateToLicense: () -> Unit) -> Unit,
) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigator = rememberNavigator(MainNavKey.Main, navGraph) {
        dispatcher?.onBackPressed()
    }
    NavigationDisplay(
        navigator = navigator,
        entryProvider = entryProvider {
            entry<MainNavKey.Main> { navKey ->
                mainContent(
                    { navigator.navigate(from = navKey, to = MainNavKey.Settings) },
                    { navigator.navigate(from = navKey, to = MainNavKey.License) },
                )
            }
            entry<MainNavKey.Settings> { navKey ->
                SettingsScreen(onBack = { navigator.goBack(navKey) })
            }
            entry<MainNavKey.License> { navKey ->
                LicenseScreen(onBack = { navigator.goBack(navKey) })
            }
        },
    )
}
