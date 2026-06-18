package uz.buron.owner.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val uzLocale = Locale.forLanguageTag("uz-UZ")
    private val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", uzLocale)
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.format(displayFormatter)
        } catch (_: Exception) {
            try {
                LocalDate.parse(isoDate.substringBefore("T")).format(displayFormatter)
            } catch (_: Exception) {
                isoDate
            }
        }
    }

    fun formatReviewDate(isoDate: String): String = formatDisplayDate(isoDate)

    fun toMonthString(yearMonth: YearMonth): String = yearMonth.format(monthFormatter)

    fun toIsoDateString(localDate: LocalDate): String =
        localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toString()
}

object PriceFormatter {
    private val uzLocale = Locale.forLanguageTag("uz-UZ")

    fun format(price: Long): String {
        val formatter = NumberFormat.getNumberInstance(uzLocale)
        return "${formatter.format(price)} so'm"
    }
}

fun statusLabel(status: String): String =
    Constants.STATUS_LABELS[status] ?: status

fun venueStatusLabel(status: String): String =
    Constants.VENUE_STATUS_LABELS[status] ?: status
