package com.devx.signbridge.core.di

import androidx.credentials.CredentialManager
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.ui.SignInViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CredentialManager.create(androidContext()) }
    single { GoogleAuthClient(context = androidContext(), credentialManager = get()) }
    viewModel { SignInViewModel(googleAuthClient = get()) }
}