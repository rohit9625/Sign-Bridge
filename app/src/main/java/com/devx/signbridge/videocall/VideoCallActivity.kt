package com.devx.signbridge.videocall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.ui.IncomingCallScreen
import com.devx.signbridge.videocall.ui.VideoCallScreen
import com.devx.signbridge.videocall.ui.VideoCallViewModel
import com.devx.signbridge.webrtc.data.LocalWebRtcClient
import com.devx.signbridge.webrtc.domain.WebRtcClient
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.getValue

class VideoCallActivity: ComponentActivity() {
    private val callId: String by lazy {
        intent.getStringExtra(EXTRA_CALL_ID) ?: ""
    }

    private val callerName: String by lazy {
        intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
    }

    private val callerAvatar: String? by lazy {
        intent.getStringExtra(EXTRA_CALLER_AVATAR)
    }

    private val isIncomingCall: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_INCOMING_CALL, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Validate required parameters
        if (callId.isEmpty()) {
            finish()
            return
        }
        val webRtcClient: WebRtcClient by inject<WebRtcClient>()

        // Set up activity for video calls
//        setupVideoCallActivity()

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalWebRtcClient provides webRtcClient) {
                SignBridgeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val videoCallViewModel = koinViewModel<VideoCallViewModel>(
                            parameters = { parametersOf(callId) }
                        )
                        val videoCallState by videoCallViewModel.videoCallState.collectAsStateWithLifecycle()
                        val remoteVideoTrack by videoCallViewModel.remoteVideoTrackFlow.collectAsStateWithLifecycle(null)
                        val localVideoTrack by videoCallViewModel.localVideoTrackFlow.collectAsStateWithLifecycle(null)

                        if(isIncomingCall) {
                            IncomingCallScreen(
                                callerName = callerName,
                                callerEmail = "rv17837@gmail.com",
                                callerAvatarUrl = callerAvatar,
                                onAnswerCall = {
                                    // TODO("Handle incoming call")
                                },
                                onDeclineCall = {
                                    videoCallViewModel.dispose()
                                    handleCallEnded()
                                }
                            )
                        } else {
                            VideoCallScreen(
                                videoCallState = videoCallState,
                                onEvent = videoCallViewModel::onEvent,
                                onScreenReady = videoCallViewModel::onCallInitiated,
                                remoteVideoTrackState = remoteVideoTrack,
                                localVideoTrackState = localVideoTrack
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupVideoCallActivity() {
        // Keep screen on during video call
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // For incoming calls, show over lock screen
        if (isIncomingCall) {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }

    private fun handleCallEnded() {
        // Clean up and finish activity
//        videoCallViewModel.endCall()
        finish()
    }

    private fun handleCallAnswered() {
        // Handle call answered logic
//        videoCallViewModel.answerCall()
    }

    private fun handleCallDeclined() {
        // Handle call declined logic
//        videoCallViewModel.declineCall()
        finish()
    }

    override fun onBackPressed() {
        // Prevent back button during active call
        // You might want to show a confirmation dialog
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up video call resources
//        videoCallViewModel.cleanup()
    }

    companion object {
        private const val EXTRA_CALL_ID = "extra_call_id"
        private const val EXTRA_CALLER_NAME = "extra_caller_name"
        private const val EXTRA_CALLER_AVATAR = "extra_caller_avatar"
        private const val EXTRA_IS_INCOMING_CALL = "extra_is_incoming_call"

        fun createIntent(
            context: Context,
            callId: String,
            callerName: String,
            callerAvatar: String? = null,
            isIncomingCall: Boolean = false
        ): Intent {
            return Intent(context, VideoCallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_AVATAR, callerAvatar)
                putExtra(EXTRA_IS_INCOMING_CALL, isIncomingCall)

                // For incoming calls, show over lock screen
                if (isIncomingCall) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }
        }
    }
}