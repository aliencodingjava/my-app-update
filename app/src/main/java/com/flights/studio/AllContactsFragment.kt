package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flights.studio.databinding.FragmentAllContactsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

class AllContactsFragment : Fragment() {
    private var isFirstImport = true
    companion object {
        private const val REQUEST_CONTACTS_PERMISSION = 1001
    }

    // Define light colors for contacts
    private val lightColors = listOf(
        Color.parseColor("#6495ED"), // Cornflower Blue
        Color.parseColor("#F08080"), // Light Coral
        Color.parseColor("#FF69B4"), // Hot Pink
        Color.parseColor("#A9A9A9"), // Dark Gray
        Color.parseColor("#4682B4"), // Steel Blue
        Color.parseColor("#FFB6C1"), // Light Pink
        Color.parseColor("#FF6347"), // Light Red
        Color.parseColor("#32CD32"), // Lime Green
        Color.parseColor("#FFA500")  // Orange
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
        savedInstanceState: Bundle?
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
                searchView?.queryHint = "Search contacts..."
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        filterContacts(query)
                        return true
                    }
                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (newText.isNullOrEmpty()) {
                            contactsAdapter.resetData()
                        } else {
                            filterContacts(newText)
                        }
                        return true
                    }
                })
                searchView?.setOnCloseListener {
                    contactsAdapter.updateData(contacts)
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


    private fun updateContactCount() {
        val navContactCount: TextView = requireActivity().findViewById(R.id.nav_contact_count)
        navContactCount.text = String.format(Locale.getDefault(), "%d", contacts.size)
    }


    fun filterContacts(query: String?) {
        val searchQuery = query?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        val filteredList = if (searchQuery.isNotEmpty()) {
            contacts.filter { it.name.lowercase(Locale.getDefault()).contains(searchQuery) }
                .toMutableList()
        } else {
            contacts.toMutableList()
        }

        contactsAdapter.updateData(filteredList)

     }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Retrieve the nav_contact_count TextView.
        // Adjust the call if it's part of the NavigationRail that's part of the fragment's layout.
        val navContactCount: TextView = requireActivity().findViewById(R.id.nav_contact_count)

        contactsAdapter = ContactsAdapter(
            contacts.toMutableList(),
            context = requireContext(),
            onDeleteConfirmed = { contact, position ->
                confirmDeleteContact(contact, position)
            },
            onItemClicked = { showUpdateContactBottomSheet(it) },
            navContactCount = navContactCount  // Pass the TextView here
        )
        binding.recyclerView.adapter = contactsAdapter
    }

    // --- Set up swipe-to-delete behavior ---
    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapterPosition = viewHolder.bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    // Get the contact from the filtered list
                    val filteredContact = contactsAdapter.getFilteredContacts()[adapterPosition]
                    // Find its position in the master list
                    val masterIndex = contacts.indexOfFirst { it.id == filteredContact.id }
                    if (masterIndex != -1) {
                        // Save the contact and its master index for undo purposes
                        deletedContacts.add(filteredContact)
                        deletedPositions.add(masterIndex)

                        // Remove the contact from the master list
                        contacts.removeAt(masterIndex)

                        // Update the adapter to reflect the removal
                        contactsAdapter.updateData(contacts)
                        saveContactsToSharedPreferences()
                        updateContactCount()

                        // Scroll to maintain current position (if needed)
                        binding.recyclerView.post {
                            binding.recyclerView.smoothScrollBy(0, 0)
                        }

                        // Cancel any previous Snackbar callbacks and show the Undo Snackbar
                        snackbarHandler.removeCallbacksAndMessages(null)
                        showUndoSnackbar()
                    }
                }

            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                // Get the background view (which should remain fixed)
                val deleteBackground = itemView.findViewById<View>(R.id.delete_background)
                // Get the foreground view (the one that will be swiped)
                val foregroundView = itemView.findViewById<View>(R.id.combined_card)

                // Update the delete background visibility and alpha based on swipe distance.
                if (dX < 0) {
                    deleteBackground.visibility = View.VISIBLE
                    val width = itemView.width.toFloat()
                    val alpha = minOf(1.0f, abs(dX) / width)
                    deleteBackground.alpha = alpha
                } else {
                    deleteBackground.visibility = View.GONE
                }
                // Instead of calling super.onChildDraw (which would translate the entire itemView),
                // let the default UI util handle only the foreground view.
                getDefaultUIUtil()
                    .onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Clear any translation applied to the foreground view.
                val foregroundView = viewHolder.itemView.findViewById<View>(R.id.combined_card)
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
        contactsAdapter.updateData(contacts.toMutableList())
        saveContactsToSharedPreferences()
        updateContactCount()
    }

    private fun loadContactsFromSharedPreferences() {
        val allContactsJson = sharedPreferences.getString("contacts", null)
        if (!allContactsJson.isNullOrEmpty()) {
            val contactsList = Gson().fromJson(allContactsJson, Array<AllContact>::class.java).toMutableList()
            contacts.clear()
            contacts.addAll(contactsList.sortedBy { it.name.lowercase(Locale.getDefault()) })
            contactsAdapter.updateData(contacts)
            updateContactCount()
        }
    }

    private fun saveContactsToSharedPreferences() {
        val json = Gson().toJson(contacts)
        sharedPreferences.edit().putString("contacts", json).apply()
    }

     fun showAddContactBottomSheet() {
        val bottomSheetFragment = AddContactBottomSheetFragment()
        bottomSheetFragment.setListener(object : AddContactBottomSheetFragment.AddContactListener {
            override fun onContactAdded(contact: AllContact) {
                addContact(contact)
                val countryInfo = bottomSheetFragment.getCountryCodeAndFlag(contact.phone)
                val countryRegion = countryInfo.first
                val countryFlag = countryInfo.second
                val message = if (countryRegion != null && countryFlag != null)
                    "Contact added: ${contact.name}, Country: $countryRegion $countryFlag"
                else
                    "Contact added: ${contact.name}, Country information unavailable."
                showSnackbar(message)
            }
        })
        bottomSheetFragment.show(parentFragmentManager, "AddContactBottomSheet")
    }

    private fun showSnackbar(message: String) {
        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showUpdateContactBottomSheet(contact: AllContact) {
        val bottomSheetFragment = UpdateContactBottomSheetFragment(contact) { updatedContact ->
            updateContact(contact, updatedContact)
            val updatedIndex = contacts.indexOfFirst { it.id == updatedContact.id }
            if (updatedIndex != -1) {
                contactsAdapter.setFilteredContacts(contacts)
                contactsAdapter.notifyItemChanged(updatedIndex)
            }
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
    }

    private fun addContact(contact: AllContact) {
        if (contacts.any { it.phone.trim() == contact.phone.trim() ||
                    it.name.trim().equals(contact.name.trim(), ignoreCase = true) }) {
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
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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
                        requireContext().contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId), null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val phoneColumnIndex = phoneCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val phoneNumber = phoneCursor.getString(phoneColumnIndex)
                                if (!contacts.any { it.phone == phoneNumber }) {
                                    val newContact = AllContact(
                                        id = UUID.randomUUID().toString(),
                                        name = contactName ?: "Unknown",
                                        phone = phoneNumber ?: "Unknown",
                                        email = "",
                                        address = "",
                                        color = lightColors.randomOrNull() ?: Color.LTGRAY,
                                        photoUri = null
                                    )
                                    contactList.add(newContact)
                                }
                            }
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (contactList.isNotEmpty()) {
            val startIndex = contacts.size  // Calculate the start index for new contacts
            contacts.addAll(contactList.sortedBy { it.name.lowercase(Locale.getDefault()) })
            saveContactsToSharedPreferences()
            contactsAdapter.setFilteredContacts(contacts)
            contactsAdapter.notifyItemRangeInserted(startIndex, contactList.size)
            contactsAdapter.updateContactCount()

            isFirstImport = false

            // Call animation function with the correct startIndex
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
                        duration = 1000  // Set your desired duration
                    }
                )
            }
        }
    }

    fun updateContactPhoto(position: Int, uri: Uri) {
        val contactId = contacts[position].id
        val updatedContact = contacts[position].copy(photoUri = uri.toString())
        contacts[position] = updatedContact

        val filteredPosition = contactsAdapter.getFilteredContacts().indexOfFirst { it.id == contactId }
        if (filteredPosition != -1) {
            contactsAdapter.updateContactPhoto(filteredPosition, uri)
        } else {
            contactsAdapter.setFilteredContacts(contacts)
        }
        saveContactsToSharedPreferences()
        binding.recyclerView.post {
            contactsAdapter.notifyItemChanged(filteredPosition)
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
