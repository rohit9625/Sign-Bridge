package com.devx.signbridge.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.devx.signbridge.R
import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.ui.theme.SignBridgeTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    uiState: SearchUserUiState,
    onEvent: (SearchUserEvent) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Scaffold { innerPadding ->
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
                        onQueryChange = { onEvent(SearchUserEvent.OnQueryChange(it)) },
                        onSearch = { onEvent(SearchUserEvent.OnSearchAction) },
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
                            onInitiateCall = { }
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


@Composable
fun UserItem(
    user: User,
    onInitiateCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = if(user.isOnline) Color.Green else Color.Red

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                SubcomposeAsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = stringResource(R.string.profile_picture),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    error = {
                        Image(
                            painter = painterResource(R.drawable.default_profile_image),
                            contentDescription = stringResource(R.string.profile_picture)
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Box(
                    modifier = Modifier.size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .align(Alignment.BottomEnd)
                )
            }
            Column {
                Text(text = user.username, style = MaterialTheme.typography.titleMedium)
                Text(text = user.email, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onInitiateCall) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call ${user.username}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun UserItemPreview() {
    SignBridgeTheme {
        UserItem(
            user = User("1", "Alice Preview", "alice.preview@example.com"),
            onInitiateCall = {}
        )
    }
}

@Preview
@Composable
private fun SearchUserScreenPreview() {
    SignBridgeTheme {
        SearchUserScreen(
            uiState = SearchUserUiState(),
            onEvent = { }
        )
    }
}