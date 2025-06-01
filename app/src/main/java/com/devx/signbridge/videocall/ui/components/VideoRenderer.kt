package com.devx.signbridge.videocall.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.devx.signbridge.videocall.data.HandGestureRecognizer
import com.devx.signbridge.webrtc.data.LocalWebRtcClient
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier,
    enableGestureRecognition: Boolean = false,
    gestureRecognizer: HandGestureRecognizer? = null,
    maxClassifications: Int = 1
) {
    val trackState: MutableState<VideoTrack?> = remember { mutableStateOf(null) }
    var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

    // Initialize HandGestureRecognizer
    LaunchedEffect(enableGestureRecognition, maxClassifications) {
        if (enableGestureRecognition && gestureRecognizer != null) {
            view?.setGestureRecognizer(
                gestureRecognizer,
                processingInterval = 5
            )
        }
    }

    DisposableEffect(videoTrack) {
        onDispose {
            cleanTrack(view, trackState)
        }
    }

    val webRtcClient = LocalWebRtcClient.current
    AndroidView(
        factory = { context ->
            VideoTextureViewRenderer(context).apply {
                init(
                    webRtcClient.peerConnectionFactory.eglBaseContext,
                    object : RendererCommon.RendererEvents {
                        override fun onFirstFrameRendered() = Unit

                        override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) = Unit
                    }
                )
                setupVideo(trackState, videoTrack, this)
                view = this
            }
        },
        update = { v -> setupVideo(trackState, videoTrack, v) },
        modifier = modifier
    )
}

private fun cleanTrack(
    view: VideoTextureViewRenderer?,
    trackState: MutableState<VideoTrack?>
) {
    view?.let { trackState.value?.removeSink(it) }
    trackState.value = null
}

private fun setupVideo(
    trackState: MutableState<VideoTrack?>,
    track: VideoTrack,
    renderer: VideoTextureViewRenderer
) {
    if (trackState.value == track) {
        return
    }

    cleanTrack(renderer, trackState)

    trackState.value = track
    track.addSink(renderer)
}
