package com.devx.signbridge.videocall.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.devx.signbridge.videocall.data.HandGestureRecognizer
import org.webrtc.VideoTrack

/**
 * Represents a floating item used to feature a participant video, usually the local participant.
 *
 * @param parentBounds Bounds of the parent, used to constrain the component to the parent bounds,
 * when dragging the floating UI around the screen.
 * @param modifier Modifier for styling.
 */
@Composable
fun FloatingVideoRenderer(
  videoTrack: VideoTrack,
  parentBounds: IntSize,
  paddingValues: PaddingValues,
  modifier: Modifier = Modifier,
  isGestureRecognitionEnabled: Boolean = false,
  gestureRecognizer: HandGestureRecognizer? = null
) {
  var videoSize by remember { mutableStateOf(IntSize(0, 0)) }
  var offsetX by remember { mutableStateOf(0f) }
  var offsetY by remember { mutableStateOf(0f) }
  val offset by animateOffsetAsState(targetValue = Offset(offsetX, offsetY))
  val density = LocalDensity.current

  LaunchedEffect(parentBounds.width) {
    offsetX = 0f
    offsetY = 0f
  }

  val paddingOffset = density.run { 16.dp.toPx() }

  Card(
    elevation = CardDefaults.cardElevation(8.dp),
    modifier = Modifier
      .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
      .pointerInput(parentBounds) {
        detectDragGestures { change, dragAmount ->
          change.consume()

          val newOffsetX = (offsetX + dragAmount.x)
            .coerceAtLeast(
              -calculateHorizontalOffsetBounds(
                parentBounds = parentBounds,
                paddingValues = paddingValues,
                floatingVideoSize = videoSize,
                density = density,
                offset = paddingOffset * 2
              )
            )
            .coerceAtMost(
              0f
            )

          val newOffsetY = (offsetY + dragAmount.y)
            .coerceAtLeast(0f)
            .coerceAtMost(
              calculateVerticalOffsetBounds(
                parentBounds = parentBounds,
                paddingValues = paddingValues,
                floatingVideoSize = videoSize,
                density = density,
                offset = paddingOffset * 2
              )
            )

          offsetX = newOffsetX
          offsetY = newOffsetY
        }
      }
      .then(modifier)
      .padding(16.dp)
      .onGloballyPositioned { videoSize = it.size },
    shape = RoundedCornerShape(16.dp)
  ) {
    VideoRenderer(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp)),
      videoTrack = videoTrack,
      enableGestureRecognition = isGestureRecognitionEnabled,
      gestureRecognizer = gestureRecognizer
    )
  }
}

private fun calculateHorizontalOffsetBounds(
  parentBounds: IntSize,
  paddingValues: PaddingValues,
  floatingVideoSize: IntSize,
  density: Density,
  offset: Float
): Float {
  val rightPadding =
    density.run { paddingValues.calculateRightPadding(LayoutDirection.Ltr).toPx() }

  return parentBounds.width - rightPadding - floatingVideoSize.width - offset
}

private fun calculateVerticalOffsetBounds(
  parentBounds: IntSize,
  paddingValues: PaddingValues,
  floatingVideoSize: IntSize,
  density: Density,
  offset: Float
): Float {
  val bottomPadding =
    density.run { paddingValues.calculateBottomPadding().toPx() }

  return parentBounds.height - bottomPadding - floatingVideoSize.height - offset
}
