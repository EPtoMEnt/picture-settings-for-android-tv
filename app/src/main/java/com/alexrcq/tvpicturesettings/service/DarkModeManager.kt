package com.alexrcq.tvpicturesettings.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.alexrcq.tvpicturesettings.R
import com.alexrcq.tvpicturesettings.storage.PictureSettings
import com.alexrcq.tvpicturesettings.storage.appPreferences
import com.alexrcq.tvpicturesettings.ui.FullScreenDarkFilter
import timber.log.Timber


class DarkModeManager : AccessibilityService() {

    private lateinit var pictureSettings: PictureSettings
    lateinit var darkFilter: FullScreenDarkFilter

    private val screenOnBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("${intent.action}")
            if (intent.action != ACTION_SCREEN_ON) return
            if (appPreferences.isDayModeAfterScreenOnEnabled) {
                isDarkModeEnabled = false
            }
        }
    }

    var isDarkModeEnabled: Boolean
        get() = appPreferences.isDarkModeEnabled
        set(isDarkModeEnabled) {
            val oldValue = appPreferences.isDarkModeEnabled
            if (isDarkModeEnabled == oldValue) return
            appPreferences.isDarkModeEnabled = isDarkModeEnabled
            handleBacklight()
            darkFilter.isEnabled = appPreferences.isDarkFilterEnabled && isDarkModeEnabled
            showModeChangedToast()
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("onServiceConnected")
        pictureSettings = PictureSettings(applicationContext)
        darkFilter = FullScreenDarkFilter(this).apply {
            alpha = appPreferences.darkFilterPower / 100f
        }
        registerReceiver(screenOnBroadcastReceiver, IntentFilter(ACTION_SCREEN_ON))
        sharedInstance = this
        application.sendBroadcast(
            Intent(ACTION_SERVICE_CONNECTED).apply {
                `package` = application.packageName
            }
        )
    }

    fun toggleDarkmode() {
        isDarkModeEnabled = !isDarkModeEnabled
    }

    private fun handleBacklight() {
        pictureSettings.backlight = if (isDarkModeEnabled) {
            appPreferences.nightBacklight
        } else {
            appPreferences.dayBacklight
        }
    }

    private fun showModeChangedToast() {
        val messageResId: Int = if (isDarkModeEnabled) {
            R.string.dark_mode_activated
        } else {
            R.string.day_mode_activated
        }
        Toast.makeText(applicationContext, messageResId, Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        unregisterReceiver(screenOnBroadcastReceiver)
        sharedInstance = null
        return super.onUnbind(intent)
    }

    companion object {
        const val ACTION_SERVICE_CONNECTED = "ACTION_DARK_MANAGER_CONNECTED"

        @SuppressLint("StaticFieldLeak")
        var sharedInstance: DarkModeManager? = null

        fun requireInstance(): DarkModeManager {
            return sharedInstance ?: throw java.lang.IllegalStateException(
                "${DarkModeManager::class.java.simpleName} is not initialized yet"
            )
        }
    }
}