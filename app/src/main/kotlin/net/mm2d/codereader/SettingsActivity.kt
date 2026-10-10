/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import net.mm2d.codereader.ui.settings.SettingsScreen
import net.mm2d.codereader.ui.theme.AppTheme

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                SettingsScreen(onBack = ::finish)
            }
        }
    }

    companion object {
        fun start(
            context: Context,
        ) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}
