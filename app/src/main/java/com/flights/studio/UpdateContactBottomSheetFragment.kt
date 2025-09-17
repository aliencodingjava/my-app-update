package com.flights.studio

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.flights.studio.CountryUtils.getCountryCodeAndFlag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class UpdateContactBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var contact: AllContact
    private var onSave: ((AllContact) -> Unit)? = null

    companion object {
        fun newInstance(contact: AllContact, onSave: (AllContact) -> Unit)
                = UpdateContactBottomSheetFragment().apply {
            this.contact = contact
            this.onSave = onSave
        }

        private const val FLAG_SCALE = 0.85f    // % of EditText line height
        private const val FLAG_MIN_DP = 24
        private const val FLAG_MAX_DP = 28
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()


    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var birthdayEditText: EditText
    private lateinit var saveButton: TextView
    private lateinit var cancelButton: TextView
    private lateinit var photoImageView: ImageView
    private lateinit var flagEnd: ImageView

    private var selectedPhotoUri: Uri? = null
    private var lastFlag: String? = null
    private var lastFlagSizePx: Int = 0




    // Photo picker using ActivityResultContracts
    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val initialsTextView = view?.findViewById<TextView>(R.id.iconInitials)

        if (uri != null) {
            val savedPath = saveImageToInternalStorage(requireContext(), uri)

            if (savedPath == null) {
                // ❌ The image was too large or failed to save
                Snackbar.make(requireView(), "Image too large. Please choose a smaller one.", Snackbar.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            selectedPhotoUri = Uri.fromFile(File(savedPath))

            // ✅ Use Glide with size override to prevent memory crash
            Glide.with(this)
                .load(selectedPhotoUri)
                .override(512, 512) // or 256 for low-end devices
                .centerCrop()
                .into(photoImageView)

            photoImageView.visibility = View.VISIBLE
            initialsTextView?.visibility = View.GONE
        } else {
            Snackbar.make(requireView(), "No photo selected", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.isDraggable = false                      // Prevent dragging
                behavior.state = BottomSheetBehavior.STATE_EXPANDED // Fully expanded
                behavior.skipCollapsed = true                    // Skip collapsed state
                isCancelable = false

                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT // ✅ FULL SCREEN
            }
        }

        dialog.setCancelable(false) // Prevent outside-tap dismiss
        return dialog
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_update_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameEditText = view.findViewById(R.id.edit_name)
        phoneEditText = view.findViewById(R.id.edit_phone)
        emailEditText = view.findViewById(R.id.edit_email)
        addressEditText = view.findViewById(R.id.edit_address)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        photoImageView = view.findViewById(R.id.iconImage)
        val calendarIcon: ImageView = view.findViewById(R.id.calendarIcon)
        birthdayEditText = view.findViewById(R.id.edit_birthday)
        val initialsTextView: TextView = view.findViewById(R.id.iconInitials)
        flagEnd        = view.findViewById(R.id.flag_end)



        flagEnd.visibility = View.GONE
        flagEnd.adjustViewBounds = true
        phoneEditText.textDirection = View.TEXT_DIRECTION_LTR

        phoneEditText.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val (_, flag) = getCountryCodeAndFlag(
                phoneEditText.text?.toString().orEmpty()
            )
            updatePhoneFlagEnd(flag)
        }
        phoneEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString()?.trim().orEmpty()
                val (_, flag) = getCountryCodeAndFlag(phone)
                updatePhoneFlagEnd(flag)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        initialsTextView.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (selectedPhotoUri == null && contact.photoUri.isNullOrEmpty()) {
                    val parts = s?.toString()?.trim()?.split(" ").orEmpty()
                    val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                    val displayInitials = initials.ifEmpty { "?" }
                    initialsTextView.text = displayInitials
                    initialsTextView.visibility = View.VISIBLE
                    photoImageView.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select birthday")
            .build()

        birthdayEditText.setOnClickListener {
            datePicker.show(parentFragmentManager, "BIRTHDAY_PICKER")
        }

        datePicker.addOnPositiveButtonClickListener { selection: Long ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            birthdayEditText.setText(formatter.format(calendar.time))
            calendarIcon.visibility = View.VISIBLE
        }

        // ✅ Restore green icon if reminder already exists
        val prefs =
            requireContext().getSharedPreferences("birthday_reminders", Context.MODE_PRIVATE)
        if (prefs.getBoolean("${contact.id}_birthday_set", false)) {
            calendarIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
        }


        // ✅ Watch birthday edit changes — clear icon + SharedPrefs if birthday is removed
        birthdayEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    calendarIcon.visibility = View.GONE
                    calendarIcon.clearColorFilter()
                    val prefs = requireContext().getSharedPreferences("birthday_reminders", Context.MODE_PRIVATE)
                    prefs.edit { remove("${contact.id}_birthday_set") }

                } else {
                    calendarIcon.visibility = View.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        populateFields(photoImageView)

        calendarIcon.visibility = if (birthdayEditText.text.isNullOrBlank()) View.GONE else View.VISIBLE
        calendarIcon.isClickable = true
        calendarIcon.isEnabled = true

        calendarIcon.setOnClickListener {
            val birthdayText = birthdayEditText.text.toString()
            if (birthdayText.isNotEmpty()) {
                val parsedDate = tryParseDate(birthdayText)
                parsedDate?.let { date ->
                    val cal = Calendar.getInstance().apply { time = date }
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = android.provider.CalendarContract.Events.CONTENT_URI
                        putExtra(android.provider.CalendarContract.Events.TITLE, "Birthday: ${nameEditText.text}")
                        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Birthday reminder")
                        putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
                        putExtra(android.provider.CalendarContract.Events.ALL_DAY, true)
                        putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=YEARLY")
                    }
                    try {
                        startActivity(intent)
                        calendarIcon.apply {
                            setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
                            isEnabled = false
                            isClickable = false
                        }

                        val prefs = requireContext().getSharedPreferences("birthday_reminders", Context.MODE_PRIVATE)
                        prefs.edit { putBoolean("${contact.id}_birthday_set", true) }


                    } catch (_: Exception) {
                        Snackbar.make(requireView(), "No calendar app found", Snackbar.LENGTH_SHORT).show()
                    }
                } ?: Snackbar.make(requireView(), "Unrecognized date format", Snackbar.LENGTH_SHORT).show()
            }
        }

        photoImageView.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()
            val birthday = birthdayEditText.text.toString()

            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(view, "Name and Phone number are required!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPhoneNumber(phone)) {
                Snackbar.make(view, "Enter a valid phone number.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalPhotoUri = selectedPhotoUri?.let {
                saveImageToInternalStorage(requireContext(), it)
            } ?: contact.photoUri

            val (regionCode, flag) = getCountryCodeAndFlag(phone)

            val updatedContact = contact.copy(
                name = name,
                phone = phone,
                email = email,
                address = address,
                photoUri = finalPhotoUri,
                birthday = birthday,
                flag = flag,
                regionCode = regionCode
            )

            onSave?.invoke(updatedContact)
            dismiss()
        }

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
        }
        photoImageView.background = drawable
        photoImageView.clipToOutline = true
    }


    private fun getEmojiDrawable(ctx: Context, emoji: String, sizePx: Int): Drawable? = try {
        val tv = TextView(ctx).apply {
            text = emoji
            includeFontPadding = false
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.82f) // small inset to avoid clipping
            setTextColor(Color.BLACK)
            measure(
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, sizePx, sizePx)
        }
        val bmp = createBitmap(sizePx, sizePx)
        Canvas(bmp).apply { tv.draw(this) }
        bmp.toDrawable(ctx.resources).apply { setBounds(0, 0, sizePx, sizePx) }
    } catch (_: Exception) { null }

    /** Renders the flag emoji at the end of the phone EditText, sized to the line height. */
    private fun updatePhoneFlagEnd(flag: String?) {
        if (!isAdded) return

        if (flag.isNullOrEmpty()) {
            flagEnd.visibility = View.GONE
            lastFlag = null
            lastFlagSizePx = 0
            return
        }

        phoneEditText.post {
            val lh = phoneEditText.lineHeight
            val sizePx = ((lh * FLAG_SCALE).toInt())
                .coerceIn(dp(FLAG_MIN_DP), dp(FLAG_MAX_DP))

            if (flag != lastFlag || sizePx != lastFlagSizePx) {
                // Keep the ImageView square and the right size
                (flagEnd.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    if (width != sizePx || height != sizePx) {
                        width = sizePx
                        height = sizePx
                        flagEnd.layoutParams = this
                    }
                }

                getEmojiDrawable(requireContext(), flag, sizePx)?.let {
                    flagEnd.setImageDrawable(it)
                    flagEnd.visibility = View.VISIBLE
                } ?: run { flagEnd.visibility = View.GONE }

                // Ensure text doesn't overlap the flag
                val neededEndPad = sizePx + dp(12)
                if (phoneEditText.paddingEnd < neededEndPad) {
                    phoneEditText.setPaddingRelative(
                        phoneEditText.paddingStart,
                        phoneEditText.paddingTop,
                        neededEndPad,
                        phoneEditText.paddingBottom
                    )
                }

                lastFlag = flag
                lastFlagSizePx = sizePx
            }
        }
    }

    private fun populateFields(photoImageView: ImageView) {
        nameEditText.setText(contact.name)
        phoneEditText.setText(contact.phone)
        emailEditText.setText(contact.email)
        addressEditText.setText(contact.address)

        // Show flag for prefilled phone
        val (_, initFlag) = getCountryCodeAndFlag(contact.phone)
        updatePhoneFlagEnd(initFlag)

        val initialsTextView: TextView? = view?.findViewById(R.id.iconInitials)
        val name = contact.name.trim()
        val parts = name.split(" ")
        val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        val displayInitials = initials.ifEmpty { "?" }

        val birthdayEditText: EditText = view?.findViewById(R.id.edit_birthday) ?: return
        birthdayEditText.setText(contact.birthday)

        val photoUriToLoad: Uri? = selectedPhotoUri ?: contact.photoUri?.toUri()
        if (photoUriToLoad != null) {
            photoImageView.setImageURI(photoUriToLoad)
            photoImageView.visibility = View.VISIBLE
            initialsTextView?.visibility = View.GONE
        } else {
            initialsTextView?.text = displayInitials
            initialsTextView?.visibility = View.VISIBLE
            photoImageView.setImageDrawable(null)
            photoImageView.visibility = View.GONE
        }

        val color = contact.color
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        initialsTextView?.background = bg
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = Regex("^\\+?[0-9. ()-]{7,15}$")
        return phone.matches(phoneRegex)
    }

    /** Save the selected image to internal storage permanently. */
    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        val fileName = "contact_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val maxDim = 1000
            val scale = minOf(
                options.outWidth.toFloat() / maxDim,
                options.outHeight.toFloat() / maxDim
            ).coerceAtLeast(1f).toInt()

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            val downsampledBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            val rotatedBitmap = correctImageOrientation(context, uri, downsampledBitmap)

            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            rotatedBitmap.recycle()
            if (rotatedBitmap != downsampledBitmap) downsampledBitmap.recycle()

            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun correctImageOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = androidx.exifinterface.media.ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )

            val m = android.graphics.Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun tryParseDate(dateStr: String): java.util.Date? {
        val formats = listOf("MM/dd/yyyy", "yyyy-MM-dd", "dd/MM/yyyy")
        for (f in formats) {
            try { return SimpleDateFormat(f, Locale.getDefault()).parse(dateStr) } catch (_: Exception) {}
        }
        return null
    }



}
