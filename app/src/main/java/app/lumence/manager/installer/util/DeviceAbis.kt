package app.lumence.manager.installer.util

import android.os.Build
import app.reseam.sdk.Abi
import app.reseam.sdk.abiNamed

// Device ABIs in the format the patch engine expects.
fun deviceAbis(): List<Abi> = Build.SUPPORTED_ABIS.mapNotNull { runCatching { abiNamed(it) }.getOrNull() }
