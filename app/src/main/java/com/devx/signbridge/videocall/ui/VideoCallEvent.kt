package com.devx.signbridge.videocall.ui

sealed interface VideoCallEvent {
    data class ToggleMicrophone(val isEnabled: Boolean): VideoCallEvent
    data class ToggleCamera(val isEnabled: Boolean): VideoCallEvent
    data object SwitchCamera: VideoCallEvent
    data object EndCall: VideoCallEvent
}