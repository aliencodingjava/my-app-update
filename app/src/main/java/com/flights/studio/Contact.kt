package com.flights.studio

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigationrail.NavigationRailView

@Suppress("DEPRECATION")
class Contact : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        window.decorView.alpha = 1.0f

        val navRail = findViewById<NavigationRailView>(R.id.navigation_rail)

        // Ensure ContactFragment loads first and is visible
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.rootFrameLayout, ContactFragment())
                .commitNow()

        }


        // ✅ Set the selected item to "Contacts"
        navRail.menu.findItem(R.id.nav_contacts).isChecked = true

        navRail.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    goToHomeScreen()
                    true
                }
                R.id.nav_contacts -> {
                    // Stay on ContactActivity (do nothing)
                    true
                }
                R.id.nav_all_contacts -> {
                    goToAllContactsScreen()
                    true
                }
                R.id.nav_settings -> {
                    goToSettingsScreen()
                    true
                }
                else -> false
            }
        }
    }

    // NEW: MainActivity::class.java (Compose home / dashboard)
    private fun goToHomeScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }


    private fun goToAllContactsScreen() {
        val intent = Intent(this, AllContactsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    private fun goToSettingsScreen() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
