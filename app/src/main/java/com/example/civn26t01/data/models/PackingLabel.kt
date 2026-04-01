package com.example.civn26t01.data.models

import com.example.civn26t01.core.constants.Common
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

data class PackingLabel(
    val itemCode: String = "",
    val revision: String = "",
    val quantity: Double = 0.0, // Changed from Int to Double to support decimal quantities
    val date: LocalDate? =  LocalDate.now(),
    val workOrderNo: String = "",
    val customerRevision: String = "",
    val time: LocalTime? = null,
    val number: String? = null
): Serializable {
    companion object {
        fun fromQrCodeData(qrData: String): PackingLabel? {
            val parts = qrData.split(",")
            return try {
                if (parts.size < 6) return null

                val itemCode = parts[0]
                val revision = parts[1]
                val quantity = parts[2].toDouble() // Changed from toInt() to toDouble()
                val date = Common.parseLocalDatePacking(parts[3]) ?: return null
                val woNo = parts[4]
                val cusRev = parts[5]
                return if (parts.size >= 8) {
                    val time = Common.tryParseTime(parts[6]) ?: return null
                    val number = parts[7]
                    PackingLabel(itemCode, revision, quantity, date, woNo, cusRev, time, number)
                } else {
                    PackingLabel(itemCode, revision, quantity, date, woNo, cusRev, null, null)
                }

            } catch (e: Exception) {
                null
            }
        }
    }
}