package com.devx.signbridge.videocall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.data.HandGestureRecognizer
import com.devx.signbridge.videocall.ui.components.FloatingVideoRenderer
import com.devx.signbridge.videocall.ui.components.VideoCallControls
import com.devx.signbridge.videocall.ui.components.VideoRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoCallScreen(
    onEvent: (VideoCallEvent) -> Unit,
    videoCallState: VideoCallState,
    onScreenReady: () -> Unit = {},
    remoteVideoTrackState: VideoTrack? = null,
    localVideoTrackState: VideoTrack? = null,
    gestureRecognizer: HandGestureRecognizer? = null,
    gestureText: String = ""
) {

    LaunchedEffect(Unit) {
        onScreenReady()
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }
            val remoteVideoTrack = remoteVideoTrackState
            val localVideoTrack = localVideoTrackState

            if (remoteVideoTrack != null) {
                VideoRenderer(
                    videoTrack = remoteVideoTrack,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { parentSize = it }
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (localVideoTrack != null && videoCallState.isCameraEnabled) {
                    FloatingVideoRenderer(
                        videoTrack = localVideoTrack,
                        gestureRecognizer = gestureRecognizer,
                        isGestureRecognitionEnabled = true,
                        parentBounds = parentSize,
                        paddingValues = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(width = 150.dp, height = 210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .align(Alignment.End)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 👇 Show recognized gesture
                if (gestureText.isNotEmpty()) {
                    Text(
                        text = gestureText,
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 16.dp)
                    )
                }

                VideoCallControls(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(),
                    callState = videoCallState,
                    onEvent = { event ->
                        when (event) {
                            is VideoCallEvent.ToggleMicrophone -> {
                                val enabled = videoCallState.isMicrophoneEnabled.not()
                                onEvent(VideoCallEvent.ToggleMicrophone(isEnabled = enabled))
                            }

                            is VideoCallEvent.ToggleCamera -> {
                                val enabled = videoCallState.isCameraEnabled.not()
                                onEvent(VideoCallEvent.ToggleCamera(isEnabled = enabled))
                            }

                            VideoCallEvent.SwitchCamera -> {
                                onEvent(VideoCallEvent.SwitchCamera)
                            }

                            VideoCallEvent.EndCall -> {
                                onEvent(VideoCallEvent.EndCall)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun VideoCallScreenPreview() {
    SignBridgeTheme {
        VideoCallScreen(
            videoCallState = VideoCallState(),
            onEvent = { }
        )
    }
}
