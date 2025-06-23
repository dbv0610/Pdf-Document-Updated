package com.azg.pdf8.app


import com.azg.pdf8.ui.onboarding.OnboardingViewModel
import com.dong.baselib.permission.Permission
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OnboardingViewModel() }
}
val dataModule = module {
    single<Permission> { Permission().initialize(get()) }
}
