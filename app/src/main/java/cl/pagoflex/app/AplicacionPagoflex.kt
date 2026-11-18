package cl.pagoflex.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AplicacionPagoflex : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}