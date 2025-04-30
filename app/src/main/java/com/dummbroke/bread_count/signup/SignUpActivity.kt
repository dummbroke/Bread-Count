package com.dummbroke.bread_count.signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dummbroke.bread_count.MainActivity
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Suppress("DEPRECATION")
class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "SignUpActivity"
    
    // For double-tap exit
    private var backPressedTime: Long = 0
    private var backToast: Toast? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In result received")
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google Sign-In successful, account: ${account?.email}")
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed during result processing: ${e.statusCode} - ${e.message}")
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(TAG, "Google Sign-In cancelled or failed with resultCode: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting SignUpActivity")
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase Auth initialized")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "Google Sign-In client configured")

        setupClickListeners()
        setupWindowInsets()
        
        // Add the custom back press callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    // Custom back press logic
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // If this is the only activity in the task stack (launcher activity)
            if (isTaskRoot) { 
                 if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    backToast?.cancel()
                    // Finish the activity, which will close the app if it's the root
                    finish() 
                    return
                } else {
                    backToast = Toast.makeText(baseContext, "Press back again to exit", Toast.LENGTH_SHORT)
                    backToast?.show()
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

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")
        binding.signUpButton.setOnClickListener {
            Log.d(TAG, "Sign Up button clicked")
            signUpWithEmail()
        }

        binding.googleSignInButton.setOnClickListener {
            Log.d(TAG, "Google Sign-In button clicked")
            signInWithGoogle()
        }

        binding.signInButton.setOnClickListener {
            Log.d(TAG, "Sign In button clicked")
            // Simply finish this activity to go back to SignInActivity
            finish() 
        }
    }

    private fun signUpWithEmail() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()
        Log.d(TAG, "Attempting email sign up with email: $email")

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Log.w(TAG, "Sign up failed: Empty fields")
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Log.w(TAG, "Sign up failed: Passwords do not match")
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Log.w(TAG, "Sign up failed: Password too short")
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sign up successful")
                    navigateToDashboard()
                } else {
                    Log.e(TAG, "Email sign up failed: ${task.exception?.message}")
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In flow")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "Authenticating with Firebase using Google token")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase authentication with Google successful")
                    navigateToDashboard()
                } else {
                    Log.e(TAG, "Firebase authentication with Google failed: ${task.exception?.message}")
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
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