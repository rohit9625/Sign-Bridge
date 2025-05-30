package com.devx.signbridge.webrtc.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.getSystemService
import com.devx.signbridge.webrtc.domain.WebRtcClient
import com.devx.signbridge.webrtc.peer.SignBridgePeerConnection
import com.devx.signbridge.webrtc.peer.SignBridgePeerConnectionFactory
import com.devx.signbridge.webrtc.peer.SignBridgePeerType
import com.devx.signbridge.webrtc.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID

val LocalWebRtcClient: ProvidableCompositionLocal<WebRtcClient> =
    staticCompositionLocalOf { error("WebRtcSessionManager was not initialized!") }

class WebRtcClientImpl(
    private val context: Context,
    override val signalingClient: SignalingClient,
    override val peerConnectionFactory: SignBridgePeerConnectionFactory
): WebRtcClient {

    private val sessionManagerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var offer: String? = null

    // used to send local video track to the fragment
    private val _localVideoTrackFlow = MutableSharedFlow<VideoTrack>()
    override val localVideoTrackFlow: SharedFlow<VideoTrack> = _localVideoTrackFlow

    // used to send remote video track to the sender
    private val _remoteVideoTrackFlow = MutableSharedFlow<VideoTrack>()
    override val remoteVideoTrackFlow: SharedFlow<VideoTrack> = _remoteVideoTrackFlow

    private val mediaConstraints = MediaConstraints().apply {
        mandatory.addAll(
            listOf(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"),
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
            )
        )
    }

    /** Video Properties */
    private val videoCapturer: VideoCapturer by lazy { buildCameraCapturer() }
    private val cameraManager by lazy { context.getSystemService<CameraManager>() }
    private val cameraEnumerator: Camera2Enumerator by lazy { Camera2Enumerator(context) }

    private val resolution: CameraEnumerationAndroid.CaptureFormat
        get() {
            val frontCamera = cameraEnumerator.deviceNames.first { cameraName ->
                cameraEnumerator.isFrontFacing(cameraName)
            }
            val supportedFormats = cameraEnumerator.getSupportedFormats(frontCamera) ?: emptyList()
            return supportedFormats.firstOrNull {
                (it.width == 720 || it.width == 480 || it.width == 360)
            } ?: error("There is no matched resolution!")
        }

    private val surfaceTextureHelper = SurfaceTextureHelper.create(
        "SurfaceTextureHelperThread",
        peerConnectionFactory.eglBaseContext
    )

    private val videoSource by lazy {
        peerConnectionFactory.makeVideoSource(videoCapturer.isScreencast).apply {
            videoCapturer.initialize(surfaceTextureHelper, context, this.capturerObserver)
            videoCapturer.startCapture(resolution.width, resolution.height, 30)
        }
    }

    private val localVideoTrack: VideoTrack by lazy {
        peerConnectionFactory.makeVideoTrack(
            source = videoSource,
            trackId = "Video${UUID.randomUUID()}"
        )
    }

    /** Audio Properties */
    private val audioManager by lazy {
        context.getSystemService<AudioManager>()
    }
    private val audioConstrains by lazy {
        buildAudioConstraints()
    }
    private val audioSource by lazy {
        peerConnectionFactory.makeAudioSource(audioConstrains)
    }
    private val localAudioTrack: AudioTrack by lazy {
        peerConnectionFactory.makeAudioTrack(
            source = audioSource,
            trackId = "Audio${UUID.randomUUID()}"
        )
    }

    private val peerConnection: SignBridgePeerConnection by lazy {
        peerConnectionFactory.makePeerConnection(
            coroutineScope = sessionManagerScope,
            configuration = peerConnectionFactory.rtcConfig,
            type = SignBridgePeerType.CALLEE,
            mediaConstraints = mediaConstraints,
            onIceCandidateRequest = { iceCandidate, _ ->
                signalingClient.sendCommand(
                    SignalingClient.SignalingCommand.ICE,
                    "${iceCandidate.sdpMid}$ICE_SEPARATOR${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}"
                )
            },
            onVideoTrack = { rtpTransceiver ->
                val track = rtpTransceiver?.receiver?.track() ?: return@makePeerConnection
                if (track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
                    val videoTrack = track as VideoTrack
                    sessionManagerScope.launch {
                        _remoteVideoTrackFlow.emit(videoTrack)
                    }
                }
            }
        )
    }

    init {
        sessionManagerScope.launch {
            signalingClient.signalingCommandFlow
                .collect { commandToValue ->
                    when (commandToValue.first) {
                        SignalingClient.SignalingCommand.OFFER -> handleOffer(commandToValue.second)
                        SignalingClient.SignalingCommand.ANSWER -> handleAnswer(commandToValue.second)
                        SignalingClient.SignalingCommand.ICE -> handleIce(commandToValue.second)
                        else -> Unit
                    }
                }
        }
    }

    private suspend fun sendOffer() {
        val offer = peerConnection.createOffer().getOrThrow()
        val result = peerConnection.setLocalDescription(offer)
        result.onSuccess {
            signalingClient.sendCommand(
                signalingCommand = SignalingClient.SignalingCommand.OFFER,
                sdpDescription = offer.description
            )
        }
        Log.d(TAG, "[SDP] send offer: ${offer.stringify()}")
    }

    private suspend fun sendAnswer() {
        peerConnection.setRemoteDescription(
            SessionDescription(SessionDescription.Type.OFFER, offer)
        )
        val answer = peerConnection.createAnswer().getOrThrow()
        val result = peerConnection.setLocalDescription(answer)
        result.onSuccess {
            signalingClient.sendCommand(SignalingClient.SignalingCommand.ANSWER, answer.description)
        }
        Log.d(TAG, "[SDP] send answer: ${answer.stringify()}")
    }

    private fun handleOffer(sdp: String) {
        Log.d(TAG, "[SDP] handle offer: $sdp")
        offer = sdp
    }

    private suspend fun handleAnswer(sdp: String) {
        Log.d(TAG, "[SDP] handle answer: $sdp")
        peerConnection.setRemoteDescription(
            SessionDescription(SessionDescription.Type.ANSWER, sdp)
        )
    }

    private suspend fun handleIce(iceDescription: String) {
        val iceArray = iceDescription.split(ICE_SEPARATOR)
        peerConnection.addIceCandidate(
            IceCandidate(
                iceArray[0],
                iceArray[1].toInt(),
                iceArray[2]
            )
        )
        Log.d(TAG, "[SDP] handle ice: $iceDescription")
    }

    override fun onSessionScreenReady() {
        setupAudio()
        peerConnection.connection.addTrack(localVideoTrack)
        peerConnection.connection.addTrack(localAudioTrack)
        sessionManagerScope.launch {
            // sending local video track to show local video from start
            _localVideoTrackFlow.emit(localVideoTrack)

            if (offer != null) {
                sendAnswer()
            } else {
                sendOffer()
            }
        }
    }

    override fun flipCamera() {
        (videoCapturer as? Camera2Capturer)?.switchCamera(null)
    }

    override fun toggleMicrophone(enabled: Boolean) {
        audioManager?.isMicrophoneMute = !enabled
    }

    override fun toggleCamera(enabled: Boolean) {
        if (enabled) {
            videoCapturer.startCapture(resolution.width, resolution.height, 30)
        } else {
            videoCapturer.stopCapture()
        }
    }

    override fun disconnect() {
        remoteVideoTrackFlow.replayCache.forEach { videoTrack ->
            videoTrack.dispose()
        }
        localVideoTrackFlow.replayCache.forEach { videoTrack ->
            videoTrack.dispose()
        }
        localAudioTrack.dispose()
        localVideoTrack.dispose()

        videoCapturer.stopCapture()
        videoCapturer.dispose()
        signalingClient.dispose()
    }

    private fun buildCameraCapturer(): VideoCapturer {
        val manager = cameraManager ?: throw RuntimeException("CameraManager was not initialized!")

        val ids = manager.cameraIdList
        var foundCamera = false
        var cameraId = ""

        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT) {
                foundCamera = true
                cameraId = id
            }
        }

        if (!foundCamera && ids.isNotEmpty()) {
            cameraId = ids.first()
        }

        val camera2Capturer = Camera2Capturer(context, cameraId, null)
        return camera2Capturer
    }

    private fun buildAudioConstraints(): MediaConstraints {
        val mediaConstraints = MediaConstraints()
        val items = listOf(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googAutoGainControl",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googHighpassFilter",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                true.toString()
            ),
            MediaConstraints.KeyValuePair(
                "googTypingNoiseDetection",
                true.toString()
            )
        )

        return mediaConstraints.apply {
            with(optional) {
                add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
                addAll(items)
            }
        }
    }

    private fun setupAudio() {
        Log.d(TAG, "[setupAudio]")
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val device = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } ?: return

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            Log.d(TAG, "[setupAudio]; isCommunicationDeviceSet: $isCommunicationDeviceSet")
        }
    }

    companion object {
        private const val TAG = "WebRtcClientImpl"
        private const val ICE_SEPARATOR = '$'
    }
}