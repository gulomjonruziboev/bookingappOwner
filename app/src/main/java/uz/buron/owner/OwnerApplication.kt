package uz.buron.owner

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import uz.buron.owner.util.LocaleHelper

@HiltAndroidApp
class OwnerApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}
