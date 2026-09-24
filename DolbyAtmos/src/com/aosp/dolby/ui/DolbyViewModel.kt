package com.aosp.dolby.ui

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.aosp.dolby.DolbyConstants.Companion.PREF_BASS
import com.aosp.dolby.DolbyConstants.Companion.PREF_DIALOGUE
import com.aosp.dolby.DolbyConstants.Companion.PREF_ENABLE
import com.aosp.dolby.DolbyConstants.Companion.PREF_HP_VIRTUALIZER
import com.aosp.dolby.DolbyConstants.Companion.PREF_IEQ
import com.aosp.dolby.DolbyConstants.Companion.PREF_PROFILE
import com.aosp.dolby.DolbyConstants.Companion.PREF_SPK_VIRTUALIZER
import com.aosp.dolby.DolbyConstants.Companion.PREF_STEREO
import com.aosp.dolby.DolbyConstants.Companion.PREF_VOLUME
import com.aosp.dolby.DolbyConstants.Companion.dlog
import com.aosp.dolby.DolbyController
import com.aosp.dolby.preference.DolbyPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "DolbyViewModel"

data class DolbyUiState(
    val dsOn: Boolean = false,
    val profile: Int = -1,
    val presetName: String = "",
    val ieq: Int = -1,
    val dialogue: Int = -1,
    val stereo: Int = -1,
    val speakerVirt: Boolean = false,
    val headphoneVirt: Boolean = false,
    val bass: Boolean = false,
    val volume: Boolean = false,
    val isOnSpeaker: Boolean = true,
) {
    /** Profile-specific controls are usable only when Dolby is on and the profile is known. */
    val controlsEnabled get() = dsOn && profile != -1

    /** Some effects are not available on the loudspeaker. */
    val headphoneControlsEnabled get() = controlsEnabled && !isOnSpeaker

    /** Stereo widening depends on headphone virtualization (same as the old XML dependency). */
    val stereoEnabled get() = headphoneControlsEnabled && headphoneVirt
}

class DolbyViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = DolbyController.getInstance(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)!!
    private val handler = Handler(Looper.getMainLooper())

    // Persists values exactly like the old preference screen did, so that
    // DolbyController.onBootCompleted() can restore them.
    private val store = DolbyPreferenceStore(application).also { it.profile = controller.profile }

    private val _state = MutableStateFlow(DolbyUiState())
    val state: StateFlow<DolbyUiState> = _state.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            dlog(TAG, "onAudioDevicesAdded")
            updateSpeakerState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            dlog(TAG, "onAudioDevicesRemoved")
            updateSpeakerState()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        refresh()
    }

    override fun onCleared() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    /** Re-reads everything from the effect. Call on resume. */
    fun refresh() {
        val dsOn = controller.dsOn
        val profile = controller.profile
        val onSpeaker = isOnSpeaker()
        dlog(TAG, "refresh: dsOn=$dsOn profile=$profile isOnSpeaker=$onSpeaker")

        if (store.profile != profile) store.profile = profile

        _state.update { prev ->
            val base = prev.copy(dsOn = dsOn, profile = profile, isOnSpeaker = onSpeaker)
            if (!(dsOn && profile != -1)) return@update base
            base.copy(
                presetName = controller.getPresetName(),
                ieq = controller.getIeqPreset(profile),
                dialogue = controller.getDialogueEnhancerAmount(profile),
                stereo = controller.getStereoWideningAmount(profile),
                speakerVirt = controller.getSpeakerVirtEnabled(profile),
                headphoneVirt = controller.getHeadphoneVirtEnabled(profile),
                bass = controller.getBassEnhancerEnabled(profile),
                volume = controller.getVolumeLevelerEnabled(profile),
            )
        }
    }

    fun setDsOn(on: Boolean) {
        dlog(TAG, "setDsOn($on)")
        controller.dsOn = on
        store.putBoolean(PREF_ENABLE, on)
        refresh()
    }

    fun setProfile(profile: Int) {
        dlog(TAG, "setProfile($profile)")
        controller.profile = profile
        store.profile = profile
        store.putString(PREF_PROFILE, profile.toString())
        refresh()
    }

    fun setIeq(value: Int) {
        controller.setIeqPreset(value)
        store.putString(PREF_IEQ, value.toString())
        _state.update { it.copy(ieq = value) }
    }

    fun setDialogue(value: Int) {
        controller.setDialogueEnhancerAmount(value)
        store.putString(PREF_DIALOGUE, value.toString())
        _state.update { it.copy(dialogue = value) }
    }

    fun setStereo(value: Int) {
        controller.setStereoWideningAmount(value)
        store.putString(PREF_STEREO, value.toString())
        _state.update { it.copy(stereo = value) }
    }

    fun setSpeakerVirt(on: Boolean) {
        controller.setSpeakerVirtEnabled(on)
        store.putBoolean(PREF_SPK_VIRTUALIZER, on)
        _state.update { it.copy(speakerVirt = on) }
    }

    fun setHeadphoneVirt(on: Boolean) {
        controller.setHeadphoneVirtEnabled(on)
        store.putBoolean(PREF_HP_VIRTUALIZER, on)
        _state.update { it.copy(headphoneVirt = on) }
    }

    fun setBass(on: Boolean) {
        controller.setBassEnhancerEnabled(on)
        store.putBoolean(PREF_BASS, on)
        _state.update { it.copy(bass = on) }
    }

    fun setVolume(on: Boolean) {
        controller.setVolumeLevelerEnabled(on)
        store.putBoolean(PREF_VOLUME, on)
        _state.update { it.copy(volume = on) }
    }

    fun resetProfile() {
        dlog(TAG, "resetProfile")
        controller.resetProfileSpecificSettings()
        // The profile's SharedPreferences file was deleted; grab a fresh instance.
        store.profile = controller.profile
        refresh()
    }

    private fun updateSpeakerState() {
        val onSpeaker = isOnSpeaker()
        _state.update { it.copy(isOnSpeaker = onSpeaker) }
    }

    private fun isOnSpeaker(): Boolean {
        val device = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA).firstOrNull()
        // No routed device: assume loudspeaker (headphone-only effects stay disabled).
        return device == null || device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    }

    private companion object {
        val ATTRIBUTES_MEDIA: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    }
}
