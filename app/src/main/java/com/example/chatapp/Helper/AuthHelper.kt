package com.example.chatapp.Helper

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.example.chatapp.R
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore

class AuthHelper(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = Firebase.firestore

    init {
        configureGoogleSignIn()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        return googleSignInClient
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        callback: AuthCallback
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(auth.currentUser)
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException ->
                            context.getString(R.string.error_invalid_credentials)

                        else ->
                            task.exception?.message ?: context.getString(R.string.error_auth_failed)
                    }
                    callback.onFailure(error)
                }
            }
    }

    fun signInWithGoogle(idToken: String, callback: AuthCallback) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            createOrUpdateUserDocument(it).addOnCompleteListener { docTask ->
                                if (docTask.isSuccessful) {
                                    callback.onSuccess(user)
                                } else {
                                    callback.onFailure("Authentication succeeded but user profile couldn't be saved")
                                }
                            }
                        } ?: callback.onSuccess(null)
                    } else {
                        val error = when (task.exception) {
                            is FirebaseAuthInvalidCredentialsException ->
                                context.getString(R.string.error_invalid_google_credentials)

                            is FirebaseAuthUserCollisionException ->
                                context.getString(R.string.error_account_exists)

                            else ->
                                task.exception?.message
                                    ?: context.getString(R.string.error_google_auth_failed)
                        }
                        callback.onFailure(error)
                    }
                }
        } catch (e: Exception) {
            callback.onFailure(context.getString(R.string.error_google_auth_error, e.message))
        }
    }

    fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        callback: AuthCallback
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        createOrUpdateUserDocument(it).addOnCompleteListener { docTask ->
                            if (docTask.isSuccessful) {
                                callback.onSuccess(user)
                            } else {
                                callback.onFailure("Registration succeeded but user profile couldn't be saved")
                            }
                        }
                    } ?: callback.onSuccess(null)
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException ->
                            context.getString(R.string.error_invalid_email)

                        is FirebaseAuthUserCollisionException ->
                            context.getString(R.string.error_email_in_use)

                        else ->
                            task.exception?.message
                                ?: context.getString(R.string.error_registration_failed)
                    }
                    callback.onFailure(error)
                }
            }
    }

    fun sendPasswordResetEmail(email: String, callback: AuthCallback) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onSuccess(null)
                } else {
                    callback.onFailure(
                        task.exception?.message
                            ?: context.getString(R.string.error_reset_email_failed)
                    )
                }
            }
    }

    fun checkRememberMeStatus(): Boolean {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getBoolean("remember_me", false)
    }

    fun setRememberMeStatus(rememberMe: Boolean) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("remember_me", rememberMe)
            .apply()
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }

    fun createOrUpdateUserDocument(user: FirebaseUser): Task<Void> {
        val userDoc = db.collection("users").document(user.uid)

        val userData = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "name" to (user.displayName ?: "User_${user.uid.take(4)}"),
            "profileImage" to (user.photoUrl?.toString() ?: "")
        )

        return userDoc.set(userData, SetOptions.merge())
    }

    interface AuthCallback {
        fun onSuccess(user: FirebaseUser?)
        fun onFailure(errorMessage: String)
    }
}