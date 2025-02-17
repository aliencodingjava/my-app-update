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
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class UpdateContactBottomSheetFragment(
    private val contact: AllContact,
    private val onSave: (AllContact) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var photoImageView: ImageView
    private var selectedPhotoUri: Uri? = null


    // Photo picker using ActivityResultContracts
    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            photoImageView.setImageURI(uri) // ✅ Instantly show image in BottomSheet

            val savedPath = saveImageToInternalStorage(requireContext(), uri)
            if (savedPath != null) {
                selectedPhotoUri = Uri.fromFile(File(savedPath)) // ✅ Save new URI
            } else {
                Snackbar.make(requireView(), "Failed to save photo", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(requireView(), "No photo selected", Snackbar.LENGTH_SHORT).show()
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        val photoPlaceholderText: TextView = view.findViewById(R.id.photoPlaceholderText)
        val countryFlagText: TextView = view.findViewById(R.id.countryFlagText) // Find country flag TextView




        populateFields(photoImageView, photoPlaceholderText)

        // Open photo picker when the image view is clicked
        photoImageView.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        // Listen for phone number changes
        phoneEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateCountryFlagAndCode(s.toString(), countryFlagText)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        cancelButton.setOnClickListener {
            dismiss()
        }
        saveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(view, "Name and Phone number are required!", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPhoneNumber(phone)) {
                Snackbar.make(view, "Enter a valid phone number.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 Always save a new image if selected, otherwise keep the old one
            val finalPhotoUri = selectedPhotoUri?.let { saveImageToInternalStorage(requireContext(), it) }
                ?: contact.photoUri

            val updatedContact = contact.copy(
                name = name,
                phone = phone,
                email = email,
                address = address,
                photoUri = finalPhotoUri
            )

            onSave(updatedContact) // ✅ Instantly update contact in the list
            dismiss()
        }

        // Set round image background
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT) // Transparent background
        }
        photoImageView.background = drawable
        photoImageView.clipToOutline = true
    }

    private fun populateFields(photoImageView: ImageView, photoPlaceholderText: TextView) {
        nameEditText.setText(contact.name)
        phoneEditText.setText(contact.phone)
        emailEditText.setText(contact.email)
        addressEditText.setText(contact.address)

        val photoUriToLoad: Uri? = selectedPhotoUri ?: contact.photoUri?.let { Uri.parse(it) }

        if (photoUriToLoad != null) {
            photoImageView.setImageURI(photoUriToLoad) // ✅ Show photo
            photoPlaceholderText.visibility = View.GONE  // ✅ Hide text
        } else {
            photoImageView.setImageResource(R.drawable.terminalinside) // ✅ Default image
            photoPlaceholderText.visibility = View.VISIBLE  // ✅ Show text
        }
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
    private fun updateCountryFlagAndCode(phone: String, countryFlagText: TextView) {
        val (regionCode, flag) = getCountryCodeAndFlag(phone)

        if (regionCode != null) {
            val countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(regionCode)
            val countryName = getCountryName(regionCode) // Get country name from mapping

            // ✅ Ensure we use the correct number of arguments
            countryFlagText.text = getString(R.string.country_flag_with_code, flag, countryCode, countryName)
            countryFlagText.visibility = View.VISIBLE
        } else {
            countryFlagText.visibility = View.GONE
        }
    }


    fun getCountryCodeAndFlag(phone: String): Pair<String?, String> {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        return try {
            val phoneNumber = phoneNumberUtil.parse(phone, null) // Auto-detect country
            val regionCode = phoneNumberUtil.getRegionCodeForNumber(phoneNumber) // Get correct country
            val flag = getFlagEmoji(regionCode) // Always returns a value

            Log.d("UpdateContact", "Detected Region: $regionCode, Flag: $flag")

            Pair(regionCode, flag)
        } catch (e: NumberParseException) {
            e.printStackTrace()
            Pair(null, "") // If parsing fails, return empty flag
        }
    }

    fun getFlagEmoji(regionCode: String?): String {
        if (regionCode.isNullOrEmpty()) return "" // Return empty string if region is null

        return regionCode.uppercase()
            .map { char -> 0x1F1E6 + (char.code - 'A'.code) }
            .map { codePoint -> String(Character.toChars(codePoint)) }
            .joinToString("")
    }

    fun getCountryName(regionCode: String): String {
        val countryMap = mapOf(
            "US" to "United States",
            "GB" to "United Kingdom",
            "DE" to "Germany",
            "IN" to "India",
            "JP" to "Japan",
            "FR" to "France",
            "IT" to "Italy",
            "ES" to "Spain",
            "BR" to "Brazil",
            "AU" to "Australia",
            "EG" to "Egypt",
            "AE" to "United Arab Emirates",
            "SA" to "Saudi Arabia",
            "KR" to "South Korea",
            "RU" to "Russia",
            "ZA" to "South Africa",
            "MX" to "Mexico",
            "PK" to "Pakistan",
            "UA" to "Ukraine",
            "CN" to "China",
            "PT" to "Portugal",
            "ID" to "Indonesia",
            "SE" to "Sweden",
            "NO" to "Norway",
            "PL" to "Poland",
            "NL" to "Netherlands",
            "TR" to "Turkey",
            "MD" to "Moldova" // ✅ Added Moldova 🇲🇩
        )
        return countryMap[regionCode] ?: "Unknown Country"
    }




}
