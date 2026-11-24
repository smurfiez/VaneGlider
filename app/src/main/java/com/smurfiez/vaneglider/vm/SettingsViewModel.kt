package com.smurfiez.vaneglider.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smurfiez.vaneglider.KiteApplication
import com.smurfiez.vaneglider.settings.FrameRate
import com.smurfiez.vaneglider.settings.MirrorSettings
import com.smurfiez.vaneglider.settings.Quality
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KiteApplication).settingsRepository

    /**
     * Starts from the defaults rather than a nullable "not loaded yet": DataStore
     * emits immediately, and the defaults are what a first run would show anyway.
     */
    val settings: StateFlow<MirrorSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = MirrorSettings(),
    )

    fun setQuality(value: Quality) = viewModelScope.launch { repository.setQuality(value) }

    fun setFrameRate(value: FrameRate) = viewModelScope.launch { repository.setFrameRate(value) }

    fun setBitRateMbps(value: Int) = viewModelScope.launch { repository.setBitRateMbps(value) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
