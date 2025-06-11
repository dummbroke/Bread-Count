package com.dummbroke.bread_count.signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dummbroke.bread_count.MainActivity
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.databinding.ActivitySignInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.GoogleAuthProvider

// Suppress specific deprecation warnings for GoogleSignIn API
@Suppress("DEPRECATION")
class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "SignInActivity"
    private var authStateListener: AuthStateListener? = null

    // For double-tap exit
    private var backPressedTime: Long = 0

    // Register for activity result
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In result received")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful, account: ${account?.email}")
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            // Google Sign In failed
            Log.e(TAG, "Google Sign-In failed: ${e.statusCode} - ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting SignInActivity")
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase Auth initialized")

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "Google Sign-In client configured")

        setupClickListeners()
        setupWindowInsets()
        setupAuthStateListener()
        
        // Add the custom back press callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    // Custom back press logic
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // If this is the only activity in the task stack (launcher activity)
            if (isTaskRoot) { 
                 if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    // Finish the activity, which will close the app if it's the root
                    finish() 
                    return
                } else {
                }
                backPressedTime = System.currentTimeMillis()
            } else {
                // If not the root, allow normal back press behavior (finish current activity)
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true // Re-enable afterwards, though likely not needed as activity finishes
            }
        }
    }

    private fun setupAuthStateListener() {
        authStateListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in, verify the account still exists
                user.reload().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Account exists, navigate to dashboard
                        Log.d(TAG, "User is authenticated and account exists")
                        navigateToDashboard()
                    } else {
                        // Account might have been deleted, sign out
                        Log.e(TAG, "Account verification failed: ${task.exception?.message}")
                        auth.signOut()
                        googleSignInClient.signOut()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Adding auth state listener")
        authStateListener?.let { auth.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Removing auth state listener")
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")
        binding.loginButton.setOnClickListener {
            Log.d(TAG, "Login button clicked")
            signInWithEmail()
        }

        binding.googleSignInButton.setOnClickListener {
            Log.d(TAG, "Google Sign-In button clicked")
            signInWithGoogle()
        }

        binding.signUpButton.setOnClickListener {
            Log.d(TAG, "Sign Up button clicked")
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun signInWithEmail() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()
        Log.d(TAG, "Attempting email sign in with email: $email")

        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "Sign in failed: Empty email or password")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sign in successful")
                    // Auth state listener will handle navigation
                } else {
                    Log.e(TAG, "Email sign in failed: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In flow")
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "Authenticating with Firebase using Google token")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase authentication with Google successful")
                    // Auth state listener will handle navigation
                } else {
                    Log.e(TAG, "Firebase authentication with Google failed: ${task.exception?.message}")
                }
            }
    }

    private fun navigateToDashboard() {
        Log.d(TAG, "Navigating to Dashboard")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupWindowInsets() {
        Log.d(TAG, "Setting up window insets")
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}