package app.lumence.manager.installer.util

import android.content.Context
import app.reseam.sdk.SigningIdentity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Key patched apps are signed with. Android only installs updates signed with the same key, so it is created on first use and kept.
object Signer : KoinComponent {
    private val context by inject<Context>()

    val identity: SigningIdentity by lazy {
        SigningIdentity.Files(
            key = context.filesDir.resolve("signing.pk8").absolutePath,
            cert = context.filesDir.resolve("signing.der").absolutePath,
        )
    }
}
