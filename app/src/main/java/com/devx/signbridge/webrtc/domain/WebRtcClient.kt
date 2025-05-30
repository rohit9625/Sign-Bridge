package com.devx.signbridge.webrtc.domain

import com.devx.signbridge.webrtc.data.SignalingClient
import com.devx.signbridge.webrtc.peer.SignBridgePeerConnectionFactory
import kotlinx.coroutines.flow.SharedFlow
import org.webrtc.VideoTrack

interface WebRtcClient {
    val signalingClient: SignalingClient

    val peerConnectionFactory: SignBridgePeerConnectionFactory

    val localVideoTrackFlow: SharedFlow<VideoTrack>

    val remoteVideoTrackFlow: SharedFlow<VideoTrack>

    fun onSessionScreenReady()

    fun flipCamera()

    fun toggleMicrophone(enabled: Boolean)

    fun toggleCamera(enabled: Boolean)

    fun disconnect()
}