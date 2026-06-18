package uz.buron.owner.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {

    @Test
    fun validatePassword_acceptsSixChars() {
        assertNull(Validation.validatePassword("owner1"))
    }

    @Test
    fun validatePassword_rejectsShort() {
        assertNotNull(Validation.validatePassword("12345"))
    }

    @Test
    fun validateTelegram_acceptsUsername() {
        assertNull(Validation.validateTelegram("@ali_owner"))
    }

    @Test
    fun validateTelegram_rejectsInvalid() {
        assertNotNull(Validation.validateTelegram("ali_owner"))
    }

    @Test
    fun validatePhoneUz_acceptsValid() {
        assertNull(PhoneUtils.validatePhoneUz("+998901111111"))
    }

    @Test
    fun validatePhoneUz_rejectsInvalid() {
        assertNotNull(PhoneUtils.validatePhoneUz("+998123"))
    }
}
