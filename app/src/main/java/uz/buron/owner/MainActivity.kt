package uz.buron.owner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import uz.buron.owner.ui.navigation.OwnerApp
import uz.buron.owner.ui.theme.BuronOwnerTheme
import uz.buron.owner.util.LocaleHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuronOwnerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OwnerApp()
                }
            }
        }
    }
}
