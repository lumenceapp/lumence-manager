package app.lumence.manager.installer.step.installing

import android.content.Context
import app.lumence.manager.R
import app.lumence.manager.domain.manager.InstallMethod
import app.lumence.manager.domain.manager.PreferenceManager
import app.lumence.manager.installer.Installer
import app.lumence.manager.installer.session.SessionInstaller
import app.lumence.manager.installer.shizuku.ShizukuInstaller
import app.lumence.manager.installer.shizuku.ShizukuPermissions
import app.lumence.manager.installer.step.Step
import app.lumence.manager.installer.step.StepGroup
import app.lumence.manager.installer.step.StepRunner
import app.lumence.manager.utils.isMiui
import app.lumence.manager.utils.showToast
import org.koin.core.component.inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Installs the modified splits with the user's chosen [Installer], see SessionInstaller and ShizukuInstaller. apksDir is where the patched APKs are.
class InstallStep(
    private val apksDir: File
): Step() {

    private val preferences: PreferenceManager by inject()
    private val context: Context by inject()

    override val group = StepGroup.INSTALLING
    override val nameRes = R.string.step_installing

    override suspend fun run(runner: StepRunner) {
        runner.logger.i("Installing apks")
        val files = apksDir.listFiles()
            ?.takeIf { it.isNotEmpty() }
            ?: throw Error("Missing APKs from patching step; failure likely")

        val installMethod = if (preferences.installMethod == InstallMethod.SHIZUKU && !ShizukuPermissions.waitShizukuPermissions()) {
            // Temporarily use DEFAULT if SHIZUKU permissions are not granted
            withContext(Dispatchers.Main) {
                context.applicationContext.showToast(R.string.msg_shizuku_denied)
            }
            InstallMethod.DEFAULT
        } else {
            preferences.installMethod
        }

        val installer: Installer = when (installMethod) {
            InstallMethod.DEFAULT -> SessionInstaller(context)
            InstallMethod.SHIZUKU -> ShizukuInstaller(context)
        }

        installer.installApks(silent = !isMiui, *files)
    }

}