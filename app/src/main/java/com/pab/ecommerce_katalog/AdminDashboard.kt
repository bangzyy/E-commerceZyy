package com.pab.ecommerce_katalog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Gunakan NavigationUI untuk menghubungkan BottomNavigationView dengan NavController
        // Ini memastikan state tab dijaga dan navigasi antar tab berjalan lancar tanpa tumpang tindih
        NavigationUI.setupWithNavController(bottomNavigation, navController)

        bottomNavigation.setOnItemSelectedListener { item ->
            // Pastikan kita melakukan navigasi yang benar dan membersihkan backstack jika perlu
            // agar tidak perlu klik tombol kembali berkali-kali
            if (item.itemId != navController.currentDestination?.id) {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}