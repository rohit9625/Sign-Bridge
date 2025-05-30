package com.devx.signbridge.webrtc.utils

import org.webrtc.AddIceObserver
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun PeerConnection.addRtcIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
    return suspendCoroutine { cont ->
        addIceCandidate(
            iceCandidate,
            object : AddIceObserver {
                override fun onAddSuccess() {
                    cont.resume(Result.success(Unit))
                }

                override fun onAddFailure(error: String?) {
                    cont.resume(Result.failure(RuntimeException(error)))
                }
            }
        )
    }
}

suspend inline fun createValue(
    crossinline call: (SdpObserver) -> Unit
): Result<SessionDescription> = suspendCoroutine { continuation ->
    val observer = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {
            description?.let {
                continuation.resume(Result.success(it))
            } ?: continuation.resume(Result.failure(RuntimeException("SessionDescription is null!")))
        }

        override fun onCreateFailure(message: String?) =
            continuation.resume(Result.failure(RuntimeException(message)))

        /**
         * Ignore set results as we are creating value
         */
        override fun onSetSuccess() = Unit
        override fun onSetFailure(p0: String?) = Unit
    }

    call(observer)
}

suspend inline fun setValue(
    crossinline call: (SdpObserver) -> Unit
): Result<Unit> = suspendCoroutine {
    val observer = object : SdpObserver {
        /**
         * Ignore create results as we are setting values
         */
        override fun onCreateFailure(p0: String?) = Unit
        override fun onCreateSuccess(p0: SessionDescription?) = Unit

        /**
         * Handling of set values.
         */
        override fun onSetSuccess() = it.resume(Result.success(Unit))
        override fun onSetFailure(message: String?) =
            it.resume(Result.failure(RuntimeException(message)))
    }

    call(observer)
}