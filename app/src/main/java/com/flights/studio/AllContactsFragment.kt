package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flights.studio.CountryUtils.getCountryCodeAndFlag
import com.flights.studio.databinding.FragmentAllContactsBinding
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

class AllContactsFragment : Fragment() {
    private var isFirstImport = true
    private var hasVibratedOverThreshold = false
    private var wasEmptyTextShown = false
    private var fabBadgeDrawable: BadgeDrawable? = null


    companion object {
        private const val REQUEST_CONTACTS_PERMISSION = 1001
    }

    // Define light colors for contacts
    private val lightColors = listOf(
        "#6495ED".toColorInt(), // Cornflower Blue
        "#F08080".toColorInt(), // Light Coral
        "#FF69B4".toColorInt(), // Hot Pink
        "#A9A9A9".toColorInt(), // Dark Gray
        "#4682B4".toColorInt(), // Steel Blue
        "#FFB6C1".toColorInt(), // Light Pink
        "#FF6347".toColorInt(), // Light Red
        "#32CD32".toColorInt(), // Lime Green
        "#FFA500".toColorInt()  // Orange
    )

    private var _binding: FragmentAllContactsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding should not be null after onCreateView")
    // Master list of contacts
    private val contacts = mutableListOf<AllContact>()
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var snackbar: Snackbar? = null
    private var snackbarTextView: TextView? = null
    // Lists to track deleted contacts and their original positions (for Undo)
    private var deletedContacts = mutableListOf<AllContact>()
    private var deletedPositions = mutableListOf<Int>()
    private var snackbarHandler = Handler(Looper.getMainLooper())
    private var deletionFinalized = false


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            importContactsFromPhone()
        } else {
            Snackbar.make(binding.root, "Permission denied to read contacts.", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAllContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        // Set up menu (with search, add, and import options)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.contact_menu, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as? SearchView
                searchView?.queryHint = ""
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        filterContacts(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (newText.isNullOrEmpty()) {
                            contactsAdapter.resetData()
                            updateContactCount()
                        } else {
                            val filteredList = contacts.filter {
                                it.name.lowercase(Locale.getDefault())
                                    .contains(newText.lowercase(Locale.getDefault()))
                            }
                            contactsAdapter.updateData(filteredList)
                            updateContactCount(filteredList.size, isFiltering = true)
                        }
                        return true
                    }
                })

                searchView?.setOnCloseListener {
                    searchView.setQuery("", false) // Clear the text
                    searchView.clearFocus()        // Remove keyboard focus
                    contactsAdapter.resetData()    // Reuse your smart reset method
                    false
                }


            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.add_contact -> {
                        true
                    }

                    R.id.import_contacts -> {
                        true
                    }

                    R.id.action_search -> true
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        sharedPreferences = requireContext().getSharedPreferences("contacts_data", Context.MODE_PRIVATE)

        setupItemTouchHelper()
        updateContactCount()
        setupRecyclerView()
        loadContactsFromSharedPreferences()


    }
    @OptIn(ExperimentalBadgeUtils::class)
    private fun updateContactCount(countOverride: Int? = null, isFiltering: Boolean = false) {
        val totalCount = contacts.size
        val visibleCount = countOverride ?: totalCount

        val emptyTextView: TextView? = requireActivity().findViewById(R.id.empty_contact_text)

        if (totalCount == 0) {
            emptyTextView?.apply {
                if (!wasEmptyTextShown) {
                    wasEmptyTextShown = true
                    visibility = View.VISIBLE
                    alpha = 0f
                    translationX = -100f
                    animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(900)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
            }
        } else {
            if (wasEmptyTextShown) {
                wasEmptyTextShown = false
                emptyTextView?.animate()
                    ?.alpha(0f)
                    ?.translationX(100f)
                    ?.setDuration(500)
                    ?.setInterpolator(android.view.animation.AccelerateInterpolator())
                    ?.withEndAction {
                        emptyTextView.visibility = View.GONE
                        emptyTextView.translationX = 0f
                    }
                    ?.start()
            } else {
                emptyTextView?.visibility = View.GONE
                emptyTextView?.alpha = 0f
                emptyTextView?.translationX = 0f
            }
        }

        // --- FAB badge like in NotesActivity ---
        val fab = requireActivity().findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.nav_add_contact)
        fab.post {
            if (totalCount > 0) {
                val badge = fabBadgeDrawable ?: BadgeDrawable.create(requireContext()).also {
                    fabBadgeDrawable = it
                }

                badge.isVisible = true
                badge.number = totalCount
                badge.badgeGravity = BadgeDrawable.TOP_END
                badge.backgroundColor = Color.RED
                badge.badgeTextColor = Color.WHITE

                // use 15dp offsets for consistent location across densities
                val d = resources.displayMetrics.density
                badge.horizontalOffset = (12 * d).toInt()
                badge.verticalOffset = (12 * d).toInt()

                BadgeUtils.attachBadgeDrawable(badge, fab)
            } else {
                fabBadgeDrawable?.let {
                    BadgeUtils.detachBadgeDrawable(it, fab)
                    fabBadgeDrawable = null
                }
            }
        }

        // --- Navigation rail count text ---
        val navContactCount: TextView = requireActivity().findViewById(R.id.nav_contact_count)
        navContactCount.text = String.format(Locale.getDefault(), "%d", visibleCount)

        // --- Optional: badge inside NavigationRail menu ---
        val navigationRailView: NavigationRailView = requireActivity().findViewById(R.id.navigation_rail)
        val railBadge = navigationRailView.getOrCreateBadge(R.id.nav_add_contact)
        railBadge.number = visibleCount
        railBadge.isVisible = visibleCount > 0 && isFiltering
    }

    fun filterContacts(query: String?) {
        val searchQuery = query?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        val filteredList = if (searchQuery.isNotEmpty()) {
            contacts.filter { it.name.lowercase(Locale.getDefault()).contains(searchQuery) }
        } else {
            contacts.toList()
        }

        contactsAdapter.updateData(filteredList)
        updateContactCount(filteredList.size, isFiltering = searchQuery.isNotEmpty())
    }




    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val navContactCount: TextView = requireActivity().findViewById(R.id.nav_contact_count)

        contactsAdapter = ContactsAdapter(
            contacts.toMutableList(),
            context = requireContext(),
            onDeleteConfirmed = { contact, position ->
                confirmDeleteContact(contact, position)
            },
            onItemClicked = { showUpdateContactBottomSheet(it) },
            navContactCount = navContactCount
        )

        binding.recyclerView.adapter = contactsAdapter
    }


    // --- Set up swipe-to-delete behavior ---
    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = false


            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapterPosition = viewHolder.bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val filteredContact = contactsAdapter.getFilteredContacts()[adapterPosition]
                    val masterIndex = contacts.indexOfFirst { it.id == filteredContact.id }
                    if (masterIndex != -1) {
                        deletedContacts.add(filteredContact)
                        deletedPositions.add(masterIndex)
                        contacts.removeAt(masterIndex)
                        contactsAdapter.updateData(contacts)
                        saveContactsToSharedPreferences()
                        updateContactCount()
                        binding.recyclerView.post {
                            binding.recyclerView.smoothScrollBy(0, 0)
                        }
                        snackbarHandler.removeCallbacksAndMessages(null)
                        showUndoSnackbar()
                    }
                }
            }


            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
             ) {
                val itemView = viewHolder.itemView
                val deleteBackground = itemView.findViewById<View>(R.id.delete_background)
                val foregroundView = itemView.findViewById<MaterialCardView>(R.id.combined_card)

                // 🔐 Clamp swipe distance to avoid overlap
                val backgroundWidth = deleteBackground.width.takeIf { it > 0 } ?: (itemView.width * 0.25f)
                val maxSwipeDistance = backgroundWidth.toFloat()
                val clampedDx = dX.coerceAtLeast(-1f * maxSwipeDistance)

                if (dX < 0) {
                    deleteBackground.visibility = View.VISIBLE

                    // 🪟 Match vertical position and height
                    deleteBackground.y = foregroundView.y
                    deleteBackground.layoutParams.height = foregroundView.height
                    deleteBackground.requestLayout()

                    // 🧼 Visual integrity
                    deleteBackground.scaleX = 1f
                    deleteBackground.scaleY = 1f
                    deleteBackground.translationZ = 0f
                    deleteBackground.translationZ = 0f
                    deleteBackground.elevation = 0f

                    // 🟦 Maintain rounded corners during swipe
                    foregroundView.clipToOutline = true
                    foregroundView.outlineProvider = ViewOutlineProvider.BACKGROUND

                    // 📊 Alpha feedback
                    val swipeProgress: Float = (abs(clampedDx) / maxSwipeDistance).coerceIn(0f, 1f)
                    foregroundView.alpha = (1f - swipeProgress).coerceAtLeast(0.92f)
                    deleteBackground.alpha = swipeProgress

                    // 🎯 Haptic feedback at threshold
                    if (swipeProgress >= 0.5f && !hasVibratedOverThreshold) {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
                        hasVibratedOverThreshold = true
                    } else if (swipeProgress < 0.5f && hasVibratedOverThreshold) {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE)
                        hasVibratedOverThreshold = false
                    }
                } else {
                    deleteBackground.visibility = View.GONE
                    foregroundView.alpha = 1f
                    hasVibratedOverThreshold = false
                }

                // 🎯 Final drawing with clamped swipe
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, clampedDx, dY, actionState, isCurrentlyActive)
            }



            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val foregroundView = viewHolder.itemView.findViewById<View>(R.id.combined_card)
                foregroundView.alpha = 1f
                getDefaultUIUtil().clearView(foregroundView)
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.bindingAdapterPosition
                return if (position != RecyclerView.NO_POSITION && contactsAdapter.isExpanded(position))
                    0  // Disable swipe when item is expanded
                else
                    super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerView)
    }

    // --- Show Undo Snackbar and restore deleted contacts using specific notifications ---
    private fun finalizeDeletion() {
        // Finalize deletion by clearing the pending deletion lists.
        deletedContacts.clear()
        deletedPositions.clear()
    }

    private fun showUndoSnackbar() {

        // If the Snackbar is not already showing, create it; otherwise, update its message.
        if (snackbar == null) {

            // Create a Snackbar with an empty message and indefinite duration.
            val snackbarInstance = Snackbar.make(binding.recyclerView, "", Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.undo)) {
                    // In case the built-in action is used.
                    restoreDeletedContacts()
                }

            // Inflate the custom layout using the Snackbar's view as parent (do not pass null).
            val parent = snackbarInstance.view as? ViewGroup
            val customView = layoutInflater.inflate(R.layout.custom_snackbar_delete_undo, parent, false)

            // Get references to the views in the custom layout.
            snackbarTextView = customView.findViewById(R.id.snackbar_text)
            val btnUndo = customView.findViewById<Button>(R.id.btn_undo)
            val btnDelete = customView.findViewById<Button>(R.id.btn_delete)

            // Update the message using the string resource with a placeholder.
            snackbarTextView?.text = getString(R.string.snackbar_message, deletedContacts.size)

            // Set up the Undo button.
            btnUndo.setOnClickListener {
                restoreDeletedContacts()
                snackbarInstance.dismiss()
            }
            // Set up the Delete button.
            btnDelete.setOnClickListener {
                deletionFinalized = true  // Mark deletion as finalized.
                finalizeDeletion()
                snackbarInstance.dismiss()
            }

            // Remove all default views from the Snackbar's layout and add your custom view.
            parent?.removeAllViews()
            parent?.addView(customView)



            // When the Snackbar is dismissed, check if deletion was finalized.
            snackbarInstance.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (!deletionFinalized) {
                        // If the Snackbar was dismissed without finalizing deletion,
                        // restore the deleted contacts.
                        restoreDeletedContacts()
                    }
                    // Reset flag and clear pending deletion lists.
                    deletionFinalized = false
                    deletedContacts.clear()
                    deletedPositions.clear()
                    snackbar = null
                    snackbarTextView = null
                    super.onDismissed(transientBottomBar, event)
                }
            })

            snackbar = snackbarInstance
            snackbarInstance.show()
        } else {
            // If the Snackbar is already showing, update its message with the new deletion count.
            snackbarTextView?.text = getString(R.string.snackbar_message, deletedContacts.size)

        }
    }


    private fun restoreDeletedContacts() {
        if (deletedContacts.isNotEmpty() && deletedPositions.isNotEmpty()) {
            // Iterate in reverse order so that reinserting doesn’t affect earlier indices
            for (i in deletedContacts.indices.reversed()) {
                val contact = deletedContacts[i]
                val masterPosition = deletedPositions[i]

                // Insert back into the master list at the stored master index
                if (masterPosition <= contacts.size) {
                    contacts.add(masterPosition, contact)
                } else {
                    contacts.add(contact)
                }
            }
            // Update the adapter with the restored master list
            contactsAdapter.updateData(contacts)
            saveContactsToSharedPreferences()
            updateContactCount()
            deletedContacts.clear()
            deletedPositions.clear()
        }
    }






    // --- Confirm deletion via dialog (if user selects Delete manually) ---
    private fun confirmDeleteContact(contact: AllContact, position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                // Finalize deletion immediately
                deleteContact(contact)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // If cancelled, refresh the item so it reappears
                contactsAdapter.notifyItemChanged(position)
            }
            .show()
    }

    // --- Delete contact immediately and update adapter ---
    private fun deleteContact(contact: AllContact) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts.removeAt(index)
        }
        contactsAdapter.setFilteredContacts(contacts)
        contactsAdapter.notifyItemRemoved(index)
        saveContactsToSharedPreferences()
        updateContactCount()
    }

    private fun loadContactsFromSharedPreferences() {
        val allContactsJson = sharedPreferences.getString("contacts", null)
        if (!allContactsJson.isNullOrEmpty()) {
            val contactsList = Gson().fromJson(allContactsJson, Array<AllContact>::class.java).toMutableList()
            val oldSize = contacts.size
            contacts.clear()
            contacts.addAll(contactsList.sortedBy { it.name.lowercase(Locale.getDefault()) })
            contactsAdapter.setFilteredContacts(contacts)

            if (oldSize == 0) {
                contactsAdapter.notifyItemRangeInserted(0, contacts.size)
            } else {
                contactsAdapter.notifyItemRangeRemoved(0, oldSize)
                contactsAdapter.notifyItemRangeInserted(0, contacts.size)
            }

            updateContactCount()
        }
    }


    private fun saveContactsToSharedPreferences() {
        val json = Gson().toJson(contacts)
        sharedPreferences.edit { putString("contacts", json) }
    }

    fun showAddContactBottomSheet() {
        val bottomSheetFragment = AddContactBottomSheetFragment()
        bottomSheetFragment.setListener(object : AddContactBottomSheetFragment.AddContactListener {
            override fun onContactAdded(contact: AllContact) {
                // Get country info *before* attempting to add
                val (countryRegion, countryFlag) = bottomSheetFragment.getCountryCodeAndFlag(contact.phone)
                addContact(contact, countryRegion, countryFlag)
            }
        })
        bottomSheetFragment.show(parentFragmentManager, "AddContactBottomSheet")
    }



    private fun showSnackbar(message: String) {
        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()

    }

    private fun showUpdateContactBottomSheet(contact: AllContact) {
        val bottomSheetFragment = UpdateContactBottomSheetFragment.newInstance(contact) { updatedContact ->
            updateContact(contact, updatedContact)

            val updatedIndex = contacts.indexOfFirst { it.id == updatedContact.id }
            if (updatedIndex != -1) {
                contactsAdapter.setFilteredContacts(contacts)
                contactsAdapter.notifyItemChanged(updatedIndex)
            }
        }

        bottomSheetFragment.show(parentFragmentManager, "UpdateContactBottomSheet")
    }
    private fun addContact(contact: AllContact, countryRegion: String?, countryFlag: String?) {
        if (contacts.any {
                it.phone.trim() == contact.phone.trim() ||
                        it.name.trim().equals(contact.name.trim(), ignoreCase = true)
            }) {
            Snackbar.make(binding.root, "Phone number or name already exists.", Snackbar.LENGTH_SHORT).show()
        } else {
            val insertIndex = contacts.binarySearchBy(contact.name.lowercase(Locale.getDefault())) {
                it.name.lowercase(Locale.getDefault())
            }
            val position = if (insertIndex < 0) -insertIndex - 1 else insertIndex
            contacts.add(position, contact)
            saveContactsToSharedPreferences()
            contactsAdapter.setFilteredContacts(contacts)
            contactsAdapter.notifyItemInserted(position)
            updateContactCount()
            contactsAdapter.updateContactCount()

            val message = if (countryRegion != null && countryFlag != null)
                "Contact added: ${contact.name}, Country: $countryRegion $countryFlag"
            else
                "Contact added: ${contact.name}, Country information unavailable."
            showSnackbar(message)
        }
    }


    private fun updateContact(oldContact: AllContact, updatedContact: AllContact) {
        val index = contacts.indexOfFirst { it.id == oldContact.id }
        if (index != -1) {
            contacts[index] = updatedContact
        }
        val filteredIndex = contactsAdapter.getFilteredContacts().indexOfFirst { it.id == oldContact.id }
        if (filteredIndex != -1) {
            contactsAdapter.updateContact(filteredIndex, updatedContact)
        } else {
            contactsAdapter.setFilteredContacts(contacts)
        }
        saveContactsToSharedPreferences()
        binding.recyclerView.post {
            contactsAdapter.notifyItemChanged(filteredIndex)
        }
        updateContactCount() // Call here after updating the contact
        contactsAdapter.updateContactCount()


    }

    private fun checkPermissions() {
        when {
            ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED -> importContactsFromPhone()
            else -> requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importContactsFromPhone()
            } else {
                Snackbar.make(binding.root, "Permission denied to read contacts.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun importContactsFromPhone() {
        val contactList = mutableListOf<AllContact>()
        try {
            requireContext().contentResolver.query(
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumnIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    val idColumnIndex = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                    do {
                        val contactId = cursor.getString(idColumnIndex)
                        val contactName = cursor.getString(nameColumnIndex)

                        var phoneNumber: String? = null
                        var email: String? = null
                        var address: String? = null
                        var birthday: String? = null
                        var photoUri: String? = null

                        // 🔹 Get phone number
                        requireContext().contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId), null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val phoneColumnIndex = phoneCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                phoneNumber = phoneCursor.getString(phoneColumnIndex)
                            }
                        }

                        // 🔹 Get email
                        requireContext().contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            "${android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                            arrayOf(contactId), null
                        )?.use { emailCursor ->
                            if (emailCursor.moveToFirst()) {
                                val emailColumnIndex = emailCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS)
                                email = emailCursor.getString(emailColumnIndex)
                            }
                        }

                        // 🔹 Get address
                        requireContext().contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                            null,
                            "${android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                            arrayOf(contactId), null
                        )?.use { addressCursor ->
                            if (addressCursor.moveToFirst()) {
                                val addressColumnIndex = addressCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                                address = addressCursor.getString(addressColumnIndex)
                            }
                        }

                        // 🔹 Get birthday
                        requireContext().contentResolver.query(
                            android.provider.ContactsContract.Data.CONTENT_URI,
                            arrayOf(android.provider.ContactsContract.CommonDataKinds.Event.START_DATE),
                            "${android.provider.ContactsContract.Data.CONTACT_ID} = ? AND ${android.provider.ContactsContract.Data.MIMETYPE} = ? AND ${android.provider.ContactsContract.CommonDataKinds.Event.TYPE} = ?",
                            arrayOf(contactId, android.provider.ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, android.provider.ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()),
                            null
                        )?.use { birthdayCursor ->
                            if (birthdayCursor.moveToFirst()) {
                                val birthdayColumnIndex = birthdayCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Event.START_DATE)
                                birthday = birthdayCursor.getString(birthdayColumnIndex)
                            }
                        }

                        // 🔹 Get photo URI
                        val contactPhotoUri = Uri.withAppendedPath(
                            android.provider.ContactsContract.Contacts.CONTENT_URI,
                            contactId
                        )
                        val photoInputStream = android.provider.ContactsContract.Contacts.openContactPhotoInputStream(
                            requireContext().contentResolver,
                            contactPhotoUri,
                            true
                        )
                        if (photoInputStream != null) {
                            val file = File(requireContext().filesDir, "imported_photo_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { output ->
                                photoInputStream.copyTo(output)
                            }
                            photoUri = file.absolutePath
                        }

                        if (!phoneNumber.isNullOrEmpty()
                            && !contacts.any { it.phone.trim() == phoneNumber.trim() }
                        )
                        {
                            val (regionCode, flag) = getCountryCodeAndFlag(phoneNumber)

                            val newContact = AllContact(
                                id = UUID.randomUUID().toString(),
                                name = contactName ?: "Unknown",
                                phone = phoneNumber,
                                email = email ?: "",
                                address = address ?: "",
                                color = lightColors.randomOrNull() ?: Color.LTGRAY,
                                photoUri = photoUri,
                                birthday = birthday ?: "",
                                flag = flag,
                                regionCode = regionCode
                            )
                            contactList.add(newContact)
                        }

                    } while (cursor.moveToNext())

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (contactList.isNotEmpty()) {
            val startIndex = contacts.size
            contacts.addAll(contactList.sortedBy { it.name.lowercase(Locale.getDefault()) })
            saveContactsToSharedPreferences()
            contactsAdapter.setFilteredContacts(contacts)
            contactsAdapter.notifyItemRangeInserted(startIndex, contactList.size)
            contactsAdapter.updateContactCount()
            updateContactCount()
            isFirstImport = false
            triggerImportAnimation(startIndex, contactList.size)
        }
    }

    // Method to animate newly imported contacts
    private fun triggerImportAnimation(startIndex: Int, count: Int) {
        for (i in startIndex until startIndex + count) {
            binding.recyclerView.post {
                val viewHolder = binding.recyclerView.findViewHolderForAdapterPosition(i) as? ContactsAdapter.ContactViewHolder
                viewHolder?.itemView?.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.item_slide_up).apply {
                        duration = 700  // Set your desired duration
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // If there are pending deleted contacts, restore them automatically.
        if (deletedContacts.isNotEmpty()) {
            restoreDeletedContacts()
            snackbar?.dismiss() // Dismiss the Undo Snackbar
        }
    }



    fun showImportConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Contacts")

            .setMessage("Do you want to import contacts from your phone?")
            .setPositiveButton("Import") { dialog, which ->
                // User clicked "Import" button, proceed with checking permissions
                checkPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}
