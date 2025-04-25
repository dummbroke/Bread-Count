package com.dummbroke.bread_count

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dummbroke.bread_count.signup.SignInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting MainActivity")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase Auth initialized")

        setupWindowInsets()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Checking authentication state")
        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, verify the account still exists
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Account still exists, proceed to dashboard
                    Log.d(TAG, "User is authenticated and account exists")
                    navigateToDashboard()
                } else {
                    // Account might have been deleted, sign out and go to sign in
                    Log.e(TAG, "Account verification failed: ${task.exception?.message}")
                    auth.signOut()
                    navigateToSignIn()
                }
            }
        } else {
            // No user is signed in
            Log.d(TAG, "No user is signed in")
            navigateToSignIn()
        }
    }

    private fun navigateToDashboard() {
        Log.d(TAG, "Navigating to Dashboard")
        startActivity(Intent(this, BottomNavigationBar::class.java))
        finish()
    }

    private fun navigateToSignIn() {
        Log.d(TAG, "Navigating to Sign In")
        startActivity(Intent(this, SignInActivity::class.java))
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