package com.devx.signbridge.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.devx.signbridge.R
import com.devx.signbridge.core.ui.components.ButtonWithLoadingIndicator
import com.devx.signbridge.ui.theme.SignBridgeTheme

@Composable
fun CompleteProfileScreen(
    uiState: CompleteProfileUiState,
    onEvent: (CompleteProfileScreenEvent) -> Unit,
    onContinue: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF62CDFF),
        Color(0xFF8D72E1),
        Color(0xFFA06CD5),
        Color(0xFF5E72E4)
    )

    LaunchedEffect(uiState.canContinue) {
        if(uiState.canContinue) onContinue()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(
                    start = 24.dp,
                    top = innerPadding.calculateTopPadding(),
                    end = 24.dp,
                    bottom = innerPadding.calculateBottomPadding()
                )
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SubcomposeAsyncImage(
                model = uiState.user?.profilePictureUrl,
                contentDescription = stringResource(R.string.profile_picture),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                },
                error = {
                    Image(
                        painter = painterResource(R.drawable.default_profile_image),
                        contentDescription = stringResource(R.string.profile_picture)
                    )
                },
                modifier = Modifier
                    .padding(top = 96.dp)
                    .size(128.dp)
                    .border(
                        width = 4.dp,
                        brush = Brush.linearGradient(gradientColors),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.user?.username ?: "",
                    onValueChange = {},
                    label = {
                        Text(text = "Username")
                    },
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = uiState.user?.email ?: "",
                    onValueChange = {},
                    label = {
                        Text(text = "Email Address")
                    },
                    readOnly = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            ButtonWithLoadingIndicator(
                text = "Continue",
                onClick = { onEvent(CompleteProfileScreenEvent.OnContinueClick) },
                isLoading = uiState.isLoading,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .align(Alignment.End)
            )
        }
    }
}

@Preview
@Composable
private fun CompleteProfileScreenPreview() {
    SignBridgeTheme {
        CompleteProfileScreen(
            uiState = CompleteProfileUiState(),
            onEvent = { },
            onContinue = { }
        )
    }
}