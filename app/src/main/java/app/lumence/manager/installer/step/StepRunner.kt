package app.lumence.manager.installer.step

import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lumence.manager.BuildConfig
import app.lumence.manager.domain.manager.PathManager
import app.lumence.manager.domain.manager.PreferenceManager
import app.lumence.manager.installer.step.download.DownloadBaseStep
import app.lumence.manager.installer.step.download.DownloadLangStep
import app.lumence.manager.installer.step.download.DownloadLibsStep
import app.lumence.manager.installer.step.download.DownloadResourcesStep
import app.lumence.manager.installer.step.download.DownloadModStep
import app.lumence.manager.installer.step.installing.InstallStep
import app.lumence.manager.installer.step.patching.PatchStep
import app.lumence.manager.installer.util.LogEntry
import app.lumence.manager.installer.util.Logger
import app.lumence.manager.utils.DiscordVersion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Runs all installation steps in order. Credit to rushii (github.com/rushiiMachine). discordVersion is the Discord version to inject Lumence into.
@Stable
class StepRunner(
    private val discordVersion: DiscordVersion
): KoinComponent {

    private val preferenceManager: PreferenceManager by inject()
    private val paths: PathManager by inject()
    private val debugInfo = """
            ${BuildConfig.MOD_NAME} Manager v${BuildConfig.VERSION_NAME}
            Built from commit ${BuildConfig.GIT_COMMIT} on ${BuildConfig.GIT_BRANCH} ${if (BuildConfig.GIT_LOCAL_CHANGES || BuildConfig.GIT_LOCAL_COMMITS) "(Changes Present)" else ""}
            
            Running Android ${Build.VERSION.RELEASE}, API level ${Build.VERSION.SDK_INT}
            Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}
            Device: ${Build.MANUFACTURER} - ${Build.MODEL} (${Build.DEVICE})
            ${if(Build.VERSION.SDK_INT > Build.VERSION_CODES.S) "SOC: ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}\n" else "\n\n"} 
            Adding ${BuildConfig.MOD_NAME} to Discord v$discordVersion
            
            
        """.trimIndent()

    // Logger associated with this runner
    val logger = Logger("StepRunner").also { logger ->
        debugInfo.split("\n").forEach {
            logger.logs += LogEntry(it, LogEntry.Level.INFO) // Add debug information to logs but don't print to logcat
        }
    }

    private val discordDir = paths.discordDownloads(discordVersion.toVersionCode())

    private val patchedDir = paths.patchedDir.also { it.deleteRecursively() }

    private val signedDir = paths.signedDir.also { it.deleteRecursively() }

    var currentStep by mutableStateOf<Step?>(null)
        private set

    // Whether the patching process has completed, not necessarily successfully
    var completed by mutableStateOf<Boolean>(false)
        private set

    // Whether a download step failed, only for network errors and not cancellations
    var downloadErrored by mutableStateOf<Boolean>(false)

    // Steps to go through for this install, order matters
    val steps: ImmutableList<Step> = buildList {
        // Downloading
        add(DownloadBaseStep(discordDir, patchedDir, discordVersion.toVersionCode()))
        add(DownloadLibsStep(discordDir, patchedDir, discordVersion.toVersionCode()))
        add(DownloadLangStep(discordDir, patchedDir, discordVersion.toVersionCode()))
        add(DownloadResourcesStep(discordDir, patchedDir, discordVersion.toVersionCode()))
        add(DownloadModStep(patchedDir))

        // Patching
        add(PatchStep(signedDir))

        // Installing
        add(InstallStep(signedDir))
    }.toImmutableList()

    // Get a step that already ran successfully, used to retrieve dependencies from a later step
    inline fun <reified T : Step> getCompletedStep(): T {
        val step = steps.asSequence()
            .filterIsInstance<T>()
            .filter { it.status == StepStatus.SUCCESSFUL }
            .firstOrNull()

        if (step == null) {
            throw IllegalArgumentException("No completed step ${T::class.simpleName} exists in container")
        }

        return step
    }

    // Run all the [steps] in order
    suspend fun runAll(): Throwable? {
        for ((index, step) in steps.withIndex()) {
            if (completed) return null // Failsafe in case runner is incorrectly marked as not completed too early

            currentStep = step
            val error = step.runCatching(this)
            if (error != null) {
                logger.e("Failed on ${step::class.simpleName}", error)

                completed = true
                return error
            }

            // Add delay for human psychology and
            // better group visibility in UI (the active group can change way too fast)
            val groupEnds = steps.getOrNull(index + 1)?.group != step.group
            if (groupEnds && !preferenceManager.isDeveloper) {
                val groupMs = steps.filter { it.group == step.group }.sumOf { it.durationMs }
                if (groupMs < 1000) delay(1000L - groupMs)
            }
        }

        completed = true
        return null
    }

}