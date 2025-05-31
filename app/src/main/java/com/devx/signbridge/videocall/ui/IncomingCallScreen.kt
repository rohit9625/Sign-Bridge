package com.devx.signbridge.videocall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.devx.signbridge.R
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.ui.components.CallActionButtons
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerEmail: String,
    callerAvatarUrl: String?,
    onAnswerCall: () -> Unit,
    onDeclineCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRinging by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    // Ringing animation
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            isRinging = !isRinging
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "ringing")

    val pulseScale = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Blue-800
                        Color(0xFF7C3AED), // Purple-600
                        Color(0xFF3730A3)  // Indigo-700
                    ),
                    radius = 1000f
                )
            )
    ) {
        AnimatedBlurEffects(isRinging = isRinging)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section with time and status
            TopSection(
                isVideoCall = true,
                isRinging = isRinging
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Caller avatar with animated rings
            CallerAvatar(
                avatarUrl = callerAvatarUrl,
                name = callerName,
                pulseScale = pulseScale.value,
                isRinging = isRinging
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Caller information
            CallerInformation(
                name = callerName,
                email = callerEmail,
                isRinging = isRinging
            )

            Spacer(modifier = Modifier.weight(1f))

            // Answer/Decline buttons
            CallActionButtons(
                onAnswerCall = onAnswerCall,
                onDeclineCall = onDeclineCall,
                isRinging = isRinging
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CallerInformation(
    name: String,
    email: String,
    isRinging: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = email,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        Color.White.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .then(
                        if (isRinging) Modifier.animateContentSize() else Modifier
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isRinging) "Ringing..." else "Connecting...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TopSection(
    isVideoCall: Boolean,
    isRinging: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current time
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Call status
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                modifier = Modifier
                    .animateContentSize()
                    .scale(if (isRinging) 1.05f else 1f),
                color = if (isRinging) Color.Green.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isRinging) Color.Green else Color.White.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .then(
                                if (isRinging) Modifier.animateContentSize() else Modifier
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isVideoCall) "Incoming video call" else "Incoming call",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBlurEffects(isRinging: Boolean) {
    val animatedScale1 by animateFloatAsState(
        targetValue = if (isRinging) 1.1f else 1f,
        animationSpec = tween(1000),
        label = "blur1"
    )

    val animatedScale2 by animateFloatAsState(
        targetValue = if (isRinging) 1.05f else 0.95f,
        animationSpec = tween(1000, delayMillis = 300),
        label = "blur2"
    )

    Box(
        modifier = Modifier
            .offset(x = (-50).dp, y = 80.dp)
            .size(200.dp)
            .scale(animatedScale1)
            .background(
                Color.Blue.copy(alpha = 0.2f),
                CircleShape
            )
            .blur(60.dp)
    )

    Box(
        modifier = Modifier
            .offset(x = 100.dp, y = 300.dp)
            .size(160.dp)
            .scale(animatedScale2)
            .background(
                Color.Magenta.copy(alpha = 0.15f),
                CircleShape
            )
            .blur(40.dp)
    )
}

@Composable
private fun CallerAvatar(
    avatarUrl: String?,
    name: String,
    pulseScale: Float,
    isRinging: Boolean
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer animated ring
        if (isRinging) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        AsyncImage(
            model = avatarUrl ?: R.drawable.default_profile_image,
            contentDescription = name,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview
@Composable
private fun IncomingCallScreenPreview() {
    SignBridgeTheme {
        IncomingCallScreen(
            callerName = "John Doe",
            callerEmail = "johndoe@example.com",
            callerAvatarUrl = null,
            onAnswerCall = {},
            onDeclineCall = {}
        )
    }
}
