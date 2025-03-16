/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import net.mm2d.codereader.setting.Settings
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object ReviewRequester {
    private const val ACTION_COUNT = 20
    private val INTERVAL_REVIEW = TimeUnit.DAYS.toMillis(21)
    private val INTERVAL_RANDOM_RANGE = TimeUnit.DAYS.toMillis(14)

    fun onAction() {
        val settings = Settings.get()
        if (settings.firstUseTime == 0L) {
            settings.firstUseTime = System.currentTimeMillis()
            settings.reviewIntervalRandomFactor = Random.nextLong(INTERVAL_RANDOM_RANGE)
        }
        settings.detectValueActionCount++
    }

    fun requestIfNecessary(
        activity: Activity,
    ): Boolean {
        val settings = Settings.get()
        if (settings.reviewed) return false
        if (settings.detectValueActionCount < ACTION_COUNT) return false
        val interval = INTERVAL_REVIEW + settings.reviewIntervalRandomFactor
        if (System.currentTimeMillis() - settings.firstUseTime < interval) return false

        settings.reviewed = true
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow()
            .addOnSuccessListener {
                manager.launchReviewFlow(activity, it)
            }

        return true
    }
}
