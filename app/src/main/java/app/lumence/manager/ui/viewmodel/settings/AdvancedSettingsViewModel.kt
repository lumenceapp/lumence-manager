package app.lumence.manager.ui.viewmodel.settings

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import app.lumence.manager.R
import app.lumence.manager.domain.manager.InstallMethod
import app.lumence.manager.domain.manager.PathManager
import app.lumence.manager.domain.manager.PreferenceManager
import app.lumence.manager.domain.manager.UpdateCheckerDuration
import app.lumence.manager.installer.shizuku.ShizukuPermissions
import app.lumence.manager.updatechecker.worker.UpdateWorker
import app.lumence.manager.utils.showToast
import kotlinx.coroutines.launch

class AdvancedSettingsViewModel(
    private val context: Context,
    private val prefs: PreferenceManager,
    private val paths: PathManager,
) : ScreenModel {

    fun clearCache() {
        paths.clearCache()
        context.showToast(R.string.msg_cleared_cache)
    }

    fun updateCheckerDuration(updateCheckerDuration: UpdateCheckerDuration) {
        val wm = WorkManager.getInstance(context)
        when (updateCheckerDuration) {
            UpdateCheckerDuration.DISABLED -> wm.cancelUniqueWork("app.lumence.manager.UPDATE_CHECK")
            else -> wm.enqueueUniquePeriodicWork(
                "app.lumence.manager.UPDATE_CHECK",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                PeriodicWorkRequestBuilder<UpdateWorker>(
                    updateCheckerDuration.time,
                    updateCheckerDuration.unit
                ).build()
            )
        }
    }

    fun setInstallMethod(method: InstallMethod) {
        when (method) {
            InstallMethod.SHIZUKU -> screenModelScope.launch {
                if (ShizukuPermissions.waitShizukuPermissions()) {
                    prefs.installMethod = InstallMethod.SHIZUKU
                } else {
                    context.showToast(R.string.msg_shizuku_denied)
                }
            }

            else -> prefs.installMethod = method
        }
    }

}