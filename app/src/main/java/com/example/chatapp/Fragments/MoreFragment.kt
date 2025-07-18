import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.chatapp.R
import com.example.chatapp.ui.SignInActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MoreFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.logoutItem).setOnClickListener {
            showLogoutConfirmation()
        }

        view.findViewById<View>(R.id.aboutItem).setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireActivity(), SignInActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showAboutDialog() {
        val aboutMessage = """
            ChatApp v1.0
            
            A simple, secure messaging app that lets you 
            connect with friends and family instantly.
            
            Features:
            • Real-time messaging
            • User profiles
            • Secure authentication
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("About")
            .setMessage(aboutMessage)
            .setPositiveButton("OK", null)
            .show()
    }
}