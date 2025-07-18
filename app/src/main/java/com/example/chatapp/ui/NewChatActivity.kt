package com.example.chatapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.Adapter.UserAdapter
import com.example.chatapp.Repository.ChatRepository
import com.example.chatapp.R
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class NewChatActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val TAG = "NewChatActivity"
    private val chatRepository = ChatRepository()
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_chat)

        progressBar = findViewById(R.id.progressBar)

        toolbar = findViewById(R.id.toolbar)
        usersRecyclerView = findViewById(R.id.usersRecyclerView)

        auth = Firebase.auth


        setupToolbar()
        setupRecyclerView()
        fetchRegisteredUsers()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select User"
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(userList) { selectedUser ->
            startChatWithUser(selectedUser)
        }

        usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NewChatActivity)
            adapter = userAdapter
            setHasFixedSize(true)
        }
    }

    private fun fetchRegisteredUsers() {
        val currentUserId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "Current user ID is null")
            return
        }

        progressBar.visibility = View.VISIBLE
        usersRecyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val users = chatRepository.getUsersWithAutoFix(currentUserId)
                userList.clear()
                userList.addAll(users)
                userAdapter.notifyDataSetChanged()

                if (userList.isEmpty()) {
                    Toast.makeText(
                        this@NewChatActivity,
                        "No other users found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching users: ${e.message}")
                Toast.makeText(
                    this@NewChatActivity,
                    "Failed to load users",
                    Toast.LENGTH_SHORT
                ).show()
            }finally {
                progressBar.visibility = View.GONE
                usersRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun startChatWithUser(user: User) {
        val currentUserId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val chatId = chatRepository.createNewChat(currentUserId, user.uid)

                // Open ChatConversationActivity
                val intent = Intent(this@NewChatActivity, ChatConversationActivity::class.java).apply {
                    putExtra("chatId", chatId)
                    putExtra("userId", user.uid)
                    putExtra("userName", user.name)
                    if (user.profileImage.isNotEmpty()) {
                        putExtra("profileImage", user.profileImage)
                    }
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)

                // Notify parent activity to refresh
                setResult(RESULT_OK)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Error creating chat: ${e.message}")
                Toast.makeText(
                    this@NewChatActivity,
                    "Failed to start chat",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}