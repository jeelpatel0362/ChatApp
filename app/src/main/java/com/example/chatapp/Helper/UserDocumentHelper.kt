package com.example.chatapp.Helper

import android.util.Log
import com.example.chatapp.model.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

object UserDocumentHelper {
    private const val TAG = "UserDocHelper"
    private val db = Firebase.firestore

    suspend fun ensureUserDocument(userId: String): User {
        return try {
            val docRef = db.collection("users").document(userId)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                createNewUserDocument(userId)
            } else {
                val updates = HashMap<String, Any>()
                val data = doc.data ?: emptyMap()

                if (!data.containsKey("uid")) updates["uid"] = userId
                if (!data.containsKey("name")) updates["name"] = "User_${userId.take(4)}"
                if (!data.containsKey("email")) updates["email"] = ""

                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                    Log.d(TAG, "Fixed missing fields for $userId")
                }

                doc.toObject(User::class.java) ?: User(uid = userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring user document", e)
            User(uid = userId) // Fallback
        }
    }

    private suspend fun createNewUserDocument(userId: String): User {
        val user = User(
            uid = userId,
            name = "New_User_${userId.take(4)}",
            email = ""
        )
        db.collection("users")
            .document(userId)
            .set(user)
            .await()
        return user
    }
}