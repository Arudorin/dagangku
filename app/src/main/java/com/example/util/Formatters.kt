package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object Formatters {

    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long): String {
        val symbols = DecimalFormatSymbols(indonesianLocale).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(amount)
        return "Rp $formattedNumber"
    }

    fun formatRupiah(amount: Double): String {
        val symbols = DecimalFormatSymbols(indonesianLocale).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(amount)
        return "Rp $formattedNumber"
    }

    fun formatTanggal(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatTanggalWaktu(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatPersen(percent: Double): String {
        val symbols = DecimalFormatSymbols(indonesianLocale).apply {
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0.#", symbols)
        return "${formatter.format(percent)}%"
    }

    fun generatePoNumber(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val datePart = sdf.format(Date())
        val randomSuffix = Random.nextInt(100, 999)
        return "PO-$datePart-$randomSuffix"
    }

    fun generateSoNumber(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val datePart = sdf.format(Date())
        val randomSuffix = Random.nextInt(100, 999)
        return "SO-$datePart-$randomSuffix"
    }

    fun getStartOfDay(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun getStartOfWeek(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getStartOfMonth(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfMonth(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun formatHariTanggal(epochMillis: Long): String {
        val sdf = SimpleDateFormat("EEE, dd MMM", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatSingkat(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }
}
