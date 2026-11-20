package app.lumence.manager.di

import app.lumence.manager.ui.viewmodel.home.HomeViewModel
import app.lumence.manager.ui.viewmodel.installer.InstallerViewModel
import app.lumence.manager.ui.viewmodel.installer.LogViewerViewModel
import app.lumence.manager.ui.viewmodel.libraries.LibrariesViewModel
import app.lumence.manager.ui.viewmodel.settings.AdvancedSettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val viewModelModule = module {
    factoryOf(::InstallerViewModel)
    factoryOf(::AdvancedSettingsViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::LogViewerViewModel)
    factoryOf(::LibrariesViewModel)
}