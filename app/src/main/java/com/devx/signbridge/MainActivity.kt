package com.devx.signbridge

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.ui.CompleteProfileScreen
import com.devx.signbridge.auth.ui.CompleteProfileViewModel
import com.devx.signbridge.auth.ui.SignInScreen
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.home.ui.HomeScreen
import com.devx.signbridge.home.ui.HomeViewModel
import com.devx.signbridge.home.ui.SearchUserScreen
import com.devx.signbridge.home.ui.SearchUserViewModel
import com.devx.signbridge.ui.theme.SignBridgeTheme
import com.devx.signbridge.videocall.VideoCallActivity
import com.devx.signbridge.videocall.domain.models.Call
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    private val userRepository: UserRepository by inject<UserRepository>()
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 0)
        val googleAuthClient: GoogleAuthClient by inject {
            parametersOf(this)
        }

        enableEdgeToEdge()
        setContent {
            SignBridgeTheme {
                val startDestination = googleAuthClient.getSignedInUser()?.userId?.let { userId ->
                    currentUserId = userId
                    Route.Home
                } ?: Route.Auth
                SignBrideApp(startDestination = startDestination)
            }
        }
    }

    fun startVideoCall(call: Call, isIncomingCall: Boolean) {
        val intent = VideoCallActivity.createIntent(
            context = applicationContext,
            callId = call.id,
            callerName = call.callerName,
            callerAvatar = null,
            isIncomingCall = isIncomingCall
        )
        startActivity(intent)
    }

    @Composable
    fun SignBrideApp(startDestination: Route) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = startDestination) {
            composable<Route.Auth> {
                val viewModel = koinViewModel<SignInViewModel> {
                    parametersOf(this@MainActivity)
                }
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
                val viewModel = koinViewModel<CompleteProfileViewModel> {
                    parametersOf(this@MainActivity)
                }
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
                val viewModel = koinViewModel<HomeViewModel> {
                    parametersOf(this@MainActivity)
                }
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    uiState = uiState.value,
                    onEvent = viewModel::onEvent,
                    onStartVideoCall = ::startVideoCall,
                    navController = navController
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

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        SignBridgeTheme {
            SignBrideApp(
                startDestination = Route.Home
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Set user is Offline
        userRepository.changeOnlineStatus(userId = currentUserId, isOnline = false)
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
    data object SearchUser: Route
}
