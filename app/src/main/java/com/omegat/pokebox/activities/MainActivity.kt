package com.omegat.pokebox.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.omegat.pokebox.R
import com.omegat.pokebox.data.CardRepository
import com.omegat.pokebox.fragments.DecksFragment
import com.omegat.pokebox.fragments.HomeFragment
import com.omegat.pokebox.fragments.LogFragment
import com.omegat.pokebox.fragments.SearchFragment

class MainActivity : AppCompatActivity() {

    private var currentFragmentIndex = 0
    private lateinit var navPill: View
    private lateinit var bottomNavigation: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if cards need to be loaded
        if (CardRepository.getCards().isEmpty()) {
            val intent = Intent(this, LoadingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navPill = findViewById(R.id.navPill)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val btHome = findViewById<LinearLayout>(R.id.btNavHome)
        val btLog = findViewById<LinearLayout>(R.id.btNavLog)
        val btSearch = findViewById<LinearLayout>(R.id.btNavSearch)
        val btDecks = findViewById<LinearLayout>(R.id.btNavDecks)

        // Initialize pill position after layout
        navPill.post {
            animatePillTo(0, animate = false)
        }

        btHome.setOnClickListener {
            if (currentFragmentIndex != 0) {
                animatePillTo(0)
                loadFragment(HomeFragment())
                currentFragmentIndex = 0
            }
        }

        btLog.setOnClickListener {
            if (currentFragmentIndex != 1) {
                animatePillTo(1)
                loadFragment(LogFragment())
                currentFragmentIndex = 1
            }
        }

        btSearch.setOnClickListener {
            if (currentFragmentIndex != 2) {
                animatePillTo(2)
                loadFragment(SearchFragment())
                currentFragmentIndex = 2
            }
        }

        btDecks.setOnClickListener {
            if (currentFragmentIndex != 3) {
                animatePillTo(3)
                loadFragment(DecksFragment())
                currentFragmentIndex = 3
            }
        }
    }

    private fun animatePillTo(position: Int, animate: Boolean = true) {
        val navContainer = findViewById<View>(R.id.navContainer)
        val containerWidth = navContainer.width
        val pillWidth = navPill.width
        val itemWidth = containerWidth / 4f
        val targetX = (itemWidth * position) + (itemWidth - pillWidth) / 2

        if (animate) {
            ObjectAnimator.ofFloat(navPill, "translationX", targetX).apply {
                duration = 300
                start()
            }
        } else {
            navPill.translationX = targetX
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
