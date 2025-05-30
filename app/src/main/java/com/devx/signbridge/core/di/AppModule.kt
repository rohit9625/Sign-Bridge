package com.devx.signbridge.core.di

import androidx.credentials.CredentialManager
import com.devx.signbridge.auth.data.UserRepositoryImpl
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.ui.CompleteProfileViewModel
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.home.ui.HomeViewModel
import com.devx.signbridge.home.ui.SearchUserViewModel
import com.devx.signbridge.videocall.ui.VideoCallViewModel
import com.devx.signbridge.webrtc.data.SignalingClient
import com.devx.signbridge.webrtc.data.WebRtcClientImpl
import com.devx.signbridge.webrtc.domain.WebRtcClient
import com.devx.signbridge.webrtc.peer.SignBridgePeerConnectionFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CredentialManager.create(androidContext()) }
    single { GoogleAuthClient(context = androidContext(), credentialManager = get()) }
    single<UserRepository> { UserRepositoryImpl() }
    single { SignalingClient() }
    single { SignBridgePeerConnectionFactory(context = androidContext()) }
    single<WebRtcClient> { WebRtcClientImpl(context = androidContext(), signalingClient = get(), peerConnectionFactory = get()) }
    viewModel { SignInViewModel(googleAuthClient = get(), userRepository = get()) }
    viewModel { CompleteProfileViewModel(googleAuthClient = get(), userRepository = get()) }
    viewModel { HomeViewModel(googleAuthClient = get(), userRepository = get()) }
    viewModel { SearchUserViewModel(userRepository = get()) }
    viewModel { VideoCallViewModel(webRtcClient = get()) }
}