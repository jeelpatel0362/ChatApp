package com.example.chatapp

import android.util.Log
import com.example.chatapp.model.Chat
import com.example.chatapp.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = Firebase.firestore

    suspend fun getUsersWithAutoFix(currentUserId: String): List<User> {
        return try {
            val query = db.collection("users")
                .whereNotEqualTo("uid", currentUserId)
                .get()
                .await()

            query.documents.mapNotNull { doc ->
                ensureUserDocument(doc).apply {
                    name = doc.getString("name") ?: name
                    email = doc.getString("email") ?: email
                    profileImage = doc.getString("profileImage") ?: profileImage
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun ensureUserDocument(doc: DocumentSnapshot): User {
        val updates = HashMap<String, Any>()
        val data = doc.data ?: emptyMap()

        if (!data.containsKey("uid")) updates["uid"] = doc.id
        if (!data.containsKey("name")) updates["name"] = "User_${doc.id.take(4)}"
        if (!data.containsKey("email")) updates["email"] = ""
        if (!data.containsKey("profileImage")) updates["profileImage"] = ""

        if (updates.isNotEmpty()) {
            doc.reference.update(updates).await()
        }

        return doc.toObject(User::class.java) ?: User(
            uid = doc.id,
            name = updates["name"] as String,
            email = updates["email"] as String,
            profileImage = updates["profileImage"] as String
        )
    }

    fun getUserNames(userIds: List<String>, callback: (Map<String, String>) -> Unit) {
        if (userIds.isEmpty()) {
            callback(emptyMap())
            return
        }

        val namesMap = mutableMapOf<String, String>()
        val batchSize = 10
        val batches = userIds.chunked(batchSize)
        val completedBatches = mutableListOf<Int>()

        batches.forEachIndexed { index, batch ->
            db.collection("users")
                .whereIn("uid", batch)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    querySnapshot.documents.forEach { doc ->
                        val uid = doc.getString("uid") ?: doc.id
                        val name = doc.getString("name") ?: "Unknown User"
                        namesMap[uid] = name
                    }
                    completedBatches.add(index)
                    if (completedBatches.size == batches.size) {
                        callback(namesMap)
                    }
                }
                .addOnFailureListener {
                    completedBatches.add(index)
                    if (completedBatches.size == batches.size) {
                        callback(namesMap)
                    }
                }
        }
    }

    fun getUserChats(userId: String, onChatsLoaded: (List<Chat>) -> Unit) {
        db.collection("user_chats")
            .document(userId)
            .collection("active_chats")
            .get()
            .addOnSuccessListener { result ->
                val chats = result.documents.mapNotNull {
                    it.toObject(Chat::class.java)
                }
                onChatsLoaded(chats)
            }
            .addOnFailureListener {
                onChatsLoaded(emptyList())
            }
    }


    fun getMessages(chatId: String, onMessagesLoaded: (List<Message>) -> Unit) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val messages = result.documents.mapNotNull {
                    it.toObject(Message::class.java)
                }
                onMessagesLoaded(messages)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error loading messages", e)
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("Firestore", "Permission denied. Check security rules.")
                }
                onMessagesLoaded(emptyList())
            }
    }

    fun sendMessage(chatId: String, message: Message, onComplete: (Boolean) -> Unit) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.id)
            .set(message)
            .addOnSuccessListener {
                updateLastMessage(chatId, message)
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error sending message", e)
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("Firestore", "Permission denied. Check security rules.")
                }
                onComplete(false)
            }
    }

    private fun updateLastMessage(chatId: String, message: Message) {
        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { chatDoc ->
                val participants = chatDoc.get("participants") as? List<String> ?: emptyList()
                val recipientId = participants.firstOrNull { it != message.senderId } ?: return@addOnSuccessListener

                val chatData = hashMapOf(
                    "lastMessage" to message.text,
                    "lastMessageSender" to message.senderName,
                    "lastMessageTimestamp" to message.timestamp,
                    "participants" to listOf(message.senderId, recipientId))

                db.collection("chats").document(chatId).update(chatData)
            }
    }

    // In ChatRepository.kt
    suspend fun getChatsWithParticipants(userId: String): Pair<List<Chat>, Map<String, String>> {
        return try {
            // Change "participantsId" to "participants"
            val querySnapshot = db.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()

            val chats = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Chat(
                        chatId = doc.id,
                        participantIds = doc.get("participants") as? List<String> ?: emptyList(),
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                        lastMessageSender = doc.getString("lastMessageSender") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("Firestore", "Error parsing chat ${doc.id}", e)
                    null
                }
            }

            val participantIds = chats.flatMap { it.participantIds }
                .distinct()
                .filter { it != userId }

            val namesMap = if (participantIds.isNotEmpty()) {
                db.collection("users")
                    .whereIn("uid", participantIds)
                    .get()
                    .await()
                    .documents.associate { doc ->
                        (doc.getString("uid") ?: doc.id) to
                                (doc.getString("name") ?: "Unknown")
                    }
            } else emptyMap()

            return chats to namesMap
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting chats", e)
            emptyList<Chat>() to emptyMap()
        }
    }

    suspend fun createNewChat(user1: String, user2: String): String {
        val participants = listOf(user1, user2)
        val chatId = if (user1 < user2) "${user1}_$user2" else "${user2}_$user1"

        val chatData = hashMapOf(
            "participants" to participants,
            "lastMessage" to "",
            "lastMessageTimestamp" to System.currentTimeMillis(),
            "lastMessageSender" to ""
        )

        db.collection("chats")
            .document(chatId)
            .set(chatData)
            .await()

        return chatId
    }
}