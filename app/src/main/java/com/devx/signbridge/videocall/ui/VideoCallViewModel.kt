package com.devx.signbridge.videocall.ui

import androidx.lifecycle.ViewModel
import com.devx.signbridge.webrtc.domain.WebRtcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class VideoCallViewModel(
    private val webRtcClient: WebRtcClient
): ViewModel() {
    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState

    val remoteVideoTrackFlow = webRtcClient.remoteVideoTrackFlow
    val localVideoTrackFlow = webRtcClient.localVideoTrackFlow

    fun onScreenReady() {
        webRtcClient.onSessionScreenReady()
    }

    fun onEvent(e: VideoCallEvent) {
        when (e) {
            is VideoCallEvent.ToggleMicrophone -> {
                _videoCallState.update { it.copy(isMicrophoneEnabled = e.isEnabled) }
                webRtcClient.toggleMicrophone(e.isEnabled)
            }
            is VideoCallEvent.ToggleCamera -> {
                _videoCallState.update { it.copy(isCameraEnabled = e.isEnabled) }
                webRtcClient.toggleCamera(e.isEnabled)
            }
            VideoCallEvent.SwitchCamera -> {
                webRtcClient.flipCamera()
            }
            VideoCallEvent.EndCall -> {
                webRtcClient.disconnect()
                _videoCallState.update { VideoCallState() }
            }
        }
    }
}