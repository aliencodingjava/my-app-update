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
import android.util.Log
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID


class AddContactBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val FLAG_SCALE = 0.85f  // % of EditText line height
        private const val FLAG_MIN_DP = 24
        private const val FLAG_MAX_DP = 28
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private lateinit var flagEnd: ImageView
    private var lastFlag: String? = null
    private var lastFlagSizePx: Int = 0
    private var selectedPhotoPath: String? = null


    private var listener: AddContactListener? = null
    private var selectedPhotoUri: Uri? = null

    private val predefinedColors = listOf(
        "#FFBB86FC".toColorInt(), "#FF6200EE".toColorInt(), "#FF03DAC5".toColorInt(),
        "#FF018786".toColorInt(), "#FFB00020".toColorInt(), "#6495ED".toColorInt(),
        "#F08080".toColorInt(), "#FF69B4".toColorInt(), "#A9A9A9".toColorInt(),
        "#4682B4".toColorInt(), "#FFB6C1".toColorInt(), "#FF6347".toColorInt(),
        "#32CD32".toColorInt(), "#FFA500".toColorInt()
    )

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            if (uri == null) {
                Snackbar.make(requireView(), "No photo selected", Snackbar.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val savedPath = saveImageToInternalStorage(requireContext(), uri)
            if (savedPath == null) {
                Snackbar.make(requireView(), "Image too large. Please choose a smaller one.", Snackbar.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // ✅ keep local path for DB
            selectedPhotoPath = savedPath

            // ✅ local uri for quick checks if you want
            selectedPhotoUri = Uri.fromFile(File(savedPath))

            val photoImageView = view?.findViewById<ImageView>(R.id.iconImage)
            val initialsTextView = view?.findViewById<TextView>(R.id.iconInitials)

            photoImageView?.let {
                Glide.with(this)
                    .load(File(savedPath))
                    .override(512, 512)
                    .centerCrop()
                    .into(it)
                it.visibility = View.VISIBLE
            }

            initialsTextView?.visibility = View.GONE
        }


    interface AddContactListener {
        fun onContactAdded(contact: AllContact)
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



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText: EditText = view.findViewById(R.id.edit_name)
        val phoneEditText: EditText = view.findViewById(R.id.edit_phone)
        flagEnd = view.findViewById(R.id.flag_end)
        flagEnd.visibility = View.GONE
        flagEnd.adjustViewBounds = true
        phoneEditText.textDirection = View.TEXT_DIRECTION_LTR

        // Recompute flag size once layout is ready
        phoneEditText.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val (_, flag) = getCountryCodeAndFlag(phoneEditText.text?.toString().orEmpty())
            updatePhoneFlagEnd(flag, phoneEditText)
        }

        val emailEditText: EditText = view.findViewById(R.id.edit_email)
        val addressEditText: EditText = view.findViewById(R.id.edit_address)
        val saveButton: TextView = view.findViewById(R.id.saveButton)
        val cancelButton: TextView = view.findViewById(R.id.cancelButton)
        val birthdayEditText: EditText = view.findViewById(R.id.edit_birthday)
        val photoImageView = view.findViewById<ImageView>(R.id.iconImage)
        val initialsTextView = view.findViewById<TextView>(R.id.iconInitials)

        populateFields(photoImageView)



        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (selectedPhotoUri == null) {
                    val initials = s?.toString()
                        ?.trim()
                        ?.split("\\s+".toRegex())
                        ?.take(2)
                        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        ?.joinToString("")
                        .orEmpty()

                    initialsTextView.text = initials.ifEmpty { "?" }
                    initialsTextView.visibility = View.VISIBLE
                    photoImageView.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Update flag as user types
        phoneEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString()?.trim().orEmpty()
                val (_, flag) = getCountryCodeAndFlag(phone)
                updatePhoneFlagEnd(flag, phoneEditText)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select birthday")
            .build()

        // 🧠 Move the listener BELOW after building the datePicker
        birthdayEditText.setOnClickListener {
            datePicker.show(parentFragmentManager, "BIRTHDAY_PICKER")
        }

        val calendarIcon: ImageView = view.findViewById(R.id.calendarIcon)
        calendarIcon.visibility = View.GONE

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }

            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val birthdayText = formatter.format(calendar.time)
            birthdayEditText.setText(birthdayText)

            calendarIcon.visibility = View.VISIBLE

            calendarIcon.setOnClickListener {

                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = android.provider.CalendarContract.Events.CONTENT_URI
                    putExtra(android.provider.CalendarContract.Events.TITLE, "Birthday: ${nameEditText.text}")
                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Birthday reminder")
                    putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.timeInMillis)
                    putExtra(android.provider.CalendarContract.Events.ALL_DAY, true)
                    putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=YEARLY")
                }

                try {
                    startActivity(intent)
                    calendarIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green)) // or another color
                } catch (_: Exception) {
                    Snackbar.make(view, "No calendar app found", Snackbar.LENGTH_SHORT).show()
                }
            }
        }



        val photoSelectorArea: View = view.findViewById(R.id.photoSelectorArea)
        photoSelectorArea.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }


        cancelButton.setOnClickListener { dismiss() }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val email = emailEditText.text.toString()
            val address = addressEditText.text.toString()
            val birthday = birthdayEditText.text.toString()

            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(view, "Name and Phone number are required!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidPhoneNumber(phone)) {
                Snackbar.make(view, "Enter a valid phone number.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val randomColor = predefinedColors.randomOrNull() ?: Color.LTGRAY
            val (regionCode, flag) = getCountryCodeAndFlag(phone)

            val newContact = AllContact(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                email = email,
                address = address,
                color = randomColor,
                photoUri = selectedPhotoPath, // ✅ local file path (or null)
                birthday = birthday,
                flag = flag,
                regionCode = regionCode
            )

            listener?.onContactAdded(newContact)
            dismiss()
        }

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
        }
        photoImageView.background = drawable
        photoImageView.clipToOutline = true
    }

    /** Renders the flag emoji at the end of the phone EditText, sized to the line height. */
    private fun updatePhoneFlagEnd(flag: String?, phoneEditText: EditText) {
        if (!isAdded) return

        if (flag.isNullOrEmpty()) {
            flagEnd.visibility = View.GONE
            lastFlag = null
            lastFlagSizePx = 0
            return
        }

        phoneEditText.post {
            val lh = phoneEditText.lineHeight
            val sizePx = ((lh * FLAG_SCALE).toInt()).coerceIn(dp(FLAG_MIN_DP), dp(FLAG_MAX_DP))

            if (flag != lastFlag || sizePx != lastFlagSizePx) {
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

                // Ensure text doesn’t overlap flag
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



    private fun getEmojiDrawable(ctx: Context, emoji: String, sizePx: Int): Drawable? = try {
        val tv = TextView(ctx).apply {
            text = emoji
            includeFontPadding = false
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.82f) // tweak fill: 0.75..0.9
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





    fun setListener(listener: AddContactListener) {
        this.listener = listener
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = Regex("^\\+?[0-9. ()-]{7,15}$")
        return phone.matches(phoneRegex)
    }

    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        val fileName = "contact_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)

        return try {
            // STEP 1: Get image dimensions without decoding full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val maxDim = 1000
            val (originalWidth, originalHeight) = options.outWidth to options.outHeight

            // STEP 2: Calculate sample size to downscale during decode
            val scale = minOf(
                originalWidth.toFloat() / maxDim,
                originalHeight.toFloat() / maxDim
            ).coerceAtLeast(1f)

            val inSample = scale.toInt()

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSample
            }


            // STEP 3: Decode downscaled bitmap directly
            val downsampledBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            // STEP 4: Fix orientation
            val rotatedBitmap = correctImageOrientation(context, uri, downsampledBitmap)

            // STEP 5: Compress & save
            FileOutputStream(file).use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            rotatedBitmap.recycle()
            if (rotatedBitmap != downsampledBitmap) downsampledBitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
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

            val rotationMatrix = android.graphics.Matrix()

            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotationMatrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotationMatrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotationMatrix.postRotate(270f)
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
        } catch (_: Exception) {
            bitmap // fallback, return original
        }
    }



    private fun populateFields(photoImageView: ImageView)
    {
        val photoUriToLoad: Uri? = selectedPhotoUri
        val initialsTextView: TextView? = view?.findViewById(R.id.iconInitials)

        if (photoUriToLoad != null) {
            photoImageView.setImageURI(photoUriToLoad)
            photoImageView.visibility = View.VISIBLE
            initialsTextView?.visibility = View.GONE
        } else {
            val nameEditText: EditText? = view?.findViewById(R.id.edit_name)
            val name = nameEditText?.text?.toString()?.trim().orEmpty()
            val parts = name.split(" ")
            val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            val displayInitials = initials.ifEmpty { "?" }
            initialsTextView?.text = displayInitials

            initialsTextView?.visibility = View.VISIBLE
            photoImageView.setImageDrawable(null)
            photoImageView.visibility = View.GONE

            // 🔽 Add this block here 🔽
            val randomColor = predefinedColors.randomOrNull() ?: Color.LTGRAY
            val initialsBackground = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(randomColor)
            }
            initialsTextView?.background = initialsBackground
        }
    }

    fun getCountryCodeAndFlag(phone: String): Pair<String?, String?> {
        val phoneUtil = PhoneNumberUtil.getInstance()

        // Clean input to keep only digits and optional plus
        val cleaned = phone.replace("[^\\d+]".toRegex(), "")

        // Normalize phone number with likely country format
        val normalized = when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("373") -> "+$cleaned"  // Moldova
            cleaned.startsWith("44") -> "+$cleaned"   // UK
            cleaned.length == 10 -> "+1$cleaned"      // Assume US if 10 digits
            cleaned.length == 11 && cleaned.startsWith("1") -> "+$cleaned"
            else -> "+$cleaned"                       // Default fallback
        }

        return try {
            val parsed = phoneUtil.parse(normalized, null)
            val regionCode = phoneUtil.getRegionCodeForNumber(parsed)
            val flag = regionCode?.let { getFlagEmoji(it) }
            Log.d("AddContactBottomSheet", "Parsed Region: $regionCode, Flag: $flag")
            regionCode to flag
        } catch (e: NumberParseException) {
            e.printStackTrace()
            null to null
        }
    }

    fun getFlagEmoji(regionCode: String): String {
        return regionCode.uppercase().map {
            Character.toChars(0x1F1E6 + (it.code - 'A'.code)).concatToString()
        }.joinToString("")
    }

}
