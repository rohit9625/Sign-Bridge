package com.devx.signbridge.auth.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.devx.signbridge.R
import com.devx.signbridge.ui.theme.SignBridgeTheme

@Composable
fun ActionIconButton(
    text: String,
    onClick: ()-> Unit,
    icon: @Composable ()-> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = Color.Unspecified
        )
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@PreviewLightDark
@Composable
private fun ActionIconButtonPreview() {
    SignBridgeTheme {
        ActionIconButton(
            text = "Continue with Google",
            onClick = {},
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
