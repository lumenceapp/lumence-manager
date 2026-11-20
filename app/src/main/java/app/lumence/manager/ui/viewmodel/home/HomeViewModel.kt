package app.lumence.manager.ui.viewmodel.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import app.lumence.manager.BuildConfig
import app.lumence.manager.R
import app.lumence.manager.domain.manager.BundleManager
import app.lumence.manager.domain.manager.DownloadManager
import app.lumence.manager.domain.manager.DownloadResult
import app.lumence.manager.domain.manager.InstallManager
import app.lumence.manager.domain.manager.InstallMethod
import app.lumence.manager.domain.manager.PathManager
import app.lumence.manager.domain.manager.PreferenceManager
import app.lumence.manager.domain.repository.RestRepository
import app.lumence.manager.installer.Installer
import app.lumence.manager.installer.session.SessionInstaller
import app.lumence.manager.installer.shizuku.ShizukuInstaller
import app.lumence.manager.installer.shizuku.ShizukuPermissions
import app.lumence.manager.network.dto.Release
import app.lumence.manager.network.utils.CommitsPagingSource
import app.lumence.manager.network.utils.dataOrNull
import app.lumence.manager.utils.DiscordVersion
import app.lumence.manager.utils.isMiui
import app.lumence.manager.utils.showToast
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: RestRepository,
    val context: Context,
    val prefs: PreferenceManager,
    val installManager: InstallManager,
    val bundleManager: BundleManager,
    private val downloadManager: DownloadManager,
    private val paths: PathManager
) : ScreenModel {

    var release by mutableStateOf<Release?>(null)
        private set

    private var updateDownloadUrl by mutableStateOf<String?>(null)
    var showUpdateDialog by mutableStateOf(false)
    var isUpdating by mutableStateOf(false)
    val commits = Pager(PagingConfig(pageSize = 30)) { CommitsPagingSource(repo) }.flow.cachedIn(
        screenModelScope
    )

    init {
        screenModelScope.launch {
            // A failure is kept in BundleManager.problem for the screen to show.
            if (runCatching { bundleManager.load(refresh = true) }.isSuccess && prefs.autoClearCache) autoClearCache()
        }
        checkForUpdate()
    }

    fun launchMod() {
        installManager.current?.let {
            val intent = context.packageManager.getLaunchIntentForPackage(it.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun uninstallMod() {
        installManager.uninstall()
    }

    fun launchModInfo() {
        installManager.current?.let {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("package:${it.packageName}")
                context.startActivity(this)
            }
        }
    }

    private fun autoClearCache() {
        val currentVersion =
            DiscordVersion.fromVersionCode(installManager.current?.versionCode.toString()) ?: return
        val targetVersion = bundleManager.discordVersion ?: return

        if (targetVersion > currentVersion) paths.discordDownloadsDir.deleteRecursively()
    }

    private fun checkForUpdate() {
        screenModelScope.launch {
            release = repo.getLatestRelease("lumenceapp/lumence-manager").dataOrNull
            release?.let {
                updateDownloadUrl = it.assets.firstOrNull { asset -> asset.name.endsWith(".apk") }?.browserDownloadUrl
                showUpdateDialog = it.tagName.removePrefix("v") != BuildConfig.VERSION_NAME
            }
        }
    }

    fun downloadAndInstallUpdate(onProgressUpdate: (Float?) -> Unit) {
        screenModelScope.launch {
            val update = paths.updateApk
            if (update.exists()) update.delete()
            isUpdating = true
            val downloadResult =
                downloadManager.downloadUpdate(updateDownloadUrl!!, update, onProgressUpdate)
            isUpdating = false

            if (downloadResult !is DownloadResult.Success) {
                if (downloadResult is DownloadResult.Error) {
                    Log.e("HomeViewModel", "Download failed: ${downloadResult.debugReason}")
                    context.showToast(R.string.msg_download_failed)
                }
                return@launch
            }

            val installMethod = if (prefs.installMethod == InstallMethod.SHIZUKU && !ShizukuPermissions.waitShizukuPermissions()) {
                // Temporarily use DEFAULT if SHIZUKU permissions are not granted
                    context.showToast(R.string.msg_shizuku_denied)
                    InstallMethod.DEFAULT
                } else {
                    prefs.installMethod
                }

            val installer: Installer = when (installMethod) {
                InstallMethod.DEFAULT -> SessionInstaller(context)
                InstallMethod.SHIZUKU -> ShizukuInstaller(context)
            }

            installer.installApks(silent = !isMiui, update)
        }
    }

}