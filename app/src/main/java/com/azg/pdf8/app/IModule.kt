package com.azg.pdf8.app

import androidx.room.Room
import com.azg.pdf8.database.AppDatabase
import com.azg.pdf8.database.FavoriteDao
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.ui.main.document.pdf.ReadPdfViewModel
import com.azg.pdf8.ui.onboarding.OnboardingViewModel
import com.azg.pdf8.viewmodel.AppDataRepo
import com.azg.pdf8.viewmodel.CreateViewModel
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.viewmodel.ScanImageViewModel
import com.dong.baselib.permission.Permission
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OnboardingViewModel() }
    viewModel { DocumentViewModel(get(), get(), get()) }
    viewModel { ScanImageViewModel(get()) }
    viewModel { CreateViewModel() }
    viewModel { ReadPdfViewModel(get()) }
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
    single<RecentDao> { get<AppDatabase>().documentDao() }
    single<FavoriteDao> { get<AppDatabase>().favoriteDao() }
}
