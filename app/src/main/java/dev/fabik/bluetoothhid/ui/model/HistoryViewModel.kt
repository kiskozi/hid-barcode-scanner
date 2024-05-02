package dev.fabik.bluetoothhid.ui.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.barcode.common.Barcode


data class HistoryEntry(
    val barcode: Barcode,
    val timestamp: Long,
    val isChecked: Boolean
)
class HistoryViewModel : ViewModel() {

    var isSearching by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    companion object {
        var textToClipboard = mutableStateOf("")
        var isAllChecked by mutableStateOf(false)
        var historyEntries by mutableStateOf<List<HistoryEntry>>(emptyList())
        fun addHistoryItem(barcode: Barcode) {
            val currentTime = System.currentTimeMillis()
            historyEntries = historyEntries + HistoryEntry(barcode, currentTime, false)
        }

        fun setCheckBoxOnHistoryEntry(historyEntry: HistoryEntry, checkBoxState: Boolean) {
            val index = historyEntries.indexOf(historyEntry)
            val updatedEntry = historyEntries[index].copy(isChecked = checkBoxState)
            historyEntries = historyEntries.toMutableList().apply {
                set(index, updatedEntry)
            }
            textToClipboard.value = ""
            historyEntries.forEach {
                if (it.isChecked) {
                    textToClipboard.value += (it.barcode.rawValue ?: "") + "\n"
                }
            }
            isAllChecked = (textToClipboard.value.lines().count() -1 ) == historyEntries.count()
        }
        fun setAllCheckBox(isChecked: Boolean) {
            historyEntries = historyEntries.map { it.copy(isChecked = isChecked) }
            isAllChecked = !isAllChecked
            if (isChecked) {
                textToClipboard.value = ""
                historyEntries.forEach {
                    textToClipboard.value += (it.barcode.rawValue ?: "") + "\n"
                }
            } else {
                textToClipboard.value = ""
            }
        }

        fun clearHistory() {
            historyEntries = emptyList()
            textToClipboard.value = ""
            isAllChecked = false
        }
    }

    fun parseBarcodeType(format: Int): String = when (format) {
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }

}