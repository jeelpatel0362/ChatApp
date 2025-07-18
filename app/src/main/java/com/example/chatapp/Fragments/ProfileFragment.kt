package com.example.chatapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        auth = Firebase.auth
        displayUserInfo(view)
        return view
    }

    private fun displayUserInfo(view: View) {
        val user = auth.currentUser
        user?.let {
            view.findViewById<TextView>(R.id.tvUserName).text = it.displayName ?: "No name"
            view.findViewById<TextView>(R.id.tvUserEmail).text = it.email ?: "No email"
            it.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(view.findViewById(R.id.ivProfile))
            } ?: run {

                Glide.with(this)
                    .load(R.drawable.ic_profile)
                    .into(view.findViewById(R.id.ivProfile))
            }
        }
    }
}
