package com.example.chatapp.Fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.Adapter.ChatAdapter
import com.example.chatapp.Repository.ChatRepository
import com.example.chatapp.R
import com.example.chatapp.model.Chat
import com.example.chatapp.ui.ChatConversationActivity
import com.example.chatapp.ui.NewChatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatRepository = ChatRepository()
    private lateinit var fabNewChat: FloatingActionButton
    private lateinit var auth: FirebaseAuth
    private val participantNames = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        auth = Firebase.auth

        fabNewChat = view.findViewById(R.id.fabNewChat)
        recyclerView = view.findViewById(R.id.rvChats)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val currentUserId = auth.currentUser?.uid ?: ""

        chatAdapter = ChatAdapter(
            emptyList(),
            currentUserId,
            { chat -> openChatConversation(chat) },
            participantNames
        )
        recyclerView.adapter = chatAdapter

        loadUserChats(currentUserId)

        fabNewChat.setOnClickListener {
            openNewChatActivity()
        }

        return view
    }

    fun loadUserChats(userId: String) {
        lifecycleScope.launch {
            try {
                Log.d("ChatsFragment", "Loading chats for user: $userId")
                val (chats, namesMap) = chatRepository.getChatsWithParticipants(userId)

                activity?.runOnUiThread {
                    participantNames.clear()
                    participantNames.putAll(namesMap)
                    chatAdapter.updateChats(
                        chats.sortedByDescending { it.lastMessageTimestamp },
                        namesMap
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error loading chats", e)
            }
        }
    }

    private val chatConversationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK &&
            result.data?.getBooleanExtra("refreshNeeded", false) == true) {
            loadUserChats(auth.currentUser?.uid ?: return@registerForActivityResult)
        }
    }

    private fun openChatConversation(chat: Chat) {
        val otherParticipantId = chat.participantIds.firstOrNull { it != auth.currentUser?.uid }
        val otherParticipantName = otherParticipantId?.let { participantNames[it] } ?: "Unknown User"

        val intent = Intent(requireActivity(), ChatConversationActivity::class.java).apply {
            putExtra("chatId", chat.chatId)
            putExtra("userId", otherParticipantId)
            putExtra("userName", otherParticipantName)
        }
        chatConversationLauncher.launch(intent)
    }

    private fun openNewChatActivity() {
        val intent = Intent(requireActivity(), NewChatActivity::class.java)
        startActivity(intent)
    }
}