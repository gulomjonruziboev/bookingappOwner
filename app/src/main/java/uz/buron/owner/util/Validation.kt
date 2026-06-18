package uz.buron.owner.util

object Validation {
    private val NAME_REGEX = Regex("""^[\u0400-\u04FFa-zA-Z\s'-]{2,50}$""")
    private val PASSWORD_REGEX = Regex("""^.{6,64}$""")
    private val TELEGRAM_REGEX = Regex("""^@[a-zA-Z0-9_]{5,32}$""")

    fun validateName(value: String, label: String): String? {
        return if (NAME_REGEX.matches(value.trim())) {
            null
        } else {
            "$label: faqat harflar, 2–50 belgi"
        }
    }

    fun validateVenueName(value: String, label: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.length < 2 -> "$label: kamida 2 belgi"
            trimmed.length > 80 -> "$label: ko'pi bilan 80 belgi"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return if (PASSWORD_REGEX.matches(password)) {
            null
        } else {
            "Parol kamida 6 belgidan iborat bo'lishi kerak"
        }
    }

    fun validateTelegram(telegram: String): String? {
        val trimmed = telegram.trim()
        return if (TELEGRAM_REGEX.matches(trimmed)) {
            null
        } else {
            "Telegram: @username formatida (masalan @ali_owner)"
        }
    }
}
