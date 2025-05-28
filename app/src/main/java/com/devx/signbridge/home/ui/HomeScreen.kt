package com.devx.signbridge.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.devx.signbridge.R
import com.devx.signbridge.Route
import com.devx.signbridge.SignBrideApp
import com.devx.signbridge.ui.theme.SignBridgeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeScreenEvent) -> Unit,
    navController: NavController
) {
    LaunchedEffect(uiState.isUserSignedOut) {
        if(uiState.isUserSignedOut) {
            navController.navigate(Route.Auth) {
                popUpTo(Route.Home) {
                    inclusive = true
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Icon(
                        painter = painterResource(R.drawable.ic_app_name),
                        contentDescription = stringResource(R.string.app_name),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(96.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(HomeScreenEvent.SignOut) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = stringResource(R.string.logout),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    SignBridgeTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onEvent = { },
            navController = rememberNavController()
        )
    }
}