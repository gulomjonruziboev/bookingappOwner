package uz.buron.owner.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun setRememberMe(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    fun getSavedPhoneDigits(): String = prefs.getString(KEY_PHONE_DIGITS, "").orEmpty()

    fun savePhoneDigits(digits: String) {
        prefs.edit().putString(KEY_PHONE_DIGITS, digits).apply()
    }

    fun clearSavedCredentials() {
        prefs.edit()
            .remove(KEY_PHONE_DIGITS)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "buron_owner_prefs"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_PHONE_DIGITS = "phone_digits"
    }
}
