package net.mm2d.codereader

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import androidx.appcompat.widget.ListPopupWindow
import net.mm2d.codereader.util.Launcher

class OptionsMenuPresenter(
    private val activity: Activity,
    private val menuView: View,
) {
    fun setUp() {
        menuView.setOnClickListener { show() }
    }

    private fun show() {
        val popup = ListPopupWindow(activity)
        popup.setAdapter(
            ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1).also { adapter ->
                TITLES.forEach { adapter.add(activity.getString(it)) }
            },
        )
        popup.setOnItemClickListener { _, _, position, _ ->
            onItemClick(TITLES[position])
            popup.dismiss()
        }

        popup.width = activity.resources.getDimensionPixelSize(R.dimen.menu_width)
        popup.setDropDownGravity(Gravity.END)
        popup.promptPosition = ListPopupWindow.POSITION_PROMPT_BELOW
        popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        popup.anchorView = menuView
        popup.verticalOffset = -menuView.height
        popup.show()
    }

    private fun onItemClick(
        title: Int,
    ) {
        when (title) {
            R.string.options_menu_license -> LicenseActivity.start(activity)
            R.string.options_menu_source_code -> Launcher.openSourceCode(activity)
            R.string.options_menu_privacy_policy -> Launcher.openPrivacyPolicy(activity)
            R.string.options_menu_share_this_app -> Launcher.shareThisApp(activity)
            R.string.options_menu_play_store -> Launcher.openGooglePlay(activity)
            R.string.options_menu_settings -> SettingsActivity.start(activity)
            else -> Unit
        }
    }

    companion object {
        private val TITLES: List<Int> = listOf(
            R.string.options_menu_license,
            R.string.options_menu_source_code,
            R.string.options_menu_privacy_policy,
            R.string.options_menu_share_this_app,
            R.string.options_menu_play_store,
            R.string.options_menu_settings,
        )
    }
}
