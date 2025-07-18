package com.example.chatapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.Adapter.MessageAdapter
import com.example.chatapp.ChatRepository
import com.example.chatapp.Message
import com.example.chatapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChatConversationActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar

    private lateinit var messageAdapter: MessageAdapter
    private val chatRepository = ChatRepository()
    private val participantNames = mutableMapOf<String, String>()

    private lateinit var auth: FirebaseAuth
    private var chatId: String = ""
    private var recipientId: String = ""
    private var recipientName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_conversation)

        initializeViews()
        setupFirebase()
        getIntentExtras()
        setupUI()
        setupChat()
        setupClickListeners()
    }

    private fun initializeViews() {
        tvUserName = findViewById(R.id.tvUserName)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupFirebase() {
        auth = Firebase.auth
    }

    private fun getIntentExtras() {
        intent.getStringExtra("chatId")?.let {
            chatId = it
        } ?: run {
            recipientId = intent.getStringExtra("userId") ?: run {
                Toast.makeText(this, "Error: No user selected", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val currentUserId = auth.currentUser?.uid ?: return
            chatId = if (currentUserId < recipientId) {
                "${currentUserId}_$recipientId"
            } else {
                "${recipientId}_$currentUserId"
            }
        }

        recipientName = intent.getStringExtra("userName") ?: ""
    }

    private fun setupUI() {
        tvUserName.text = recipientName

        auth.currentUser?.let { currentUser ->
            participantNames[currentUser.uid] = currentUser.displayName ?: "You"
            participantNames[recipientId] = recipientName
        }

        messageAdapter = MessageAdapter(
            messages = mutableListOf(),
            currentUserId = auth.currentUser?.uid ?: "",
            participantNames = participantNames
        )

        rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatConversationActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupChat() {
        loadMessages()
    }

    private fun loadMessages() {
        progressBar.visibility = View.VISIBLE
        chatRepository.getMessages(chatId) { messages ->
            progressBar.visibility = View.GONE
            messageAdapter.updateMessages(messages)
            rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra("refreshNeeded", true)
            })
            finish()
        }

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun sendMessage(messageText: String) {
        val currentUser = auth.currentUser ?: return
        val message = Message(
            id = System.currentTimeMillis().toString(),
            senderId = currentUser.uid,
            senderName = currentUser.displayName ?: "You",
            text = messageText,
            timestamp = System.currentTimeMillis()
        )

        progressBar.visibility = View.VISIBLE
        chatRepository.sendMessage(chatId, message) { success ->
            progressBar.visibility = View.GONE
            if (success) {
                etMessage.text.clear()
                loadMessages()
            } else {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}