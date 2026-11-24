package com.smurfiez.vaneglider

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.smurfiez.vaneglider.airplay.discovery.ReceiverDiscovery
import com.smurfiez.vaneglider.airplay.session.CredentialStore
import com.smurfiez.vaneglider.data.DataStoreCredentialStore
import com.smurfiez.vaneglider.settings.SettingsRepository

/**
 * Manual dependency wiring. Three singletons is not worth a DI framework here.
 */
class KiteApplication : Application() {

    val credentialStore: CredentialStore by lazy { DataStoreCredentialStore(this) }
    val discovery: ReceiverDiscovery by lazy { ReceiverDiscovery(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        com.smurfiez.vaneglider.airplay.discovery.WifiNetworkResolver.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID_MIRRORING,
            getString(R.string.notification_channel_name),
            // Low: the session notification is ambient status, not something to interrupt for.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID_MIRRORING = "mirroring"
    }
}
