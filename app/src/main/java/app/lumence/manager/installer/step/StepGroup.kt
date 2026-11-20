package app.lumence.manager.installer.step

import androidx.annotation.StringRes
import app.lumence.manager.R

// A group of [Step]s
enum class StepGroup(@StringRes val nameRes: Int) {
    // Steps that download files remotely
    DL(R.string.group_download),

    // Steps that modify the APKs
    PATCHING(R.string.group_patch),

    // Only contains the install step
    INSTALLING(R.string.group_installing)
}