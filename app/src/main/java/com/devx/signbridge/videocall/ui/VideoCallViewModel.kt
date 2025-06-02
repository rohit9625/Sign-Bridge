package com.devx.signbridge.videocall.ui

import androidx.lifecycle.ViewModel
import com.devx.signbridge.videocall.domain.CallRepository
import com.devx.signbridge.webrtc.domain.WebRtcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoCallViewModel(
    private val webRtcClient: WebRtcClient,
    private val currentCallId: String,
    private val callRepository: CallRepository
): ViewModel() {
    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState

    val remoteVideoTrackFlow = webRtcClient.remoteVideoTrackFlow
    val localVideoTrackFlow = webRtcClient.localVideoTrackFlow

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    init {
        webRtcClient.signalingClient.startIceCandidateListener(currentCallId)
    }

    fun onCallInitiated() {
        webRtcClient.onCallScreenReady(currentCallId)
    }

    fun onEvent(e: VideoCallEvent) {
        when (e) {
            is VideoCallEvent.ToggleMicrophone -> {
                _videoCallState.update { it.copy(isMicrophoneEnabled = !e.isEnabled) }
                webRtcClient.toggleMicrophone(e.isEnabled)
            }
            is VideoCallEvent.ToggleCamera -> {
                _videoCallState.update { it.copy(isCameraEnabled = e.isEnabled) }
                webRtcClient.toggleCamera(e.isEnabled)
            }
            VideoCallEvent.SwitchCamera -> {
                webRtcClient.flipCamera()
            }
            VideoCallEvent.EndCall -> dispose()
        }
    }

    fun answerCall() {
        _isCallActive.update { true }
    }

    fun dispose() {
        callRepository.deleteCall(currentCallId)
        webRtcClient.disconnect()
    }
}