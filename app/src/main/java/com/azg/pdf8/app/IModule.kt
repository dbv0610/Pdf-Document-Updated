package com.azg.pdf8.app

import androidx.room.Room
import com.azg.pdf8.database.AppDatabase
import com.azg.pdf8.database.DocumentDao
import com.azg.pdf8.ui.onboarding.OnboardingViewModel
import com.azg.pdf8.viewmodel.AppDataRepo
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.permission.Permission
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OnboardingViewModel() }
    viewModel { DocumentViewModel(get(), get()) }
}
val dataModule = module {
    single<Permission> { Permission().initialize(get()) }
    single<AppDataRepo> { AppDataRepo() }
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "${androidApplication().packageName}_db"
        ).allowMainThreadQueries()
            .build()
    }
    single<DocumentDao> { get<AppDatabase>().documentDao() }
}
