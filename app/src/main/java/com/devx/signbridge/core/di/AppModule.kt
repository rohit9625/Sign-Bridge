package com.devx.signbridge.core.di

import androidx.credentials.CredentialManager
import com.devx.signbridge.auth.data.UserRepositoryImpl
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.ui.CompleteProfileViewModel
import com.devx.signbridge.auth.ui.SignInViewModel
import com.devx.signbridge.home.ui.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CredentialManager.create(androidContext()) }
    single { GoogleAuthClient(context = androidContext(), credentialManager = get()) }
    single<UserRepository> { UserRepositoryImpl() }
    viewModel { SignInViewModel(googleAuthClient = get(), userRepository = get()) }
    viewModel { CompleteProfileViewModel(googleAuthClient = get(), userRepository = get()) }
    viewModel { HomeViewModel(googleAuthClient = get()) }
}