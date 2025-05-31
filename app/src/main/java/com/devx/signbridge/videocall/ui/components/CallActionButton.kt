package com.devx.signbridge.videocall.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CallActionButtons(
    onAnswerCall: () -> Unit,
    onDeclineCall: () -> Unit,
    isRinging: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decline button
        CallActionButton(
            icon = Icons.Default.Call,
            backgroundColor = Color.Red,
            size = 80.dp,
            onClick = onDeclineCall
        )

        // Answer button
        CallActionButton(
            icon = Icons.Default.Call,
            backgroundColor = Color.Green,
            size = 80.dp,
            onClick = onAnswerCall,
            animated = isRinging
        )
    }
}

@Composable
private fun CallActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    size: Dp,
    onClick: () -> Unit,
    animated: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (animated) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .scale(if (animated) scale else 1f),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}