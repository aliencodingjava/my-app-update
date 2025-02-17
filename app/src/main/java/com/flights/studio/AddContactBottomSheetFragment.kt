package com.flights.studio

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class AddContactBottomSheetFragment : BottomSheetDialogFragment() {

    private var listener: AddContactListener? = null
    private var selectedPhotoUri: Uri? = null // Holds the URI of the selected photo

    // List of predefined colors for contacts
    private val predefinedColors = listOf(
        Color.parseColor("#FFBB86FC"), // Purple
        Color.parseColor("#FF6200EE"), // Deep Purple
        Color.parseColor("#FF03DAC5"), // Teal
        Color.parseColor("#FF018786"), // Dark Teal
        Color.parseColor("#FFB00020"), // Red
        Color.parseColor("#6495ED"),   // Cornflower Blue
        Color.parseColor("#F08080"),   // Light Coral
        Color.parseColor("#FF69B4"),   // Hot Pink
        Color.parseColor("#A9A9A9"),   // Dark Gray
        Color.parseColor("#4682B4"),   // Steel Blue
        Color.parseColor("#FFB6C1"),   // Light Pink
        Color.parseColor("#FF6347"),   // Light Red
        Color.parseColor("#32CD32"),   // Lime Green
        Color.parseColor("#FFA500")    // Orange
    )

    // Photo picker using ActivityResultContracts
    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(requireContext(), uri)
            if (savedPath != null) {
                selectedPhotoUri = Uri.fromFile(File(savedPath)) // ✅ Use permanent URI
                view?.findViewById<ImageView>(R.id.iconImage)?.setImageURI(selectedPhotoUri)
                view?.findViewById<TextView>(R.id.photoPlaceholderText)?.visibility = View.GONE // ✅ Hide Placeholder
            } else {
                Snackbar.make(requireView(), "Failed to save photo", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(requireView(), "No photo selected", Snackbar.LENGTH_SHORT).show()
        }
    }



    interface AddContactListener {
        fun onContactAdded(contact: AllContact)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText: EditText = view.findViewById(R.id.edit_name)
        val phoneEditText: EditText = view.findViewById(R.id.edit_phone)
        val emailEditText: EditText = view.findViewById(R.id.edit_email)
        val addressEditText: EditText = view.findViewById(R.id.edit_address)
        val saveButton: Button = view.findViewById(R.id.saveButton)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val photoImageView: ImageView = view.findViewById(R.id.iconImage)
        val countryFlagText: TextView = view.findViewById(R.id.countryFlagText)
        val photoPlaceholderText: TextView = view.findViewById(R.id.photoPlaceholderText) // ✅ Get placeholder TextView

        populateFields(photoImageView, photoPlaceholderText)

        phoneEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val phone = s.toString().trim()
                if (phone.isNotEmpty()) {
                    updateCountryFlagAndCode(phone, countryFlagText)
                } else {
                    countryFlagText.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        // Open photo picker when the image view is clicked
        photoImageView.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        cancelButton.setOnClickListener {
            dismiss() // Dismiss the BottomSheet
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val email = emailEditText.text.toString()
            val address = addressEditText.text.toString()

            // Validate fields
            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(view, "Name and Phone number are required!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate phone number format
            if (!isValidPhoneNumber(phone)) {
                Snackbar.make(view, "Enter a valid phone number (e.g., +1234567890 or 555-1234).", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate random color and create contact
            val randomColor = predefinedColors.randomOrNull() ?: Color.LTGRAY
            // Save the image to internal storage before saving contact
            val savedImagePath = selectedPhotoUri?.let { saveImageToInternalStorage(requireContext(), it) }
            val newContact = AllContact(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                email = email,
                address = address,
                color = randomColor,
                photoUri = savedImagePath ?: selectedPhotoUri?.toString() // Ensure permanent path is used
            )


            listener?.onContactAdded(newContact) // Notify listener
            dismiss() // Dismiss the BottomSheet
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE  // 🔥 Change from OVAL to RECTANGLE
            setColor(Color.TRANSPARENT) // Transparent background
        }
        photoImageView.background = drawable
        photoImageView.clipToOutline = true

    }

    fun setListener(listener: AddContactListener) {
        this.listener = listener
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = Regex("^\\+?[0-9. ()-]{7,15}\$")
        return phone.matches(phoneRegex)
    }

    /**
     * Save the selected image to internal storage permanently.
     */
    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver

        // Create a file in the app's private storage
        val fileName = "contact_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)

        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream: OutputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath // Return the saved file path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    /**
     * Updates the country flag and code based on the phone number.
     */
    private fun updateCountryFlagAndCode(phone: String, countryFlagText: TextView) {
        val (regionCode, flag) = CountryUtils.getCountryCodeAndFlag(phone)

        if (regionCode != null) {
            val countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(regionCode)
            val countryName = CountryUtils.getCountryName(regionCode)

            // ✅ Ensure correct usage of getString()
            val displayText = requireContext().getString(R.string.country_flag_with_code, flag, countryCode, countryName)

            countryFlagText.text = displayText
            countryFlagText.visibility = View.VISIBLE
        } else {
            countryFlagText.visibility = View.GONE
        }
    }
    private fun populateFields(photoImageView: ImageView, photoPlaceholderText: TextView) {
        val photoUriToLoad: Uri? = selectedPhotoUri

        if (photoUriToLoad != null) {
            photoImageView.setImageURI(photoUriToLoad) // ✅ Show image
            photoPlaceholderText.visibility = View.GONE  // ✅ Hide placeholder text
        } else {
            photoImageView.setImageResource(R.drawable.terminalinside) // ✅ Default image
            photoPlaceholderText.visibility = View.VISIBLE  // ✅ Show placeholder text
        }
    }


    fun getCountryCodeAndFlag(phone: String): Pair<String?, String?> {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        return try {
            val phoneNumber = phoneNumberUtil.parse(phone, "US")
            val countryCode = phoneNumber.countryCode
            val regionCode = phoneNumberUtil.getRegionCodeForCountryCode(countryCode)
            val flag = regionCode?.let { getFlagEmoji(it) }

            // Log to check if we're getting the correct region and flag
            Log.d("AddContactBottomSheet", "Parsed Region: $regionCode, Flag: $flag")

            Pair(regionCode, flag)
        } catch (e: NumberParseException) {
            e.printStackTrace()
            Pair(null, null) // If there's an error, return nulls
        }
    }

    fun getFlagEmoji(regionCode: String): String {
        return regionCode
            .uppercase()
            .map { char -> 0x1F1E6 + (char.code - 'A'.code) }
            .map { codePoint -> String(Character.toChars(codePoint)) }
            .joinToString("")
    }
}