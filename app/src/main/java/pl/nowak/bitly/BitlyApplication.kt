package pl.nowak.bitly

import android.app.Application
import timber.log.Timber

class BitlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(object : Timber.DebugTree() {
            /**
             * Override [log] to modify the tag and add a "global tag" prefix to it. You can rename the String "global_tag_" as you see fit.
             */
            override fun log(
                priority: Int, tag: String?, message: String, t: Throwable?
            ) {
                super.log(priority, "[BL] $tag", message, t)
            }


            /**
             * Override [createStackElementTag] to include a append a "method name" to the tag.
             */
            override fun createStackElementTag(element: StackTraceElement): String {

                return String.format(
                    "%s:%s",
                    element.methodName,
                    super.createStackElementTag(element)
                )
            }
        })
        Timber.i("created")
    }
}