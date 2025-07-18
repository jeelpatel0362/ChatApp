package com.example.chatapp.model

data class Chat(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSender: String = ""
)