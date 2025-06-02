package com.devx.signbridge.videocall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devx.signbridge.R
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.data.HandGestureRecognizer
import com.devx.signbridge.videocall.ui.components.FloatingVideoRenderer
import com.devx.signbridge.videocall.ui.components.VideoCallControls
import com.devx.signbridge.videocall.ui.components.VideoRenderer
import org.webrtc.VideoTrack
import java.util.Locale

@Composable
fun VideoCallScreen(
    onEvent: (VideoCallEvent) -> Unit,
    videoCallState: VideoCallState,
    onScreenReady: () -> Unit = {},
    remoteVideoTrackState: VideoTrack? = null,
    localVideoTrackState: VideoTrack? = null,
    gestureRecognizer: HandGestureRecognizer? = null,
    gestureText: String = "",
    onCallEnd: () -> Unit = {},
) {
    var enableGestureRecognition by rememberSaveable { mutableStateOf(false) }

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
                    gestureRecognizer = gestureRecognizer,
                    enableGestureRecognition = enableGestureRecognition,
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
                        isGestureRecognitionEnabled = false,
                        parentBounds = parentSize,
                        paddingValues = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(width = 150.dp, height = 210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 👇 Show recognized gesture
                if (gestureText.isNotEmpty() && remoteVideoTrack != null) {
                    Text(
                        text = gestureText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                    )
                }

                ElevatedFilterChip(
                    onClick = { enableGestureRecognition = !enableGestureRecognition },
                    selected = enableGestureRecognition,
                    label = {
                        Text(text = "Enable Gesture")
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_front_hand),
                            contentDescription = "Enable/Disable Gesture Recognition",
                        )
                    },
                    enabled = remoteVideoTrack != null,
                    modifier = Modifier.padding(end = 24.dp)
                        .align(alignment = Alignment.End)
                )

                VideoCallControls(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    callState = videoCallState,
                    onEvent = { event ->
                        when (event) {
                            is VideoCallEvent.ToggleMicrophone -> {
                                val enabled = videoCallState.isMicrophoneEnabled
                                onEvent(VideoCallEvent.ToggleMicrophone(isEnabled = enabled))
                            }

                            is VideoCallEvent.ToggleCamera -> {
                                val enabled = !videoCallState.isCameraEnabled
                                onEvent(VideoCallEvent.ToggleCamera(isEnabled = enabled))
                            }

                            VideoCallEvent.SwitchCamera -> {
                                onEvent(VideoCallEvent.SwitchCamera)
                            }

                            VideoCallEvent.EndCall -> {
                                onEvent(VideoCallEvent.EndCall)
                                onCallEnd()
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
