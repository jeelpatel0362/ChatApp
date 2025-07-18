package com.example.chatapp.ui

import MoreFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.chatapp.Fragments.ChatsFragment
import com.example.chatapp.Fragments.ProfileFragment
import com.example.chatapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()

        loadFragment(ChatsFragment())

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_Chats -> {
                    loadFragment(ChatsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                R.id.nav_more -> {
                    loadFragment(MoreFragment())
                    true
                }
                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.nav_Chats
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        if (fragment is ChatsFragment) {
            fragment.loadUserChats(auth.currentUser?.uid ?: return)
        }
    }
}