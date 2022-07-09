/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.mm2d.codereader.result.ScanResult

class MainActivityViewModel : ViewModel() {
    val resultLiveData: MutableLiveData<List<ScanResult>> = MutableLiveData(emptyList())

    fun add(result: ScanResult): Boolean {
        val results = resultLiveData.value ?: emptyList()
        if (results.contains(result)) return false
        resultLiveData.value = results + result
        return true
    }
}
