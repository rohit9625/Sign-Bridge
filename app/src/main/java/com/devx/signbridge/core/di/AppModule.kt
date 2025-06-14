package com.devx.signbridge.core.di

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import com.devx.signbridge.MainActivity
import com.devx.signbridge.auth.data.UserRepositoryImpl
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.ui.CompleteProfileViewModel
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.home.ui.HomeViewModel
import com.devx.signbridge.home.ui.SearchUserViewModel
import com.devx.signbridge.videocall.data.CallRepositoryImpl
import com.devx.signbridge.videocall.domain.CallRepository
import com.devx.signbridge.videocall.ui.VideoCallViewModel
import com.devx.signbridge.webrtc.data.SignalingClient
import com.devx.signbridge.webrtc.data.WebRtcClientImpl
import com.devx.signbridge.webrtc.domain.WebRtcClient
import com.devx.signbridge.webrtc.peer.SignBridgePeerConnectionFactory
import com.devx.signbridge.webrtc.peer.SignBridgePeerType
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {
    single { CredentialManager.create(androidContext()) }
    single { (activityCtx: Context) ->
        GoogleAuthClient(context = activityCtx, credentialManager = get())
    }
    single<UserRepository> { UserRepositoryImpl() }
    single<CallRepository> { CallRepositoryImpl() }
    single { SignalingClient() }
    single { SignBridgePeerConnectionFactory(context = androidContext()) }
    single<WebRtcClient> { (peerType: SignBridgePeerType) ->
        WebRtcClientImpl(
            context = androidContext(),
            signalingClient = get(),
            peerConnectionFactory = get(),
            peerType = peerType
        )
    }

    viewModel { (activityCtx: Context) ->
        SignInViewModel(googleAuthClient = get { parametersOf(activityCtx) }, userRepository = get())
    }
    viewModel { (activityCtx: Context) ->
        CompleteProfileViewModel(googleAuthClient = get { parametersOf(activityCtx) }, userRepository = get())
    }
    viewModel { (activityCtx: Context) ->
        HomeViewModel(googleAuthClient = get { parametersOf(activityCtx) }, userRepository = get(), callRepository = get())
    }
    viewModel { SearchUserViewModel(userRepository = get()) }
    viewModel { (callId: String, peerType: SignBridgePeerType) ->
        VideoCallViewModel(webRtcClient = get { parametersOf(peerType) }, currentCallId = callId, callRepository = get())
    }
}