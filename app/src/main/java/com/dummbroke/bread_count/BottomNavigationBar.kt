package com.dummbroke.bread_count

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dummbroke.bread_count.dashboard.DashboardPage
import com.dummbroke.bread_count.inventory.InventoryPage
import com.dummbroke.bread_count.transaction.TransactionPage
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationBar : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bar_navigation)

        initializeViews()
        if (savedInstanceState == null) {
            setInitialFragment()
        }
    }

    private fun initializeViews() {
        try {
            bottomNavigationView = findViewById(R.id.bottomNavigationView)
            setupBottomNavigation()
            setupBackPressedCallback()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setInitialFragment() {
        try {
            val initialFragment = DashboardPage()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, initialFragment)
                .commit()
            currentFragment = initialFragment
            bottomNavigationView.selectedItemId = R.id.navigation_dashboard
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    updateNavigationSelection()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun updateNavigationSelection() {
        try {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            val menuItemId = when (currentFragment) {
                is DashboardPage -> R.id.navigation_dashboard
                is InventoryPage -> R.id.navigation_inventory
                is TransactionPage -> R.id.navigation_transaction
                else -> R.id.navigation_dashboard
            }
            bottomNavigationView.selectedItemId = menuItemId
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnItemSelectedListener { item ->
                handleNavigation(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNavigation(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    if (currentFragment !is DashboardPage) {
                        loadFragment(DashboardPage())
                    }
                    true
                }
                R.id.navigation_inventory -> {
                    if (currentFragment !is InventoryPage) {
                        loadFragment(InventoryPage())
                    }
                    true
                }
                R.id.navigation_transaction -> {
                    if (currentFragment !is TransactionPage) {
                        loadFragment(TransactionPage())
                    }
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .apply {
                    replace(R.id.fragmentContainer, fragment)
                    if (currentFragment != null) {
                        addToBackStack(null)
                    }
                    commit()
                }
            currentFragment = fragment
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}