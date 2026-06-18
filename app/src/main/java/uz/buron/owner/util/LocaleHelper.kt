package uz.buron.owner.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private val SUPPORTED_LANGUAGES = setOf("en", "ru", "uz")

    fun resolveLanguage(deviceLanguage: String): String =
        if (deviceLanguage in SUPPORTED_LANGUAGES) deviceLanguage else "uz"

    fun wrap(context: Context): Context {
        val deviceLang = context.resources.configuration.locales[0].language
        val appLang = resolveLanguage(deviceLang)
        val locale = Locale.forLanguageTag(appLang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
