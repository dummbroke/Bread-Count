package com.dummbroke.bread_count

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        setupBottomNavigation()
        
        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardPage(), false)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            handleNavigation(item)
            true
        }
    }

    private fun handleNavigation(item: MenuItem) {
        when (item.itemId) {
            R.id.navigation_dashboard -> {
                if (currentFragment !is DashboardPage) {
                    loadFragment(DashboardPage(), true)
                }
            }
            R.id.navigation_inventory -> {
                if (currentFragment !is InventoryPage) {
                    loadFragment(InventoryPage(), true)
                }
            }
            R.id.navigation_transaction -> {
                if (currentFragment !is TransactionPage) {
                    loadFragment(TransactionPage(), true)
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace the current fragment
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)

        // Add to back stack if needed
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }

        // Commit the transaction
        fragmentTransaction.commit()
        
        // Update current fragment
        currentFragment = fragment
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}