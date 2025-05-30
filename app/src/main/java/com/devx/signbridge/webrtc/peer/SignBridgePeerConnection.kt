package com.devx.signbridge.webrtc.peer

import android.util.Log
import com.devx.signbridge.webrtc.utils.addRtcIceCandidate
import com.devx.signbridge.webrtc.utils.createValue
import com.devx.signbridge.webrtc.utils.setValue
import com.devx.signbridge.webrtc.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription

class SignBridgePeerConnection(
    private val coroutineScope: CoroutineScope,
    private val type: SignBridgePeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded: ((MediaStream) -> Unit)?,
    private val onNegotiationNeeded: ((SignBridgePeerConnection, SignBridgePeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, SignBridgePeerType) -> Unit)?,
    private val onVideoTrack: ((RtpTransceiver?) -> Unit)?
) : PeerConnection.Observer {

    private val typeTag = type.name
    /**
     * The wrapped connection for all the WebRTC communication.
     */
    lateinit var connection: PeerConnection
    private set

    private var statsJob: Job? = null

    /**
     * Used to pool together and store [IceCandidate]s before consuming them.
     */
    private val pendingIceMutex = Mutex()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    /**
     * Contains stats events for observation.
     */
    private val statsFlow: MutableStateFlow<RTCStatsReport?> = MutableStateFlow(null)

    init {
        Log.d(TAG, "<init>; MediaConstraints: $mediaConstraints")
    }

    /**
     * Initialize a [SignBridgePeerConnection] using a WebRTC [PeerConnection].
     *
     * @param peerConnection The connection that holds audio and video tracks.
     */
    fun initialize(peerConnection: PeerConnection) {
        Log.d(TAG, "Initializing Peer Connection [$typeTag]: $peerConnection")
        this.connection = peerConnection
    }

    /**
     * Used to create an offer whenever there's a negotiation that we need to process on the
     * publisher side.
     *
     * @return [Result] wrapper of the [org.webrtc.SessionDescription] for the publisher.
     */
    suspend fun createOffer(): Result<SessionDescription> {
        Log.d(TAG, "Creating Offer [$typeTag]; no args")
        return createValue { connection.createOffer(it, mediaConstraints) }
    }

    /**
     * Used to create an answer whenever there's a subscriber offer.
     *
     * @return [Result] wrapper of the [SessionDescription] for the subscriber.
     */
    suspend fun createAnswer(): Result<SessionDescription> {
        Log.d(TAG, "Creating Answer [$typeTag]; no args")
        return createValue { connection.createAnswer(it, mediaConstraints) }
    }

    /**
     * Used to set up the SDP on underlying connections and to add [pendingIceCandidates] to the
     * connection for listening.
     *
     * @param sessionDescription That contains the remote SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        Log.d(TAG, "[$typeTag] Setting Remote Description: ${sessionDescription.stringify()}")
        return setValue {
            connection.setRemoteDescription(
                it,
                SessionDescription(
                    sessionDescription.type,
                    sessionDescription.description.mungeCodecs()
                )
            )
        }.also {
            pendingIceMutex.withLock {
                pendingIceCandidates.forEach { iceCandidate ->
                    Log.d(TAG, "Adding Pending Ice Candidate [$typeTag]: $iceCandidate")
                    connection.addRtcIceCandidate(iceCandidate)
                }
                pendingIceCandidates.clear()
            }
        }
    }

    /**
     * Sets the local description for a connection either for the subscriber or publisher based on
     * the flow.
     *
     * @param sessionDescription That contains the subscriber or publisher SDP.
     * @return An empty [Result], if the operation has been successful or not.
     */
    suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
        val sdp = SessionDescription(
            sessionDescription.type,
            sessionDescription.description.mungeCodecs()
        )
        Log.d(TAG, "[$typeTag] Setting Local Description: ${sdp.stringify()}")
        return setValue { connection.setLocalDescription(it, sdp) }
    }

    /**
     * Adds an [IceCandidate] to the underlying [connection] if it's already been set up, or stores
     * it for later consumption.
     *
     * @param iceCandidate To process and add to the connection.
     * @return An empty [Result], if the operation has been successful or not.
     */
    suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
        if (connection.remoteDescription == null) {
            Log.d(TAG, "Adding Pending Ice Candidate [$typeTag]: $iceCandidate")
            pendingIceMutex.withLock {
                pendingIceCandidates.add(iceCandidate)
            }
            return Result.failure(RuntimeException("RemoteDescription is not set"))
        }
        Log.d(TAG, "Adding Ice Candidate [$typeTag]: $iceCandidate")
        return connection.addRtcIceCandidate(iceCandidate).also {
            Log.d(TAG, "Adding Ice Candidate [$typeTag]: Result: $it")
        }
    }

    /**
     * Peer connection listeners.
     */

    /**
     * Triggered whenever there's a new [RtcIceCandidate] for the call. Used to update our tracks
     * and subscriptions.
     *
     * @param candidate The new candidate.
     */
    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(TAG, "onIceCandidate [$typeTag]: $candidate")
        if (candidate == null) return

        onIceCandidate?.invoke(candidate, type)
    }

    /**
     * Triggered whenever there's a new [MediaStream] that was added to the connection.
     *
     * @param stream The stream that contains audio or video.
     */
    override fun onAddStream(stream: MediaStream?) {
        Log.d(TAG, "onAddStream [$typeTag]: $stream")
        if (stream != null) {
            onStreamAdded?.invoke(stream)
        }
    }

    /**
     * Triggered whenever there's a new [MediaStream] or [MediaStreamTrack] that's been added
     * to the call. It contains all audio and video tracks for a given session.
     *
     * @param receiver The receiver of tracks.
     * @param mediaStreams The streams that were added containing their appropriate tracks.
     */
    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        Log.d(TAG, "onAddTrack [$typeTag]: receiver: $receiver, mediaStreams: $mediaStreams")
        mediaStreams?.forEach { mediaStream ->
            Log.d(TAG, "onAddTrack [$typeTag]: mediaStream: $mediaStream")
            mediaStream.audioTracks?.forEach { remoteAudioTrack ->
                Log.d(TAG, "onAddTrack [$typeTag]: remoteAudioTrack: $remoteAudioTrack")
                remoteAudioTrack.setEnabled(true)
            }
            onStreamAdded?.invoke(mediaStream)
        }
    }

    /**
     * Triggered whenever there's a new negotiation needed for the active [PeerConnection].
     */
    override fun onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded [$typeTag]")
        onNegotiationNeeded?.invoke(this, type)
    }

    /**
     * Triggered whenever a [MediaStream] was removed.
     *
     * @param stream The stream that was removed from the connection.
     */
    override fun onRemoveStream(stream: MediaStream?) {}

    /**
     * Triggered when the connection state changes.  Used to start and stop the stats observing.
     *
     * @param newState The new state of the [PeerConnection].
     */
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "onIceConnectionChange [$typeTag]: $newState")
        when (newState) {
            PeerConnection.IceConnectionState.CLOSED,
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.DISCONNECTED -> statsJob?.cancel()

            PeerConnection.IceConnectionState.CONNECTED -> statsJob = observeStats()
            else -> Unit
        }
    }

    /**
     * @return The [RTCStatsReport] for the active connection.
     */
    fun getStats(): StateFlow<RTCStatsReport?> {
        return statsFlow
    }

    /**
     * Observes the local connection stats and emits it to [statsFlow] that users can consume.
     */
    private fun observeStats() = coroutineScope.launch {
        while (isActive) {
            delay(10_000L)
            connection.getStats {
                Log.d(TAG, "Stats: $it")
                statsFlow.value = it
            }
        }
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        Log.d(TAG, "onTrack [$typeTag]: $transceiver")
        onVideoTrack?.invoke(transceiver)
    }

    /**
     * Domain - [PeerConnection] and [PeerConnection.Observer] related callbacks.
     */
    override fun onRemoveTrack(receiver: RtpReceiver?) {
        Log.d(TAG, "onRemoveTrack: [$typeTag]; Receiver: $receiver")
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Log.d(TAG, "onSignalingChange [$typeTag]; New State: $newState")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Log.d(TAG, "onIceConnectionReceivingChange [$typeTag]; Receiving: $receiving")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "onIceGatheringChange [$typeTag]; New State: $newState")
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
        Log.d(TAG, "onIceCandidatesRemoved [$typeTag]; Candidates: $iceCandidates")
    }

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        Log.d(TAG, "onIceCandidateError [$typeTag]; Event: $event")
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        Log.d(TAG, "onConnectionChange [$typeTag]; New State: $newState")
    }

    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
        Log.d(TAG, "onSelectedCandidatePairChanged [$typeTag]; Event: $event")
    }

    override fun onDataChannel(channel: DataChannel?): Unit = Unit

    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }

    companion object {
        private const val TAG = "SignBridgePeerConnection"
    }
}