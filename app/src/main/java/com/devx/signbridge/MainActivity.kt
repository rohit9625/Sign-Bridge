package com.devx.signbridge

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.ui.CompleteProfileScreen
import com.devx.signbridge.auth.ui.CompleteProfileViewModel
import com.devx.signbridge.auth.ui.SignInScreen
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.home.ui.HomeScreen
import com.devx.signbridge.home.ui.HomeViewModel
import com.devx.signbridge.home.ui.SearchUserScreen
import com.devx.signbridge.home.ui.SearchUserViewModel
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.ui.VideoCallScreen
import com.devx.signbridge.videocall.ui.VideoCallViewModel
import com.devx.signbridge.webrtc.data.LocalWebRtcClient
import com.devx.signbridge.webrtc.domain.WebRtcClient
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    val googleAuthClient: GoogleAuthClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 0)
        val sessionManager: WebRtcClient by inject<WebRtcClient>()

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalWebRtcClient provides sessionManager) {
                SignBridgeTheme {
                    val startDestination = if(googleAuthClient.getSignedInUser() != null) Route.Home else Route.Auth
                    SignBrideApp(startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
fun SignBrideApp(startDestination: Route) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Auth> {
            val viewModel = koinViewModel<SignInViewModel>()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()

            SignInScreen(
                uiState = uiState.value,
                onEvent = viewModel::onEvent,
                onSuccess = { route ->
                    navController.navigate(route) {
                        if (route == Route.Home) {
                            popUpTo(Route.Auth) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }
        composable<Route.CompleteProfile> {
            val viewModel = koinViewModel<CompleteProfileViewModel>()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()

            CompleteProfileScreen(
                uiState = uiState.value,
                onEvent = viewModel::onEvent,
                onContinue = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Auth) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<Route.Home> {
            val viewModel = koinViewModel<HomeViewModel>()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = uiState.value,
                onEvent = viewModel::onEvent,
                navController = navController
            )
        }
        composable<Route.VideoCall> {
            val videoCallViewModel = koinViewModel<VideoCallViewModel>()
            val videoCallState by videoCallViewModel.videoCallState.collectAsStateWithLifecycle()
            val remoteVideoTrack by videoCallViewModel.remoteVideoTrackFlow.collectAsStateWithLifecycle(null)
            val localVideoTrack by videoCallViewModel.localVideoTrackFlow.collectAsStateWithLifecycle(null)

            VideoCallScreen(
                videoCallState = videoCallState,
                onEvent = videoCallViewModel::onEvent,
                onScreenReady = videoCallViewModel::onScreenReady,
                remoteVideoTrackState = remoteVideoTrack,
                localVideoTrackState = localVideoTrack
            )
        }

        composable<Route.SearchUser> {
            val viewModel = koinViewModel<SearchUserViewModel>()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()

            SearchUserScreen(
                uiState = uiState.value,
                onEvent = viewModel::onEvent
            )
        }
    }
}

sealed interface Route {
    @Serializable
    data object Auth: Route

    @Serializable
    data object CompleteProfile: Route

    @Serializable
    data object Home: Route

    @Serializable
    data object VideoCall: Route

    @Serializable
    data object SearchUser: Route
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SignBridgeTheme {
        SignBrideApp(
            startDestination = Route.Home
        )
    }
}