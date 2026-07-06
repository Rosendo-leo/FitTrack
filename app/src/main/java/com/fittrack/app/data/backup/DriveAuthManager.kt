package com.fittrack.app.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Login Google com escopo drive.appdata (pasta oculta do app no Drive).
 * Requer um OAuth client Android (package + SHA-1) no Google Cloud Console
 * com a Drive API habilitada — ver docs/DRIVE_SETUP.md.
 */
@Singleton
class DriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    }

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(SCOPE_APPDATA))
        .build()

    private val client get() = GoogleSignIn.getClient(context, signInOptions)

    fun signInIntent(): Intent = client.signInIntent

    /** Conta logada com o escopo do Drive concedido, ou null. */
    fun currentAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)
            ?.takeIf { GoogleSignIn.hasPermissions(it, Scope(SCOPE_APPDATA)) }

    fun signOut() {
        client.signOut()
    }

    /** Header Authorization pronto para a API do Drive. */
    suspend fun authorizationHeader(): String {
        val account = currentAccount()?.account
            ?: throw IllegalStateException("Nenhuma conta Google conectada.")
        val token = withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(context, account, "oauth2:$SCOPE_APPDATA")
        }
        return "Bearer $token"
    }
}
