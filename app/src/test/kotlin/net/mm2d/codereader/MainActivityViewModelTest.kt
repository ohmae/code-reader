package net.mm2d.codereader

import net.mm2d.codereader.result.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityViewModelTest {
    @Test
    fun keepsInsertionOrderAndIgnoresExactDuplicates() {
        val model = MainActivityViewModel()
        val first = ScanResult("first", "Text", "QR code", false)
        val second = ScanResult("second", "Text", "QR code", false)
        model.add(first)
        model.add(second)
        model.add(first.copy())
        assertEquals(listOf(first, second), model.getResultStream().value)
    }

    @Test
    fun distinguishesAllFieldsEvenWhenValuesMatch() {
        val model = MainActivityViewModel()
        val original = ScanResult("same", "Text", "QR code", false)
        val results = listOf(
            original,
            original.copy(type = "URL"),
            original.copy(format = "Code 128"),
            original.copy(isUrl = true),
        )
        results.forEach(model::add)
        assertEquals(results, model.getResultStream().value)
    }
}
