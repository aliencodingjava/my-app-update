package com.flights.studio

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.flights.studio.databinding.ActivityCreateProfileBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.launch
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale


class CreateProfileBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: ActivityCreateProfileBinding
    private lateinit var userPrefsManager: UserPreferencesManager
    private var currentSelectedPhotoUri: Uri? = null
    private var isEditMode = false
    private var editContact: UserContactRow? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            isEditMode = args.getBoolean("isEdit", false)
            // on API>=33:
            editContact = args.getParcelable("contact", UserContactRow::class.java)
            // on older:
            // editContact = args.getParcelable("contact")
        }
    }

    /** ✅ Listener for communicating back to the activity */
    interface OnProfileSavedListener {
        fun onProfileSaved()
    }

    var onProfileSavedListener: OnProfileSavedListener? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { newUri ->
                currentSelectedPhotoUri = newUri

                // ✅ Glide safely loads & resizes the image
                Glide.with(requireContext())
                    .load(newUri)
                    .override(512, 512) // or smaller if needed
                    .centerCrop()
                    .into(binding.iconImage)
                binding.iconInitials.visibility = View.GONE
                binding.iconImage.visibility = View.VISIBLE
            }
            // If no saved photo, show initials from the current name
            if (currentSelectedPhotoUri == null && binding.iconImage.drawable == null) {
                showInitialsFrom(binding.editName.text?.toString().orEmpty())
            }

        }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.isDraggable = false
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                isCancelable = false
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        dialog.setCancelable(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = ActivityCreateProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Adjust the sheet title & button text
        binding.headerTitle.text = if (isEditMode)
            getString(R.string.edit_account)
        else
            getString(R.string.add_account)

        binding.saveButton.text = if (isEditMode)
            getString(R.string.update)
        else
            getString(R.string.save)

        // 2) If we're in edit mode, pre-fill the fields
        editContact?.let { c ->
            binding.editName.setText(c.name)
            binding.editPhone.setText(c.phone)
            binding.editEmail.setText(c.email)
            binding.editBirthday.setText(c.birthday)
            // if you have more fields, fill them here…
        }

        userPrefsManager = UserPreferencesManager(requireContext())
        loadProfileData()       // your existing loader (will set default and photo)
        setupClickListeners()   // your existing click logic

        // Keep initials live while typing if there's no photo
        binding.editName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (currentSelectedPhotoUri == null && binding.iconImage.drawable == null) {
                    showInitialsFrom(s?.toString().orEmpty())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

// Also ensure avatar is correct on first open
        updateAvatarFromNameOrPhoto()

    }



    private fun loadProfileData() {
        binding.editName.setText(userPrefsManager.userName)
        binding.editPhone.setText(userPrefsManager.userPhone)
        binding.editEmail.setText(userPrefsManager.userEmail)
        binding.editBirthday.setText(userPrefsManager.userBirthday)

        userPrefsManager.getUserPhotoUri()?.let { uri ->
            currentSelectedPhotoUri = uri
            try {
                val persistedUriPermissions = requireContext().contentResolver.persistedUriPermissions
                val hasPermission = persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

                if (hasPermission) {
                    Glide.with(requireContext())
                        .load(uri)
                        .override(512, 512)
                        .centerCrop()
                        .into(binding.iconImage)
                } else {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Glide.with(requireContext())
                        .load(uri)
                        .override(512, 512)
                        .centerCrop()
                        .into(binding.iconImage)
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Failed to grant permission for URI: $uri", e)
                Toast.makeText(context, getString(R.string.error_failed_to_load_image_permission), Toast.LENGTH_LONG).show()
                userPrefsManager.userPhotoUriString = null
                currentSelectedPhotoUri = null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved photo URI: $uri", e)
                Toast.makeText(context, getString(R.string.error_loading_image_uri), Toast.LENGTH_LONG).show()
                userPrefsManager.userPhotoUriString = null
                currentSelectedPhotoUri = null
            }
        }
    }


    fun setupClickListeners() {

        binding.editBirthday.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select your birthday")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(selection))
                binding.editBirthday.setText(formattedDate)
            }

            picker.show(parentFragmentManager, "birthdayPicker")
        }

        binding.editPhotoIcon.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.iconImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

         binding.saveButton.setOnClickListener {
             if (!validateInputs()) return@setOnClickListener

             // ▶️ If we’re already logged in, just save and dismiss
             if (userPrefsManager.isLoggedIn) {
                 saveProfile()
                 onProfileSavedListener?.onProfileSaved()
                 dismiss()
                 return@setOnClickListener
             }

             // 🔑 Not yet logged in: do the OTP flow
             val userEmail = binding.editEmail.text.toString().trim()
             lifecycleScope.launch {
                 try {
                     SupabaseManager.client.auth.signInWith(OTP) {
                         email = userEmail
                     }
                     Log.d(TAG, "✅ OTP email dispatched to $userEmail")
                 } catch (e: Exception) {
                     Log.e(TAG, "❌ Error sending OTP", e)
                     Toast.makeText(
                         requireContext(),
                         "Error sending OTP: ${e.message}",
                         Toast.LENGTH_LONG
                     ).show()
                     return@launch
                 }

                 // Prompt for the 6-digit code
                 val codeInputView = layoutInflater.inflate(R.layout.dialog_enter_code, null)
                 val codeEditText = codeInputView.findViewById<EditText>(R.id.codeEditText)

                 MaterialAlertDialogBuilder(requireContext())
                     .setTitle("Enter the 6-digit code sent to your email")
                     .setView(codeInputView)
                     .setPositiveButton("Verify") { dialog, _ ->
                         val code = codeEditText.text.toString().trim()
                         if (code.length != 6 || !code.all(Char::isDigit)) {
                             Toast.makeText(
                                 context,
                                 getString(R.string.enter_valid_otp_message),
                                 Toast.LENGTH_SHORT
                             ).show()
                             return@setPositiveButton
                         }

                         // Verify OTP
                         lifecycleScope.launch {
                             try {
                                 SupabaseManager.client.auth.verifyEmailOtp(
                                     type  = OtpType.Email.EMAIL,
                                     email = userEmail,
                                     token = code
                                 )

                                 // Mark logged in
                                 SupabaseManager.client.auth
                                     .currentSessionOrNull()
                                     ?.user
                                     ?.id
                                     ?.let { id ->
                                         userPrefsManager.loggedInUserId = id
                                         userPrefsManager.isLoggedIn     = true
                                     }

                                 // Save now that they’re authenticated
                                 saveProfile()
                                 onProfileSavedListener?.onProfileSaved()

                                 Toast.makeText(
                                     context,
                                     getString(R.string.verification_successful),
                                     Toast.LENGTH_SHORT
                                 ).show()

                                 dialog.dismiss()
                                 dismiss()
                             } catch (ve: Exception) {
                                 Toast.makeText(
                                     context,
                                     getString(R.string.verification_failed, ve.localizedMessage),
                                     Toast.LENGTH_LONG
                                 ).show()
                             }
                         }
                     }
                     .setNegativeButton(R.string.cancel, null)
                     .setCancelable(false)
                     .show()
             }
         }

        // set the START phone icon once
        binding.editPhone.setCompoundDrawablesRelativeWithIntrinsicBounds(
            AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_local_phone_24),
            null, null, null
        )

        binding.editPhone.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString().orEmpty()
                val flagEmoji = getFlagForPhoneNumber(phone)
                applyPhoneFlagEnd(flagEmoji)   // <-- only this; no TextDrawable
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

// also apply once on open (for prefilled values)
        binding.editPhone.post {
            applyPhoneFlagEnd(getFlagForPhoneNumber(binding.editPhone.text?.toString().orEmpty()))
        }

    }


    override fun onResume() {
        super.onResume()

        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("shouldSaveProfile", false)) {
            prefs.edit { remove("shouldSaveProfile") }
            saveProfile()
        }
    }


    fun getFlagForPhoneNumber(phone: String): String? {
        val digitsOnly = phone.replace(Regex("[^0-9+]"), "")
        return try {
            val parsed = PhoneNumberUtil.getInstance().parse(digitsOnly, null)
            val region = PhoneNumberUtil.getInstance().getRegionCodeForNumber(parsed)
            if (!region.isNullOrEmpty()) {
                countryCodeToFlagEmoji(region)
            } else {
                // ✅ US fallback for numbers like "307..."
                val localAreaCode = digitsOnly.take(3)
                if (usAreaCodes.contains(localAreaCode)) "🇺🇸" else null
            }
        } catch (_: Exception) {
            // Try local fallback on parse error
            val localAreaCode = digitsOnly.take(3)
            if (usAreaCodes.contains(localAreaCode)) "🇺🇸" else null
        }
    }
    val usAreaCodes = setOf(
        "201", "202", "203", "205", "206", "207", "208", "209", "210", "212", "213", "214", "215", "216", "217",
        "218", "219", "224", "225", "228", "229", "231", "234", "239", "240", "248", "251", "252", "253", "254",
        "256", "260", "262", "267", "269", "270", "272", "274", "276", "281", "301", "302", "303", "304", "305",
        "307", "308", "309", "310", "312", "313", "314", "315", "316", "317", "318", "319", "320", "321", "323",
        "325", "327", "330", "331", "334", "336", "337", "339", "346", "351", "352", "360", "361", "364", "380",
        "385", "386", "401", "402", "404", "405", "406", "407", "408", "409", "410", "412", "413", "414", "415",
        "417", "419", "423", "424", "425", "430", "432", "434", "435", "440", "442", "443", "458", "463", "464",
        "469", "470", "475", "478", "479", "480", "484", "501", "502", "503", "504", "505", "507", "508", "509",
        "510", "512", "513", "515", "516", "517", "518", "520", "530", "531", "534", "539", "540", "541", "551",
        "559", "561", "562", "563", "564", "567", "570", "571", "573", "574", "575", "580", "585", "586", "601",
        "602", "603", "605", "606", "607", "608", "609", "610", "612", "614", "615", "616", "617", "618", "619",
        "620", "623", "626", "628", "629", "630", "631", "636", "641", "646", "650", "651", "657", "660", "661",
        "662", "667", "669", "678", "681", "682", "701", "702", "703", "704", "705", "706", "707", "708", "712",
        "713", "714", "715", "716", "717", "718", "719", "720", "724", "725", "727", "730", "731", "732", "734",
        "737", "740", "743", "747", "754", "757", "758", "760", "762", "763", "764", "765", "769", "770", "772",
        "773", "774", "775", "779", "781", "785", "786", "801", "802", "803", "804", "805", "806", "808", "810",
        "812", "813", "814", "815", "816", "817", "818", "828", "830", "831", "832", "838", "839", "840", "843",
        "845", "847", "848", "850", "854", "856", "857", "858", "859", "860", "862", "863", "864", "865", "870",
        "872", "878", "901", "903", "904", "906", "907", "908", "909", "910", "912", "913", "914", "915", "916",
        "917", "918", "919", "920", "925", "928", "929", "930", "931", "934", "936", "937", "938", "940", "941",
        "947", "949", "951", "952", "954", "956", "957", "959", "970", "971", "972", "973", "975", "976", "978",
        "979", "980", "984", "985", "986", "989"
    )


    fun countryCodeToFlagEmoji(countryCode: String): String {
        return countryCode.uppercase().map {
            Character.toChars(0x1F1E6 + (it.code - 'A'.code)).concatToString()
        }.joinToString("")
    }



    private fun validateInputs(): Boolean {
        var isValid = true
        val name = binding.editName.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        // 👤 Name required
        if (name.isEmpty()) {
            binding.editName.error = getString(R.string.validation_name_required)
            isValid = false
        } else {
            binding.editName.error = null
        }

        // 📞 Phone required
        if (phone.isEmpty()) {
            binding.editPhone.error = getString(R.string.validation_phone_required)
            isValid = false
        } else if (!isValidPhoneNumber(phone)) {
            binding.editPhone.error = "Please enter a valid phone number"
            isValid = false
        } else {
            binding.editPhone.error = null
        }

        // 📧 Email required (now mandatory!)
        if (email.isEmpty()) {
            binding.editEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.error = getString(R.string.validation_email_invalid)
            isValid = false
        } else {
            binding.editEmail.error = null
        }

        return isValid
    }
    private fun isValidPhoneNumber(phone: String): Boolean {
        val digitsOnly = phone.replace(Regex("[^0-9+]"), "")
        return digitsOnly.length >= 10 && digitsOnly.matches(Regex("^\\+?[0-9]{10,15}$"))
    }


    private fun saveProfile() {
        val name     = binding.editName.text.toString().trim()
        val phone    = binding.editPhone.text.toString().trim()
        val email    = binding.editEmail.text.toString().trim()
        val birthday = binding.editBirthday.text.toString().trim()
        val bio      = binding.editBio.text.toString().trim()

        // 1) Persist to SharedPreferences (with URI permission if needed)
        currentSelectedPhotoUri?.let { uri ->
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                userPrefsManager.saveUserProfile(name, phone, email, birthday, bio, uri)
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to persist URI permission for $uri", e)
                Toast.makeText(
                    context,
                    getString(R.string.error_failed_to_save_image_permission),
                    Toast.LENGTH_LONG
                ).show()
                userPrefsManager.saveUserProfile(name, phone, email, birthday, bio, null)
            }
        } ?: run {
            userPrefsManager.saveUserProfile(name, phone, email, birthday, bio, null)
            if (userPrefsManager.getUserPhotoUri() != null) {
                userPrefsManager.clearUserPhoto()
            }
        }

        // 2) Immediate feedback & dismiss
        Toast.makeText(
            context,
            getString(R.string.profile_saved_successfully),
            Toast.LENGTH_SHORT
        ).show()
        onProfileSavedListener?.onProfileSaved()
        dismiss()

        // 3) Fire-and-forget Supabase sync if logged in
        SupabaseManager.client.auth.currentSessionOrNull()?.let { session ->
            val userId    = session.user?.id.orEmpty()
            val authToken = session.accessToken

            if (userId.isNotEmpty() && authToken.isNotEmpty()) {
                lifecycleScope.launch {
                    val success = SupabaseProfileUploader.uploadProfile(
                        userId       = userId,
                        authToken    = authToken,
                        name         = name,
                        phone        = phone,
                        email        = email,
                        bio          = bio,
                        birthday     = birthday,
                        photoUri     = currentSelectedPhotoUri?.toString(),
                        languageCode = Locale.getDefault().language,
                        appVersion   = BuildConfig.VERSION_NAME
                    )
                    if (success) {
                        Log.d(TAG, "✅ Synced profile to Supabase")
                    } else {
                        Log.e(TAG, "❌ Failed to sync profile")
                    }
                }
            }
        }
    }

    private fun showInitialsFrom(name: String) {
        val initials = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it[0].uppercaseChar().toString() }
            .ifEmpty { "?" }

        binding.iconInitials.text = initials
        binding.iconInitials.visibility = View.VISIBLE
        binding.iconImage.setImageDrawable(null)
        binding.iconImage.visibility = View.GONE
    }

    private fun updateAvatarFromNameOrPhoto() {
        val hasPhoto = currentSelectedPhotoUri != null || binding.iconImage.drawable != null
        if (hasPhoto) {
            binding.iconImage.visibility = View.VISIBLE
            binding.iconInitials.visibility = View.GONE
        } else {
            showInitialsFrom(binding.editName.text?.toString().orEmpty())
        }
    }


    private fun emojiToDrawable(ctx: Context, emoji: String, sizePx: Int): Drawable? = try {
        val tv = TextView(ctx).apply {
            text = emoji
            includeFontPadding = false
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.9f)
            setTextColor(Color.BLACK)
            measure(
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, sizePx, sizePx)
        }
        val bmp = androidx.core.graphics.createBitmap(sizePx, sizePx)
        Canvas(bmp).apply { tv.draw(this) }
        bmp.toDrawable(ctx.resources).apply { setBounds(0, 0, sizePx, sizePx) }
    } catch (_: Exception) { null }

    /**
     * @param sizeScale   0.0..1.2 roughly — fraction of lineHeight for flag size
     * @param edgeGapDp   space from the view's END edge to the flag
     * @param textGapDp   space between the text and the flag
     */
    private fun applyPhoneFlagEnd(
        flagEmoji: String?,
        sizeScale: Float = 0.95f,
        edgeGapDp: Int = 1,
        textGapDp: Int = 10
    ) {
        val et = binding.editPhone

        if (flagEmoji.isNullOrEmpty()) {
            et.setCompoundDrawablesRelativeWithIntrinsicBounds(
                AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_local_phone_24),
                null, null, null
            )
            return
        }

        // 1) flag size relative to current line height
        val sizePx = (et.lineHeight * sizeScale).toInt().coerceAtLeast(dp(20))
        val raw = emojiToDrawable(requireContext(), flagEmoji, sizePx) ?: return

        // 2) push the flag inwards from the END edge
        val endInset = InsetDrawable(raw, /*left*/0, /*top*/0, /*right*/dp(edgeGapDp), /*bottom*/0)

        et.setCompoundDrawablesRelativeWithIntrinsicBounds(
            AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_local_phone_24),
            null, endInset, null
        )

        // 3) gap between text and the flag
        et.compoundDrawablePadding = dp(textGapDp)

        // 4) ensure text never overlaps the flag (padding on END)
        val minEndPad = sizePx + dp(edgeGapDp + textGapDp + 2)
        if (et.paddingEnd < minEndPad) {
            et.setPaddingRelative(et.paddingStart, et.paddingTop, minEndPad, et.paddingBottom)
        }
    }

    companion object {
        private const val TAG = "CreateProfileBottomSheet"
    }
}
