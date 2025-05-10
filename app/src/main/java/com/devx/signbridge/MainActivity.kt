package com.devx.signbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devx.signbridge.auth.ui.SignInScreen
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.ui.theme.SignBridgeTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignBridgeTheme {
                SignBrideApp()
            }
        }
    }
}

@Composable
fun SignBrideApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Auth) {
        composable<Route.Auth> {
            val viewModel = viewModel { SignInViewModel() }
            SignInScreen(
                onEvent = viewModel::onEvent,
                onNavigateToHome = {
                    navController.navigate(Route.Home)
                }
            )
        }
        composable<Route.Home> {
            Text("Home Screen")
        }
    }
}

sealed interface Route {
    @Serializable
    data object Auth: Route

    @Serializable
    data object Home: Route
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SignBridgeTheme {
        SignBrideApp()
    }
}