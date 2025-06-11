package com.dummbroke.bread_count

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.dummbroke.bread_count.dashboard.DashboardPage
import com.dummbroke.bread_count.inventory.InventoryPage
import com.dummbroke.bread_count.transaction.TransactionPage
import com.dummbroke.bread_count.databinding.ActivityMainBinding
import com.dummbroke.bread_count.signup.SignInActivity
import com.dummbroke.bread_count.utils.NavigationHandler
import com.google.firebase.auth.FirebaseAuth
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import android.view.Gravity

class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationHandler: NavigationHandler
    private val TAG = "MainActivity"

    // For double-tap exit
    private var backPressedTime: Long = 0
    private var backToast: Toast? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting MainActivity")
        
        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Use ViewBinding to inflate the layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase Auth initialized")

        // Initialize Navigation Handler
        navigationHandler = NavigationHandler(supportFragmentManager, binding.bottomNavigationView)
        Log.d(TAG, "NavigationHandler initialized")

        setupWindowInsets()

        // Add the back stack listener
        supportFragmentManager.addOnBackStackChangedListener(this)
        // Handle initial state in case activity is recreated
        updateBottomNavSelection()
        
        // Add the custom back press callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showLogoutDialog()
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    // Listener for back stack changes
    override fun onBackStackChanged() {
        updateBottomNavSelection()
    }

    private fun updateBottomNavSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val menuId = when (currentFragment) {
            is DashboardPage -> R.id.navigation_dashboard
            is InventoryPage -> R.id.navigation_inventory
            is TransactionPage -> R.id.navigation_transaction
            else -> null // Or a default ID if applicable
        }

        menuId?.let {
            // Check if already selected to avoid triggering listener unnecessarily
            if (binding.bottomNavigationView.selectedItemId != it) {
                Log.d(TAG, "Updating BottomNav selection to: $it based on fragment ${currentFragment?.javaClass?.simpleName}")
                binding.bottomNavigationView.selectedItemId = it
            }
        }
    }

    // Custom back press logic
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            
            // Check if on Dashboard and if there are fragments in the back stack
            if (currentFragment is DashboardPage && supportFragmentManager.backStackEntryCount == 0) {
                 // Implement double-tap-to-exit logic
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    backToast?.cancel() // Cancel the toast if exiting
                    // Call finish() instead of isEnabled = false and super.onBackPressed() 
                    // to ensure the activity finishes correctly.
                    finish() 
                    return
                } else {
                    backToast = Toast.makeText(baseContext, "Press back again to exit", Toast.LENGTH_SHORT)
                    backToast?.show()
                }
                backPressedTime = System.currentTimeMillis()
            } else {
                // Default behavior: Pop back stack or finish activity if stack is empty
                // Allow the default system back press behavior.
                // Temporarily disable this callback, invoke the dispatcher, then re-enable.
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
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
                    // Account still exists, proceed with current view
                    Log.d(TAG, "User is authenticated and account exists")
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

    private fun navigateToSignIn() {
        Log.d(TAG, "Navigating to Sign In")
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun setupWindowInsets() {
        Log.d(TAG, "Setting up window insets")
        
        // Make status bar icons dark
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Handle insets for the root view
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = insets.top,
                left = insets.left,
                right = insets.right,
                bottom = 0
            )
            WindowInsetsCompat.CONSUMED
        }

        // Handle insets for the bottom navigation
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.Builder(windowInsets).setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                androidx.core.graphics.Insets.of(0, 0, 0, 0)
            ).build()
        }
    }
    
    override fun onDestroy() {
        // Remove listener to prevent memory leaks
        supportFragmentManager.removeOnBackStackChangedListener(this)
        super.onDestroy()
    }

    fun openDrawer() {
        drawerLayout.openDrawer(Gravity.START)
    }

    private fun showLogoutDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_logout_confirmation)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        dialog.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<android.widget.Button>(R.id.confirmButton).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, com.dummbroke.bread_count.signup.SignInActivity::class.java))
            finish()
            dialog.dismiss()
        }
        dialog.show()
        // Fix: Set dialog width to 90% of screen width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }
} 