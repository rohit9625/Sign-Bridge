package com.devx.signbridge.videocall.domain.models

data class Call(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val status: CallStatus = CallStatus.CALLING,
    val offer: String? = null,
    val answer: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

enum class CallType {
    AUDIO, VIDEO
}

enum class CallStatus {
    CALLING,
    RINGING,
    ACCEPTED,
    REJECTED,
    ENDED,
    CANCELLED,
    MISSED
}