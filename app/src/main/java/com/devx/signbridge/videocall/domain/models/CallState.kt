package com.devx.signbridge.videocall.domain.models

sealed class CallState {
    object Idle : CallState()
    object Initiating : CallState()
    data class Calling(val callId: String) : CallState()
    data class IncomingCall(val call: Call) : CallState()
    object Connecting : CallState()
    data class Connected(val callId: String) : CallState()
    data class Error(val message: String) : CallState()
}
