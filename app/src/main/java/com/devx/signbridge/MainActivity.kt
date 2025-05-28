package com.devx.signbridge

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
import androidx.navigation.toRoute
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.auth.ui.CompleteProfileScreen
import com.devx.signbridge.auth.ui.CompleteProfileViewModel
import com.devx.signbridge.auth.ui.SignInScreen
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.home.ui.HomeScreen
import com.devx.signbridge.ui.theme.SignBridgeTheme
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import kotlin.getValue

class MainActivity : ComponentActivity() {
    val googleAuthClient: GoogleAuthClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignBridgeTheme {
                val startDestination = if(googleAuthClient.getSignedInUser() != null) Route.Home else Route.Auth
                SignBrideApp(startDestination = startDestination)
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
                onSuccess = {
                    navController.navigate(Route.CompleteProfile)
                }
            )
        }
        composable<Route.CompleteProfile> {
            val viewModel = koinViewModel<CompleteProfileViewModel>()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()

            CompleteProfileScreen(
                uiState = uiState.value,
                onContinue = {
                    navController.navigate(Route.Home)
                }
            )
        }
        composable<Route.Home> {
            HomeScreen()
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