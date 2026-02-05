package com.example.civn26t01.ui.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateUiFormatter {

    fun format(date: LocalDate?): String {
        if (date == null) return ""

        return date.format(
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
        )
    }
}
