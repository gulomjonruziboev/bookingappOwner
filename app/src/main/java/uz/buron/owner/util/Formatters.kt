package uz.buron.owner.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val uzLocale = Locale.forLanguageTag("uz-UZ")
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", uzLocale)

    fun formatPrice(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(uzLocale)
        return "${formatter.format(amount)} so'm"
    }

    fun formatPrice(amount: Long): String = formatPrice(amount.toDouble())

    fun formatDate(iso: String): String {
        return try {
            val instant = Instant.parse(iso)
            instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        } catch (_: Exception) {
            try {
                LocalDate.parse(iso.take(10)).format(dateFormatter)
            } catch (_: Exception) {
                iso
            }
        }
    }
}
