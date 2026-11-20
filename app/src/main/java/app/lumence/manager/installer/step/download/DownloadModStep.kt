package app.lumence.manager.installer.step.download

import androidx.compose.runtime.Stable
import app.lumence.manager.R
import app.lumence.manager.domain.manager.BundleManager
import app.lumence.manager.installer.step.download.base.DownloadStep
import org.koin.core.component.inject
import java.io.File

// Downloads the Lumence Xposed module from the bundle's release. https://github.com/lumenceapp/lumence
@Stable
class DownloadModStep(
    workingDir: File
): DownloadStep() {

    private val bundleManager: BundleManager by inject()

    override val nameRes = R.string.step_dl_mod

    override val downloadFullUrl: String
        get() = bundleManager.bundle?.moduleUrl ?: error("The patch bundle isn't loaded")
    override val destination = preferenceManager.moduleLocation
    override val workingCopy = workingDir.resolve("xposed.apk")

}
