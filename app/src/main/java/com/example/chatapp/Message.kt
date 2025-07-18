package com.example.chatapp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImage: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)