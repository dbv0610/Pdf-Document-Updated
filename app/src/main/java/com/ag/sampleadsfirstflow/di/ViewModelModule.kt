package com.ag.sampleadsfirstflow.di

import com.ag.sampleadsfirstflow.ui.onboarding.OnboardingViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::OnboardingViewModel)
}
