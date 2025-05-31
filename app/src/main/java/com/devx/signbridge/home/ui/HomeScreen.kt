package com.devx.signbridge.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

    LaunchedEffect(uiState.incomingCall) {
        uiState.incomingCall?.let {
            navController.navigate(Route.VideoCall(callId = it.id, isIncomingCall = true))
        }
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

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
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = uiState.searchQuery,
                        onQueryChange = { onEvent(HomeScreenEvent.OnQueryChange(it)) },
                        onSearch = { onEvent(HomeScreenEvent.OnSearchAction) },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = { Text(stringResource(R.string.search_input_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                if(uiState.searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No users found matching the query")
                    }
                }
                LazyColumn {
                    items(uiState.searchResults, key = { it.userId }) {
                        UserItem(
                            user = it,
                            onInitiateCall = {
                                onEvent(HomeScreenEvent.OnCallAction(it) { callId ->
                                    navController.navigate(Route.VideoCall(callId))
                                })
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enter an email address to search for a user"
                )
            }
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