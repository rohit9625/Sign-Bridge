package com.devx.signbridge.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devx.signbridge.R
import com.devx.signbridge.auth.ui.components.ActionIconButton
import com.devx.signbridge.ui.theme.SignBridgeTheme

@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onEvent: (SignInScreenEvent) -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(key1 = uiState.isSuccessful) {
        if (uiState.isSuccessful) {
            onNavigateToHome()
        }
    }

    Scaffold { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box {
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.labelLarge
                )
                Image(
                    painter = painterResource(  R.drawable.ic_app_name),
                    contentDescription = "App Icon",
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            ActionIconButton(
                text = stringResource(R.string.continue_with_google),
                onClick = { onEvent(SignInScreenEvent.SignInWithGoogle) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Continue with Google",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun SignInScreenPreview() {
    SignBridgeTheme {
        SignInScreen(
            uiState = SignInUiState(),
            onEvent = { },
            onNavigateToHome = { }
        )
    }
}