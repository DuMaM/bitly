package pl.nowak.bitly

import android.app.Application
import timber.log.Timber

class BitlyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}