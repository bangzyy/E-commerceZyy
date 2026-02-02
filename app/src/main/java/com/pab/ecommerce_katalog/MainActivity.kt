package com.pab.ecommerce_katalog

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pab.ecommerce_katalog.fragments.CartFragment
import com.pab.ecommerce_katalog.fragments.HomeFragment
import com.pab.ecommerce_katalog.fragments.OrderFragment
import com.pab.ecommerce_katalog.fragments.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var searchView: SearchView
    private lateinit var header: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        searchView = findViewById(R.id.searchView)
        header = findViewById(R.id.header)

        // Load the default fragment
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    header.visibility = View.VISIBLE
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_cart -> {
                    header.visibility = View.GONE
                    loadFragment(CartFragment())
                    true
                }
                R.id.navigation_order -> {
                    header.visibility = View.GONE
                    loadFragment(OrderFragment())
                    true
                }
                R.id.navigation_profile -> {
                    header.visibility = View.GONE
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        setupSearchView()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterHomeFragment(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterHomeFragment(newText)
                return true
            }
        })
    }

    private fun filterHomeFragment(query: String?) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is HomeFragment) {
            fragment.filterProductsByName(query)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
