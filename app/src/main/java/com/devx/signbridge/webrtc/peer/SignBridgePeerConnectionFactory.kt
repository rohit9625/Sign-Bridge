package com.devx.signbridge.webrtc.peer

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SimulcastVideoEncoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class SignBridgePeerConnectionFactory(
    private val context: Context
) {

    val eglBaseContext: EglBase.Context by lazy {
        EglBase.create().eglBaseContext
    }

    /**
     * Uses Google’s public STUN server.
     * Enables UNIFIED_PLAN SDP semantics (modern standard replacing old PLAN_B).
     */
    val rtcConfig = PeerConnection.RTCConfiguration(
        listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
    ).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
    }


    /**
     * Supports Simulcast: Sending multiple video resolutions at once.
     * Tries to use hardware encoding, falls back to software.
     */
    private val videoEncoderFactory by lazy {
        val hardwareEncoder = HardwareVideoEncoderFactory(eglBaseContext, true, true)
        SimulcastVideoEncoderFactory(hardwareEncoder, SoftwareVideoEncoderFactory())
    }

    /**
     * Used for decoding incoming video streams (e.g., from a remote peer).
     */
    private val videoDecoderFactory by lazy {
        DefaultVideoDecoderFactory(eglBaseContext)
    }

    private val factory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setInjectableLogger({ message, severity, label ->
                    val logMsg = "[onLogMessage] label: $label, message: $message"
                    when (severity) {
                        Logging.Severity.LS_VERBOSE -> Log.v(WEBRTC_TAG, logMsg)
                        Logging.Severity.LS_INFO -> Log.i(WEBRTC_TAG, logMsg)
                        Logging.Severity.LS_WARNING -> Log.w(WEBRTC_TAG, logMsg)
                        Logging.Severity.LS_ERROR -> Log.e(WEBRTC_TAG, logMsg)
                        Logging.Severity.LS_NONE -> Log.d(WEBRTC_TAG, logMsg)
                        else -> {}
                    }
                }, Logging.Severity.LS_VERBOSE)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setAudioDeviceModule(
                JavaAudioDeviceModule.builder(context)
                    .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    .setUseHardwareNoiseSuppressor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                        override fun onWebRtcAudioRecordInitError(p0: String?) {
                            Log.w(AUDIO_TAG, "[onWebRtcAudioRecordInitError] $p0")
                        }

                        override fun onWebRtcAudioRecordStartError(
                            p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?, p1: String?
                        ) {
                            Log.w(AUDIO_TAG, "[onWebRtcAudioRecordStartError] $p1")
                        }

                        override fun onWebRtcAudioRecordError(p0: String?) {
                            Log.w(AUDIO_TAG, "[onWebRtcAudioRecordError] $p0")
                        }
                    })
                    .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                        override fun onWebRtcAudioTrackInitError(p0: String?) {
                            Log.w(AUDIO_TAG, "[onWebRtcAudioTrackInitError] $p0")
                        }

                        override fun onWebRtcAudioTrackStartError(
                            p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?, p1: String?
                        ) {
                            Log.w(AUDIO_TAG, "[onWebRtcAudioTrackStartError] $p1")
                        }

                        override fun onWebRtcAudioTrackError(p0: String?) {
                            Log.w(AUDIO_TAG, "[onWebRtcAudioTrackError] $p0")
                        }
                    })
                    .setAudioRecordStateCallback(object : JavaAudioDeviceModule.AudioRecordStateCallback {
                        override fun onWebRtcAudioRecordStart() {
                            Log.d(AUDIO_TAG, "[onWebRtcAudioRecordStart] no args")
                        }

                        override fun onWebRtcAudioRecordStop() {
                            Log.d(AUDIO_TAG, "[onWebRtcAudioRecordStop] no args")
                        }
                    })
                    .setAudioTrackStateCallback(object : JavaAudioDeviceModule.AudioTrackStateCallback {
                        override fun onWebRtcAudioTrackStart() {
                            Log.d(AUDIO_TAG, "[onWebRtcAudioTrackStart] no args")
                        }

                        override fun onWebRtcAudioTrackStop() {
                            Log.d(AUDIO_TAG, "[onWebRtcAudioTrackStop] no args")
                        }
                    })
                    .createAudioDeviceModule().also {
                        it.setMicrophoneMute(false)
                        it.setSpeakerMute(false)
                    }
            )
            .createPeerConnectionFactory()
    }

    fun makePeerConnection(
        coroutineScope: CoroutineScope,
        configuration: PeerConnection.RTCConfiguration,
        type: SignBridgePeerType,
        mediaConstraints: MediaConstraints,
        onStreamAdded: ((MediaStream) -> Unit)? = null,
        onNegotiationNeeded: ((SignBridgePeerConnection, SignBridgePeerType) -> Unit)? = null,
        onIceCandidateRequest: ((IceCandidate, SignBridgePeerType) -> Unit)? = null,
        onVideoTrack: ((RtpTransceiver?) -> Unit)? = null
    ): SignBridgePeerConnection {
        val peerConnection = SignBridgePeerConnection(
            coroutineScope = coroutineScope,
            type = type,
            mediaConstraints = mediaConstraints,
            onStreamAdded = onStreamAdded,
            onNegotiationNeeded = onNegotiationNeeded,
            onIceCandidate = onIceCandidateRequest,
            onVideoTrack = onVideoTrack
        )

        val connection = makePeerConnectionInternal(
            configuration = configuration,
            observer = peerConnection
        )

        return peerConnection.apply {
            initialize(connection)
        }
    }

    private fun makePeerConnectionInternal(
        configuration: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer?
    ): PeerConnection {
        return requireNotNull(
            factory.createPeerConnection(
                configuration,
                observer
            )
        )
    }

    fun makeVideoSource(isScreencast: Boolean): VideoSource =
        factory.createVideoSource(isScreencast)

    fun makeVideoTrack(
        source: VideoSource,
        trackId: String
    ): VideoTrack = factory.createVideoTrack(trackId, source)

    fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
        factory.createAudioSource(constraints)

    fun makeAudioTrack(
        source: AudioSource,
        trackId: String
    ): AudioTrack = factory.createAudioTrack(trackId, source)

    companion object {
        private const val WEBRTC_TAG= "Call:WebRTC"
        private const val AUDIO_TAG = "Call:AudioTrackCallback"
    }
}