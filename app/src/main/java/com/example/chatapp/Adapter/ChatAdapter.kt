package com.example.chatapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.model.Chat
import com.example.chatapp.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var chats: List<Chat>,
    private val currentUserId: String,
    private val onItemClick: (Chat) -> Unit,
    private var participantNames: Map<String, String> = emptyMap(),
    private var participantProfileImages: Map<String, String> = emptyMap()
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.ivProfile)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val lastMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val time: TextView = itemView.findViewById(R.id.tvTime)
    }

    fun updateChats(
        newChats: List<Chat>,
        newParticipantNames: Map<String, String>? = null,
        newParticipantProfileImages: Map<String, String>? = null
    ) {
        this.chats = newChats.toList()
        newParticipantNames?.let { this.participantNames = it }
        newParticipantProfileImages?.let { this.participantProfileImages = it }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        val otherParticipantId = chat.participantIds.firstOrNull { it != currentUserId }

        val participantName = otherParticipantId?.let { participantNames[it] } ?: "Unknown User"
        holder.name.text = participantName

        holder.lastMessage.text = if (chat.lastMessageSender == currentUserId) {
            "You: ${chat.lastMessage}"
        } else {
            chat.lastMessage.takeIf { it.isNotBlank() } ?: "No messages yet"
        }

        holder.time.text = formatTimestamp(chat.lastMessageTimestamp)

        // Load profile image if available
        otherParticipantId?.let { participantId ->
            val profileImageUrl = participantProfileImages[participantId]
            if (!profileImageUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.profileImage)
            } else {

                holder.profileImage.setImageResource(R.drawable.ic_person)
            }
        } ?: run {
            holder.profileImage.setImageResource(R.drawable.ic_person)
        }

        holder.itemView.setOnClickListener { onItemClick(chat) }
    }


    override fun getItemCount() = chats.size

    private fun formatTimestamp(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}