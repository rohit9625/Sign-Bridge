package com.devx.signbridge.core.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devx.signbridge.ui.theme.SignBridgeTheme

@Composable
fun ButtonWithLoadingIndicator(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    trailingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        shape = shape,
        modifier = modifier
    ) {
        if(isLoading && trailingIcon == null) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
        trailingIcon?.let {
            Spacer(modifier = Modifier.width(4.dp))
            if(isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    imageVector = it,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ButtonWithLoadingIndicatorPreview() {
    SignBridgeTheme {
        ButtonWithLoadingIndicator(
            text = "Continue",
            onClick = { },
            isLoading = false,
            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward
        )
    }
}