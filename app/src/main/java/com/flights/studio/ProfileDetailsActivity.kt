package com.flights.studio

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.flights.studio.databinding.ActivityProfileDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Suppress("DEPRECATION")
class ProfileDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileDetailsBinding
    private lateinit var userPrefs: UserPreferencesManager

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // fallback or notify user
            }
            userPrefs.userPhotoUriString = it.toString()
            userPrefs.userInitials = null
            displayProfilePhoto(it)
            invalidateOptionsMenu()  // in case menu needs update
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPrefs = UserPreferencesManager(this)

        setupToolbar()
        loadAndDisplayProfile()

        // ✅ Predictive Back Support for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
                overridePendingTransition(0, R.anim.zoom_out) // or R.anim.slide_down
            }
        }
    }
    @SuppressLint("GestureBackNavigation")
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.zoom_out)
    }




    private fun setupToolbar() {
        setSupportActionBar(binding.profileToolbar)
        supportActionBar?.apply {
            title = getString(R.string.title_my_profile)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.profileToolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(0, R.anim.zoom_out)
        }
    }

    private fun loadAndDisplayProfile() {
        // 1) Split whatever the user typed
        val rawName = userPrefs.userName?.trim().orEmpty()
        val tokens = rawName
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        // 2) Pull off any leading “ornament” tokens (no letters/digits)
        val leadingOrnaments = tokens.takeWhile { it.none { ch -> ch.isLetterOrDigit() } }
        // 3) Pull off any trailing ones
        val trailingOrnaments = tokens.takeLastWhile { it.none { ch -> ch.isLetterOrDigit() } }

        // 4) The “core” name‐words are those tokens with letters
        val coreWords = tokens
            .filter { it.any { ch -> ch.isLetter() } }
            .take(3)                  // up to 3 words

        // 5) Reassemble: [ornaments…] + core + [ornaments…]
        val displayNameTokens = mutableListOf<String>()
        displayNameTokens += leadingOrnaments
        displayNameTokens += coreWords
        displayNameTokens += trailingOrnaments

        // 6) Join & show (or fallback)
        val displayName = displayNameTokens.joinToString(" ")
        binding.userMainText.text = displayName.ifEmpty {
            getString(R.string.default_user_name)
        }

        // …and keep the rest of your binding logic exactly as before:
        binding.userSecondaryText.text = when {
            !userPrefs.userPhone.isNullOrBlank() -> userPrefs.userPhone
            !userPrefs.userEmail.isNullOrBlank() -> userPrefs.userEmail
            else                                 -> getString(R.string.unknown_contact)
        }
        binding.userEmail.text     = getString(R.string.label_email,    userPrefs.userEmail.orEmpty())
        binding.userBio.text       = getString(R.string.label_bio,      userPrefs.userBio.orEmpty())
        binding.userBirthday.text  = getString(R.string.label_birthday,userPrefs.userBirthday.orEmpty())

        // Photo or initials (initials from first 3 coreWords)
        val initials = coreWords
            .map { it.first().uppercaseChar() }
            .joinToString("")
        userPrefs.getUserPhotoUri()?.let { displayProfilePhoto(it) } ?: run {
            binding.iconInitials.apply {
                visibility = View.VISIBLE
                text       = initials.ifEmpty { "?" }
            }
            binding.iconImage.setImageDrawable(null)
        }

        // Click‐to‐change + dim‐when‐logged‐out
        val clickListener = View.OnClickListener {
            if (userPrefs.isLoggedIn) photoPickerLauncher.launch(arrayOf("image/*"))
            else                       toast(R.string.prompt_create_profile)
        }
        binding.iconImage.setOnClickListener(clickListener)
        binding.iconInitials.setOnClickListener(clickListener)
        val alpha = if (userPrefs.isLoggedIn) 1f else 0.4f
        binding.iconImage.alpha    = alpha
        binding.iconInitials.alpha = alpha
    }

    private fun displayProfilePhoto(uri: Uri) {
        binding.iconInitials.visibility = View.GONE
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.placeholder_background)
            .circleCrop()
            .into(binding.iconImage)
        binding.iconImage.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.profile_toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // 1) Auth-item logic (what you already have)
        val authItem = menu.findItem(R.id.action_auth)
        if (userPrefs.isLoggedIn) {
            authItem.title = getString(R.string.log_out)
            authItem.setIcon(R.drawable.door_open_24dp_ffffff_fill1_wght400_grad0_opsz24)
        } else {
            authItem.title = getString(R.string.login)
            authItem.setIcon(R.drawable.person_24dp_ffffff_fill1_wght400_grad0_opsz24)
        }

        // 2) NEW: only show the “edit” button once there’s actually a saved profile
        val editItem = menu.findItem(R.id.action_edit)
        // e.g. if you use `userPrefs.userName` to detect “profile created”
        val hasProfile = !userPrefs.userName.isNullOrBlank()
        editItem.isVisible = hasProfile

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                // Pass edit-mode = true
                showCreateProfileSheet(isEdit = true)
                true
            }
            R.id.action_auth -> {
                if (userPrefs.isLoggedIn) {
                    showLogoutDialog()
                } else {
                    // For “login” we’re really “add” mode
                    showCreateProfileSheet(isEdit = false)
                }
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_log_out)
            .setMessage(R.string.confirm_log_out)
            .setPositiveButton(R.string.yes) { _, _ ->
                userPrefs.clear()
                invalidateOptionsMenu()
                loadAndDisplayProfile()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    override fun onResume() {
        super.onResume()
        // re-read all the prefs and update UI
        loadAndDisplayProfile()
        invalidateOptionsMenu()
    }

    private fun showCreateProfileSheet(isEdit: Boolean = false, contact: UserContactRow? = null) {
        CreateProfileBottomSheetFragment().apply {
            arguments = bundleOf(
                "isEdit"  to isEdit,
                "contact" to contact   // if editing, pass in the row
            )
            onProfileSavedListener = object : CreateProfileBottomSheetFragment.OnProfileSavedListener {
                override fun onProfileSaved() {
                    invalidateOptionsMenu()
                    loadAndDisplayProfile()
                }
            }
        }.show(supportFragmentManager, "CreateProfileSheet")
    }


    private fun toast(resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }
}
