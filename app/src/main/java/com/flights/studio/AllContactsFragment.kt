package com.flights.studio

import android.content.Context
import android.content.Intent
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
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.flights.studio.CountryUtils.getCountryCodeAndFlag
import com.flights.studio.databinding.FragmentAllContactsBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import androidx.core.net.toUri

class AllContactsFragment : Fragment() {
    private var isFirstImport = true
    private var hasVibratedOverThreshold = false
    private var wasEmptyTextShown = false


    companion object {
        private const val REQUEST_CONTACTS_PERMISSION = 1001
        private const val CONTACT_SWIPE_ARM_DELAY_MS = 600L
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
    private val visibleContacts = mutableStateListOf<AllContact>()
    private val contactPalettes = mutableStateMapOf<String, ContactsAdapter.ColorPalette>()
    private var topSearchQuery by mutableStateOf("")
    private var currentFilterQuery = ""
    private var welcomeOverlayVisible by mutableStateOf(false)
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var snackbar: Snackbar? = null
    private var snackbarTextView: TextView? = null
    // Lists to track deleted contacts and their original positions (for Undo)
    private var deletedContacts = mutableListOf<AllContact>()
    private var deletedPositions = mutableListOf<Int>()
    private var snackbarHandler = Handler(Looper.getMainLooper())
    private var deletionFinalized = false
    private var floatingSearchVisible = false
    private var contactSwipeStartedOnCard = false
    private var contactSwipeArmed = false
    private var contactSwipeDownX = 0f
    private var contactSwipeDownY = 0f
    private var contactSwipeArmRunnable: Runnable? = null
    private var alphabeticalMode = false
    private var contactsAddFabVisible = true


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

        setupWelcomeOverlay()

        sharedPreferences = requireContext().getSharedPreferences("contacts_data", Context.MODE_PRIVATE)
        contacts.clear()
        contacts.addAll(readContactsFromSharedPreferences())

        updateVisibleContacts(contacts)
        setupRecyclerView()
        updateAlphabetIndex()
        updateContactCount()
        updateWelcomeAndListState()


    }

    private fun setupWelcomeOverlay() {
        binding.contactsComposeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.contactsComposeView.setContent {
            FlightsModernTheme {
                ContactsWelcomeOnboardingOverlay(
                    visible = welcomeOverlayVisible,
                    onContinue = ::showAddContactBottomSheet,
                    onSecondary = null
                )
            }
        }
    }

    private fun updateWelcomeAndListState() {
        val showWelcome = contacts.isEmpty() && currentFilterQuery.isBlank()
        welcomeOverlayVisible = showWelcome
        binding.contactsComposeView.visibility = if (showWelcome) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (showWelcome) View.GONE else View.VISIBLE
    }

    private fun updateContactCount(countOverride: Int? = null) {
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

        (activity as? MainActivity)?.updateContactsChromeCount(visibleCount)
    }

    fun filterContacts(
        query: String?,
        syncTopSearch: Boolean = true,
        keepFloatingSearchActive: Boolean = false
    ) {
        val searchQuery = query?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        currentFilterQuery = query.orEmpty()
        val filteredList = if (searchQuery.isNotEmpty()) {
            contacts.filter { it.name.lowercase(Locale.getDefault()).contains(searchQuery) }
        } else {
            contacts.toList()
        }.let { list ->
            if (alphabeticalMode) list.sortedBy { it.name.lowercase(Locale.getDefault()) } else list
        }

        if (syncTopSearch) {
            topSearchQuery = query.orEmpty()
        }
        updateRecyclerTopPaddingForSearch(searchQuery.isNotEmpty())
        updateVisibleContacts(filteredList)
        if (searchQuery.isNotEmpty()) {
            binding.recyclerView.post {
                binding.recyclerView.smoothScrollToPosition(0)
            }
        }
        updateContactCount(filteredList.size)
        updateWelcomeAndListState()
        if (keepFloatingSearchActive && searchQuery.isNotEmpty()) {
            floatingSearchVisible = true
            (activity as? MainActivity)?.updateContactsFloatingSearchVisible(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupContactsCompose() {
        updateVisibleContacts(contacts)
        binding.contactsComposeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.contactsComposeView.setContent {
            FlightsModernTheme {
                ContactsComposeList(
                    contacts = visibleContacts,
                    topSearchQuery = topSearchQuery,
                    palettes = contactPalettes,
                    onFloatingSearchVisibleChange = ::setContactsFloatingSearchVisible,
                    onEditContact = ::showUpdateContactBottomSheet,
                    onDeleteContact = { confirmDeleteContact(it) },
                    onSwipeDeleteContact = ::swipeDeleteContact,
                    onPaletteClick = ::showPalettePicker,
                    onCallContact = ::callContact,
                    onOpenChat = { contact ->
                        // Removed
                    },
                    onAddContact = ::showAddContactBottomSheet,
                    onImportContacts = ::showImportConfirmationDialog
                )
            }
        }
    }

    private fun updateVisibleContacts(newContacts: List<AllContact>) {
        visibleContacts.clear()
        visibleContacts.addAll(newContacts)
        refreshPaletteState(newContacts)
        if (::contactsAdapter.isInitialized) {
            contactsAdapter.setAlphabeticalMode(alphabeticalMode)
            contactsAdapter.setRecentTextSuppressed(currentFilterQuery.isNotBlank() || floatingSearchVisible)
            contactsAdapter.setSearchQuery(currentFilterQuery)
            contactsAdapter.updateData(newContacts)
        }
        updateAlphabetIndex()
        if (_binding != null) {
            updateWelcomeAndListState()
        }
    }

    override fun onResume() {
        super.onResume()
        if (syncIncomingSmsForContacts()) {
            refreshVisibleContacts()
        }
    }

    private fun updateRecyclerTopPaddingForSearch(searching: Boolean) {
        if (_binding == null) return
        val density = resources.displayMetrics.density
        val targetTop = ((if (searching) 150 else 96) * density).toInt()
        if (binding.recyclerView.paddingTop == targetTop) return
        binding.recyclerView.setPadding(
            binding.recyclerView.paddingLeft,
            targetTop,
            binding.recyclerView.paddingRight,
            binding.recyclerView.paddingBottom
        )
    }

    fun toggleContactsAlphabeticalSort() {
        alphabeticalMode = !alphabeticalMode
        refreshVisibleContacts()
        (activity as? MainActivity)?.updateContactsAlphabeticalMode(alphabeticalMode)
    }

    fun isContactsAlphabeticalMode(): Boolean = alphabeticalMode

    fun prepareContactsSearchOpen() {
        floatingSearchVisible = true
        updateRecyclerTopPaddingForSearch(true)
        if (::contactsAdapter.isInitialized) {
            contactsAdapter.setRecentTextSuppressed(true)
            contactsAdapter.setSearchQuery(currentFilterQuery)
        }
        contactsAddFabVisible = false
        (activity as? MainActivity)?.updateContactsAddFabVisible(false)
        binding.recyclerView.post {
            binding.recyclerView.smoothScrollToPosition(0)
        }
    }

    fun closeContactsSearchUi() {
        currentFilterQuery = ""
        floatingSearchVisible = false
        updateRecyclerTopPaddingForSearch(false)
        if (::contactsAdapter.isInitialized) {
            contactsAdapter.setRecentTextSuppressed(false)
            contactsAdapter.setSearchQuery("")
        }
        refreshVisibleContacts()
        contactsAddFabVisible = true
        (activity as? MainActivity)?.updateContactsAddFabVisible(true)
    }

    private fun updateAlphabetIndex() {
        if (_binding == null || !::contactsAdapter.isInitialized) return
        val letters = if (alphabeticalMode && currentFilterQuery.isBlank()) {
            contactsAdapter.getAvailableSectionLetters()
        } else {
            emptyList()
        }
        val rail = binding.alphabetIndex
        rail.removeAllViews()
        rail.visibility = if (letters.isEmpty()) View.GONE else View.VISIBLE
        if (letters.isEmpty()) return

        val textColor = if ((resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        ) Color.rgb(210, 214, 222) else Color.rgb(88, 92, 100)

        letters.forEach { letter ->
            rail.addView(TextView(requireContext()).apply {
                text = letter
                gravity = android.view.Gravity.CENTER
                includeFontPadding = false
                setTextColor(textColor)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            })
        }

        rail.setOnTouchListener { view, event ->
            if (event.actionMasked != MotionEvent.ACTION_DOWN &&
                event.actionMasked != MotionEvent.ACTION_MOVE
            ) return@setOnTouchListener false

            val index = ((event.y / view.height.coerceAtLeast(1)) * letters.size)
                .toInt()
                .coerceIn(0, letters.lastIndex)
            val adapterPosition = contactsAdapter.adapterPositionForSection(letters[index])
            if (adapterPosition != RecyclerView.NO_POSITION) {
                binding.recyclerView.stopScroll()
                (binding.recyclerView.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(adapterPosition, binding.recyclerView.paddingTop + 4)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            true
        }
    }

    private fun refreshVisibleContacts() {
        filterContacts(
            query = currentFilterQuery,
            syncTopSearch = false,
            keepFloatingSearchActive = currentFilterQuery.isNotBlank()
        )
    }

    private fun setContactsFloatingSearchVisible(visible: Boolean) {
        if (!visible && currentFilterQuery.isNotBlank()) return
        if (visible == floatingSearchVisible) return
        floatingSearchVisible = visible
        (activity as? MainActivity)?.updateContactsFloatingSearchVisible(visible)
    }

    private fun refreshPaletteState(scope: List<AllContact> = contacts) {
        scope.forEach { contact ->
            contactPalettes[contact.id] = getPaletteForContact(contact.id)
        }
    }

    private fun getPaletteForContact(contactId: String): ContactsAdapter.ColorPalette {
        val prefs = requireContext().getSharedPreferences("contact_palettes", Context.MODE_PRIVATE)
        val isDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val fallbackButton = if (isDark) Color.BLACK else Color.WHITE
        return ContactsAdapter.ColorPalette(
            mainColor = prefs.getInt("${contactId}_main", Color.TRANSPARENT),
            overlayColor = prefs.getInt("${contactId}_overlay", Color.TRANSPARENT),
            buttonColor = prefs.getInt("${contactId}_button", fallbackButton)
        )
    }

    private fun showPalettePicker(contact: AllContact) {
        PalettePickerBottomSheet { palette ->
            updateContactPalette(contact, palette)
        }.show(parentFragmentManager, "PalettePicker")
    }

    private fun updateContactPalette(contact: AllContact, palette: ContactsAdapter.ColorPalette) {
        val prefs = requireContext().getSharedPreferences("contact_palettes", Context.MODE_PRIVATE)
        prefs.edit {
            if (palette.mainColor == Color.TRANSPARENT &&
                palette.overlayColor == Color.TRANSPARENT &&
                palette.buttonColor == Color.TRANSPARENT
            ) {
                remove("${contact.id}_main")
                remove("${contact.id}_overlay")
                remove("${contact.id}_button")
            } else {
                putInt("${contact.id}_main", palette.mainColor)
                putInt("${contact.id}_overlay", palette.overlayColor)
                putInt("${contact.id}_button", palette.buttonColor)
            }
        }
        contactPalettes[contact.id] = getPaletteForContact(contact.id)
        if (::contactsAdapter.isInitialized) {
            refreshVisibleContacts()
        }
    }

    private fun callContact(contact: AllContact) {
        if (contact.phone.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_DIAL, "tel:${contact.phone}".toUri()))
        } else {
            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun swipeDeleteContact(contact: AllContact) {
        val masterIndex = contacts.indexOfFirst { it.id == contact.id }
        if (masterIndex == -1) return
        deletedContacts.add(contact)
        deletedPositions.add(masterIndex)
        contacts.removeAt(masterIndex)
        saveContactsToSharedPreferences()
        refreshVisibleContacts()
        updateContactCount()
        snackbarHandler.removeCallbacksAndMessages(null)
        showUndoSnackbar()
    }




    private fun setupRecyclerView() {
        val layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun calculateExtraLayoutSpace(
                state: RecyclerView.State,
                extraLayoutSpace: IntArray
            ) {
                val extra = (resources.displayMetrics.heightPixels * 0.75f).toInt()
                extraLayoutSpace[0] = extra
                extraLayoutSpace[1] = extra
            }
        }.apply {
            initialPrefetchItemCount = 12
        }
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setHasFixedSize(true)
        (binding.recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.recycledViewPool.setMaxRecycledViews(ContactsAdapter.VIEW_TYPE_CONTACT, 24)
        binding.recyclerView.setItemViewCacheSize(18)
        binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        val pageSwipeGutter = (2 * resources.displayMetrics.density).toInt()
        binding.recyclerView.setPadding(
            pageSwipeGutter,
            binding.recyclerView.paddingTop,
            pageSwipeGutter,
            binding.recyclerView.paddingBottom
        )

        contactsAdapter = ContactsAdapter(
            contacts.toMutableList(),
            context = requireContext(),
            onDeleteConfirmed = { contact, position ->
                confirmDeleteContact(contact, position)
            },
            onItemClicked = { showUpdateContactBottomSheet(it) },
            onSelectionChanged = { count ->
                (activity as? MainActivity)?.updateContactsSelectionCount(count)
            },
            onContactOpened = {
                if (currentFilterQuery.isNotBlank() || floatingSearchVisible) {
                    closeContactsSearchUi()
                    (activity as? MainActivity)?.hideContactsKeyboard()
                        ?: (activity as? MainActivity)?.hideContactsKeyboard()
                }
            },
            onRecentContactMenuRequested = { contact, onRemove ->
                (activity as? MainActivity)?.showRecentContactMenu(contact, onRemove)
            }
        )

        binding.recyclerView.adapter = contactsAdapter
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var handledSelectionTap = false
            private val detector = GestureDetector(
                requireContext(),
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        if (!contactsAdapter.isSelectionMode) return false
                        val child = binding.recyclerView.findChildViewUnder(e.x, e.y) ?: return false
                        val position = binding.recyclerView.getChildAdapterPosition(child)
                        if (position == RecyclerView.NO_POSITION) return false
                        contactsAdapter.toggleSelectionAtAdapterPosition(position)
                        handledSelectionTap = true
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        val child = binding.recyclerView.findChildViewUnder(e.x, e.y) ?: return
                        val position = binding.recyclerView.getChildAdapterPosition(child)
                        if (position == RecyclerView.NO_POSITION) return
                        child.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        contactsAdapter.startSelectionAtAdapterPosition(position)
                    }
                }
            )

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                handledSelectionTap = false
                detector.onTouchEvent(e)
                return handledSelectionTap
            }
        })
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!isAdded) return
                val glide = Glide.with(this@AllContactsFragment)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        glide.resumeRequests()
                        val first = layoutManager.findFirstVisibleItemPosition().coerceAtLeast(0)
                        contactsAdapter.preloadAvatarsAround(first, 24)
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        glide.pauseRequests()
                    }
                    else -> {
                        glide.resumeRequests()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateFloatingSearchVisibility(layoutManager)
                updateAddFabVisibility(dy, layoutManager)
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first != RecyclerView.NO_POSITION) {
                    contactsAdapter.preloadAvatarsAround(first + 8, 12)
                }
            }
        })
        binding.recyclerView.post {
            updateFloatingSearchVisibility(layoutManager)
            updateAddFabVisibility(0, layoutManager)
            contactsAdapter.preloadAvatarsAround(0, 32)
        }
    }

    private fun updateAddFabVisibility(dy: Int, layoutManager: LinearLayoutManager) {
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val shouldShow = when {
            currentFilterQuery.isNotBlank() -> false
            firstVisible == RecyclerView.NO_POSITION -> true
            firstVisible <= 0 && dy <= 0 -> true
            dy > 6 -> false
            dy < -2 -> true
            else -> contactsAddFabVisible
        }
        if (shouldShow == contactsAddFabVisible) return
        contactsAddFabVisible = shouldShow
        (activity as? MainActivity)?.updateContactsAddFabVisible(shouldShow)
    }

    private fun updateFloatingSearchVisibility(layoutManager: LinearLayoutManager) {
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val headerView = binding.recyclerView.findViewHolderForAdapterPosition(0)?.itemView
        val threshold = (24 * resources.displayMetrics.density).toInt()
        val showFloatingSearch = firstVisible > 0 ||
            (firstVisible == 0 && headerView != null && headerView.bottom <= threshold)

        if (showFloatingSearch == floatingSearchVisible) return
        floatingSearchVisible = showFloatingSearch

        (activity as? MainActivity)?.updateContactsFloatingSearchVisible(showFloatingSearch)
    }

    private fun setupContactSwipeGestureGate() {
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    contactSwipeStartedOnCard = touchStartedOnContactCard(rv, e.x, e.y)
                    contactSwipeArmed = false
                    contactSwipeDownX = e.x
                    contactSwipeDownY = e.y
                    contactSwipeArmRunnable?.let { rv.removeCallbacks(it) }
                    if (contactSwipeStartedOnCard) {
                        contactSwipeArmRunnable = Runnable {
                            contactSwipeArmed = true
                            rv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }.also { rv.postDelayed(it, CONTACT_SWIPE_ARM_DELAY_MS) }
                    }
                } else if (e.actionMasked == MotionEvent.ACTION_MOVE && !contactSwipeArmed) {
                    val movedEnough = abs(e.x - contactSwipeDownX) > touchSlop ||
                        abs(e.y - contactSwipeDownY) > touchSlop
                    if (movedEnough) {
                        cancelContactSwipeArm(rv)
                    }
                } else if (
                    e.actionMasked == MotionEvent.ACTION_UP ||
                    e.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    cancelContactSwipeArm(rv)
                }
                return false
            }
        })
    }

    private fun cancelContactSwipeArm(recyclerView: RecyclerView = binding.recyclerView) {
        contactSwipeArmRunnable?.let { recyclerView.removeCallbacks(it) }
        contactSwipeArmRunnable = null
        contactSwipeStartedOnCard = false
        contactSwipeArmed = false
    }

    private fun touchStartedOnContactCard(recyclerView: RecyclerView, x: Float, y: Float): Boolean {
        val child = recyclerView.findChildViewUnder(x, y) ?: return false
        val holder = recyclerView.getChildViewHolder(child)
        if (holder.adapterPosition < 0) return false

        val card = child.findViewById<View>(R.id.combined_card) ?: return false
        val cardLeft = child.left + card.left
        val cardRight = child.left + card.right
        val cardTop = child.top + card.top
        val cardBottom = child.top + card.bottom

        return x >= cardLeft && x <= cardRight && y >= cardTop && y <= cardBottom
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
                val adapterPosition = viewHolder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val contactPosition = contactsAdapter.contactIndexForAdapterPosition(adapterPosition)
                    if (contactPosition !in contactsAdapter.getFilteredContacts().indices) return
                    val filteredContact = contactsAdapter.getFilteredContacts()[contactPosition]
                    val masterIndex = contacts.indexOfFirst { it.id == filteredContact.id }
                    if (masterIndex != -1) {
                        deletedContacts.add(filteredContact)
                        deletedPositions.add(masterIndex)
                        contacts.removeAt(masterIndex)
                        saveContactsToSharedPreferences()
                        refreshVisibleContacts()
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
                cancelContactSwipeArm(recyclerView)
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.adapterPosition
                val contactPosition = contactsAdapter.contactIndexForAdapterPosition(position)
                return if (
                    !contactSwipeStartedOnCard ||
                    !contactSwipeArmed ||
                    contactsAdapter.isSelectionMode ||
                    position == RecyclerView.NO_POSITION ||
                    contactPosition !in contactsAdapter.getFilteredContacts().indices ||
                    contactsAdapter.isExpanded(contactPosition)
                )
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
            val snackbarInstance = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
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
            positionUndoSnackbarAboveContactsTabs(snackbarInstance)



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

    private fun positionUndoSnackbarAboveContactsTabs(snackbar: Snackbar) {
        val extraBottom = (92 * resources.displayMetrics.density).toInt()
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams
        if (params is ViewGroup.MarginLayoutParams && params.bottomMargin < extraBottom) {
            params.bottomMargin = extraBottom
            snackbarView.layoutParams = params
        } else {
            snackbarView.translationY = -extraBottom.toFloat()
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
            saveContactsToSharedPreferences()
            refreshVisibleContacts()
            updateContactCount()
            deletedContacts.clear()
            deletedPositions.clear()
        }
    }






    // --- Confirm deletion via dialog (if user selects Delete manually) ---
    private fun confirmDeleteContact(contact: AllContact, position: Int = -1) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                // Finalize deletion immediately
                deleteContact(contact)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // If canceled, refresh the item so it reappears
                if (::contactsAdapter.isInitialized && position != -1) {
                    contactsAdapter.notifyItemChanged(position)
                }
            }
            .show()
    }

    // --- Delete contact immediately and update adapter ---
    private fun deleteContact(contact: AllContact) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts.removeAt(index)
        }
        saveContactsToSharedPreferences()
        refreshVisibleContacts()
        updateContactCount()
    }

    fun deleteSelectedContacts() {
        if (!::contactsAdapter.isInitialized) return
        val selectedContacts = contactsAdapter.getSelectedContacts()
        if (selectedContacts.isEmpty()) return
        val selectedIds = selectedContacts.mapTo(hashSetOf()) { it.id }
        deletedContacts.addAll(selectedContacts)
        deletedPositions.addAll(
            selectedContacts.map { contact ->
                contacts.indexOfFirst { it.id == contact.id }.coerceAtLeast(contacts.size)
            }
        )
        contacts.removeAll { selectedIds.contains(it.id) }
        contactsAdapter.clearSelection()
        saveContactsToSharedPreferences()
        refreshVisibleContacts()
        updateContactCount()
        snackbarHandler.removeCallbacksAndMessages(null)
        showUndoSnackbar()
    }

    fun clearContactSelection() {
        if (::contactsAdapter.isInitialized) {
            contactsAdapter.clearSelection()
        }
    }

    fun selectAllVisibleContacts() {
        if (::contactsAdapter.isInitialized) {
            contactsAdapter.selectAllVisibleContacts()
        }
    }

    private fun syncIncomingSmsForContacts(): Boolean = false

    private fun readContactsFromSharedPreferences(): List<AllContact> {
        val allContactsJson = sharedPreferences.getString("contacts", null)
        if (allContactsJson.isNullOrEmpty()) return emptyList()

        return Gson()
            .fromJson(allContactsJson, Array<AllContact>::class.java)
            .toList()
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }


    private fun saveContactsToSharedPreferences() {
        val json = Gson().toJson(contacts)
        sharedPreferences.edit { putString("contacts", json) }
    }

    fun showAddContactBottomSheet() {
        if (!isAdded || parentFragmentManager.isStateSaved) return
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

    fun openContactDetails(contact: AllContact) {
        showUpdateContactBottomSheet(contact)
    }

    private fun showUpdateContactBottomSheet(contact: AllContact) {
        val bottomSheetFragment = UpdateContactBottomSheetFragment.newInstance(contact) { updatedContact ->
            updateContact(contact, updatedContact)

            val updatedIndex = contacts.indexOfFirst { it.id == updatedContact.id }
            if (updatedIndex != -1) {
                refreshVisibleContacts()
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
            refreshVisibleContacts()
            updateContactCount()

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
        saveContactsToSharedPreferences()
        refreshVisibleContacts()
        updateContactCount() // Call here after updating the contact


    }

    private fun checkPermissions() {
        when {
            ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED -> importContactsFromPhone()
            else -> requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    override fun onDestroyView() {
        contactSwipeArmRunnable?.let { binding.recyclerView.removeCallbacks(it) }
        contactSwipeArmRunnable = null
        floatingSearchVisible = false
        (activity as? MainActivity)?.updateContactsFloatingSearchVisible(false)
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
        var updatedExistingPhotos = false
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
                        photoInputStream?.use { input ->
                            val file = File(requireContext().filesDir, "imported_photo_${contactId}_${UUID.randomUUID()}.jpg")
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                            photoUri = file.absolutePath
                        }

                        if (!phoneNumber.isNullOrEmpty()) {
                            val existingContact = contacts.firstOrNull { it.phone.trim() == phoneNumber.trim() }
                            if (existingContact != null) {
                                val importedPhoto = photoUri?.trim().orEmpty()
                                if (importedPhoto.isNotBlank() && existingContact.photoUri?.trim() != importedPhoto) {
                                    existingContact.photoUri = importedPhoto
                                    updatedExistingPhotos = true
                                }
                            } else {
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
            refreshVisibleContacts()
            updateContactCount()
            isFirstImport = false
            triggerImportAnimation(startIndex, contactList.size)
        } else if (updatedExistingPhotos) {
            saveContactsToSharedPreferences()
            refreshVisibleContacts()
        }
    }

    // Method to animate newly imported contacts
    private fun triggerImportAnimation(startIndex: Int, count: Int) {
        // Compose list rows animate their own placement; no XML holder animation is needed.
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
        if (!isAdded || parentFragmentManager.isStateSaved) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Contacts")

            .setMessage("Do you want to import contacts from your phone?")
            .setPositiveButton("Import") { _, _ ->
                // User clicked "Import" button, proceed with checking permissions
                checkPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}
