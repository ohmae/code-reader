/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.extension

import com.google.mlkit.vision.barcode.common.Barcode

fun Barcode.typeString(): String =
    when (valueType) {
        Barcode.TYPE_CONTACT_INFO -> "contact info"
        Barcode.TYPE_EMAIL -> "email"
        Barcode.TYPE_ISBN -> "ISBN"
        Barcode.TYPE_PHONE -> "phone"
        Barcode.TYPE_PRODUCT -> "product"
        Barcode.TYPE_SMS -> "SMS"
        Barcode.TYPE_TEXT -> "text"
        Barcode.TYPE_URL -> "URL"
        Barcode.TYPE_WIFI -> "WiFi"
        Barcode.TYPE_GEO -> "geo"
        Barcode.TYPE_CALENDAR_EVENT -> "calendar event"
        Barcode.TYPE_DRIVER_LICENSE -> "driver license"
        else -> "unknown"
    }

fun Barcode.formatString(): String =
    when (format) {
        Barcode.FORMAT_CODE_128 -> "CODE-128"
        Barcode.FORMAT_CODE_39 -> "CODE-39"
        Barcode.FORMAT_CODE_93 -> "CODE-93"
        Barcode.FORMAT_CODABAR -> "NW-7 CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR code"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_PDF417 -> "PDF-417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "unknown"
    }
