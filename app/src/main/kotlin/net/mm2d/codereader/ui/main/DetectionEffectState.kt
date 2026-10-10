/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DetectionEffectState(
    private val pause: () -> Unit,
    private val resume: () -> Unit,
) {
    var frame: DetectedFrame? by mutableStateOf(null)
        private set

    fun show(
        frame: DetectedFrame,
    ) {
        pause()
        this.frame = frame
    }

    fun finish(
        frame: DetectedFrame,
    ) {
        if (this.frame !== frame) return
        clear()
    }

    fun clear() {
        if (frame == null) return
        // 表示中の Bitmap は recycle せず、Compose の描画参照とともに GC に任せる。
        frame = null
        resume()
    }
}
