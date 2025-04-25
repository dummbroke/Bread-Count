package com.dummbroke.bread_count.signup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dummbroke.bread_count.R
import com.dummbroke.bread_count.databinding.ActivitySignUpBinding
import com.dummbroke.bread_count.BottomNavigationBar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting SignUpActivity")
        binding = ActivitySignUpBinding.inflate(layoutInflater)
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
            startActivity(Intent(this, SignInActivity::class.java))
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
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult called with requestCode: $requestCode")

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                Log.d(TAG, "Google Sign-In successful, authenticating with Firebase")
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed: ${e.message}")
                Toast.makeText(this, "Google sign in failed: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
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
        startActivity(Intent(this, BottomNavigationBar::class.java))
        finish()
    }

    private fun setupWindowInsets() {
        Log.d(TAG, "Setting up window insets")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}