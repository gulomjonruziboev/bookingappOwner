package uz.buron.owner.util

object Constants {
    val UZBEK_REGIONS = listOf(
        "Toshkent",
        "Samarqand",
        "Buxoro",
        "Farg'ona",
        "Andijon",
        "Namangan",
        "Qashqadaryo",
        "Surxondaryo",
        "Jizzax",
        "Sirdaryo",
        "Navoiy",
        "Xorazm",
        "Qoraqalpog'iston"
    )

    val SESSION_LABELS = mapOf(
        "morning" to "Nahorgi (09:00–14:00)",
        "afternoon" to "Abetgi (14:00–18:00)",
        "evening" to "Kechgi (18:00–23:00)"
    )

    val SESSION_ORDER = listOf("morning", "afternoon", "evening")

    val STATUS_LABELS = mapOf(
        "pending" to "Kutilmoqda",
        "confirmed" to "Tasdiqlangan",
        "cancelled" to "Bekor qilingan"
    )

    val VENUE_STATUS_LABELS = mapOf(
        "pending" to "Ko'rib chiqilmoqda",
        "approved" to "Tasdiqlangan",
        "rejected" to "Rad etilgan"
    )
}
