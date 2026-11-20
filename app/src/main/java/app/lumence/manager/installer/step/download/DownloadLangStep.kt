package app.lumence.manager.installer.step.download

import androidx.compose.runtime.Stable
import app.lumence.manager.R
import app.lumence.manager.installer.step.download.base.DownloadStep
import java.io.File

// Downloads the language split, always English because Discord keeps its strings elsewhere
@Stable
class DownloadLangStep(
    dir: File,
    workingDir: File,
    version: String
): DownloadStep() {

    override val nameRes = R.string.step_dl_lang

    override val downloadMirrorUrlPath: String = "/tracker/download/$version/config.en"
    override val destination = dir.resolve("config.en-$version.apk")
    override val workingCopy = workingDir.resolve("config.en-$version.apk")

}