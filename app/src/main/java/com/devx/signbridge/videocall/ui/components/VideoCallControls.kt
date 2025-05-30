package com.devx.signbridge.videocall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devx.signbridge.R
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.ui.VideoCallEvent
import com.devx.signbridge.videocall.ui.VideoCallState

@Composable
fun VideoCallControls(
    callState: VideoCallState,
    onEvent: (VideoCallEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val microphoneIcon = painterResource(
        id = if (callState.isMicrophoneEnabled) {
            R.drawable.ic_mic_on
        } else {
            R.drawable.ic_mic_off
        }
    )
    val cameraIcon = painterResource(
        id = if (callState.isCameraEnabled) {
            R.drawable.ic_videocam_on
        } else {
            R.drawable.ic_videocam_off
        }
    )

    val defaultActions = listOf(
        VideoCallControlAction(
            icon = microphoneIcon,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            background = MaterialTheme.colorScheme.tertiaryContainer,
            callAction = VideoCallEvent.ToggleMicrophone(callState.isMicrophoneEnabled)
        ),
        VideoCallControlAction(
            icon = cameraIcon,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            background = MaterialTheme.colorScheme.primaryContainer,
            callAction = VideoCallEvent.ToggleCamera(callState.isCameraEnabled)
        ),
        VideoCallControlAction(
            icon = painterResource(id = R.drawable.ic_cameraswitch),
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            background = MaterialTheme.colorScheme.primaryContainer,
            callAction = VideoCallEvent.SwitchCamera
        ),
        VideoCallControlAction(
            icon = painterResource(id = R.drawable.ic_call_end),
            iconTint = MaterialTheme.colorScheme.onError,
            background = MaterialTheme.colorScheme.error,
            callAction = VideoCallEvent.EndCall
        )
    )

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
    ) {
        items(defaultActions) { action ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(action.background)
            ) {
                Icon(
                    painter = action.icon,
                    contentDescription = null,
                    tint = action.iconTint,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .clickable { onEvent(action.callAction) }
                )
            }
        }
    }
}

data class VideoCallControlAction(
    val icon: Painter,
    val iconTint: Color,
    val background: Color,
    val callAction: VideoCallEvent
)

@Preview
@Composable
private fun VideoCallControlsPreview() {
    SignBridgeTheme {
        VideoCallControls(
            callState = VideoCallState(),
            onEvent = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
