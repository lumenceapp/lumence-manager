package app.lumence.manager.domain.manager

import android.content.Context
import java.io.File

// Every path the Manager writes to. Patching files live in internal storage because the system wipes Android cache dirs when storage runs low.
class PathManager(private val context: Context) {

    private val patchingDir = context.filesDir.resolve("patching")

    private val downloadsDir = patchingDir.resolve("downloads")

    // Discord splits, one directory per version code
    val discordDownloadsDir = downloadsDir.resolve("discord")

    // Discord splits, one directory per version code
    val moduleApk: File = downloadsDir.resolve("xposed.apk")

    // Working copies of the downloads that get patched
    val patchedDir = patchingDir.resolve("patched")

    // Where the patched and signed apks are written
    val signedDir = patchingDir.resolve("signed")

    val updateApk = context.filesDir.resolve("update.apk")

    fun discordDownloads(versionCode: String) = discordDownloadsDir.resolve(versionCode)

    // Also clears the external cache dir, where earlier versions kept downloads
    fun clearCache() {
        for (dir in listOfNotNull(patchingDir, updateApk, context.externalCacheDir)) dir.deleteRecursively()
    }

}
