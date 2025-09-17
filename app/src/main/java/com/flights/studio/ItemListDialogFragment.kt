package com.flights.studio

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flights.studio.databinding.FragmentItemListDialogListDialogBinding
import com.flights.studio.databinding.ItemListCardBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class ItemListDialogFragment : BottomSheetDialogFragment() {
    private lateinit var rootView: View
    private var _binding: FragmentItemListDialogListDialogBinding? = null
    private val binding get() = _binding!!

    private val itemsList by lazy {
        listOf(
            getString(R.string._307_733_7682), // Phone
            getString(R.string.info_jhairport_org), // Email
            getString(R.string.fbo_general_manager), // Email
            getString(R.string.fbo_customer_service_manager), // Email
            getString(R.string.info_jhflight_org), // New Email
            getString(R.string.jackson_hole_airport_n1250_east_airport_road), // Address
            getString(R.string.PO_Box_n159Jackson_WY_n83001), // Address
            getString(R.string.airport_operations), // Email
            getString(R.string.lost_and_found), // Email
            getString(R.string.human_resources), // Email
            getString(R.string.communications_customer_experience) // Email
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemListDialogListDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view

        // Configure RecyclerView
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = ItemAdapter(itemsList) { position -> handleItemClick(position) }

        // Configure BottomSheetBehavior
        (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            BottomSheetBehavior.from(this).apply {
                peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
                isDraggable = true
            }
        }
    }

    // In your handleItemClick function
    private fun handleItemClick(position: Int) {
        when (position) {
            0 -> dialPhoneNumber(itemsList[position]) // Dial phone number
            in 1..4, in 7..10 -> sendEmail(itemsList[position]) // Send email
            5 -> showSnackbar() // Show map for address
            // No action for PO Box (position 6)
        }
    }


    private fun showSnackbar() {
        val snackbar = Snackbar.make(rootView, "Drive safely and stay focused.", Snackbar.LENGTH_LONG)
        snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.setTextAppearance(R.style.SnackbarText)
        snackbar.setAction("Open Maps") {
            val gmmIntentUri =
                "geo:43.603207,-110.736018?q=43.603207,-110.736018(Jackson Hole Airport)".toUri()
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            startActivity(mapIntent)
        }
        val slideInAnimation = AnimationUtils.loadAnimation(context, androidx.appcompat.R.anim.abc_slide_in_bottom)
        snackbar.view.startAnimation(slideInAnimation)
        snackbar.show()
        Handler(Looper.getMainLooper()).postDelayed({
            snackbar.view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_left))
        }, 9000)
    }




    private fun dialPhoneNumber(phoneNumber: String) {
        startActivity(Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri()))
    }

    private fun sendEmail(emailAddress: String) {
        startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$emailAddress".toUri()))
    }

    private inner class ItemAdapter(private val items: List<String>, private val itemClick: (Int) -> Unit) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemListCardBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.cardView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) itemClick(position)
                }

                // Set elevation programmatically
                binding.cardView.cardElevation = resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemListCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.itemText.text = items[position]

            // Set icon for the card based on the position
            val iconResourceId = when (position) {
                0 -> R.drawable.dialpad_fill0_wght400_grad0_opsz24
                in 1..4 -> R.drawable.baseline_email_24
                5 -> R.drawable.baseline_map_24
                6 -> R.drawable.local_post_office_fill0_wght400_grad0_opsz24
                7 -> R.drawable.manage_accounts_fill0_wght400_grad0_opsz24
                8 -> R.drawable.baseline_no_luggage_24
                9 -> R.drawable.baseline_hub_24
                10 -> R.drawable.baseline_contact_support_24
                else -> R.drawable.baseline_email_24
            }
            holder.binding.itemIcon.setImageResource(iconResourceId)

            // Determine the background color based on the theme
            val backgroundColor = ContextCompat.getColor(
                holder.itemView.context,
                if (isInDarkMode(holder.itemView.context)) R.color.card_background_dark else R.color.card_background_light
            )

            // Set the background color for the card
            holder.binding.cardView.setCardBackgroundColor(backgroundColor)
        }

        override fun getItemCount() = items.size
    }

    private fun isInDarkMode(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setContentView(layoutInflater.inflate(R.layout.fragment_item_list_dialog_list_dialog, dialog.findViewById(R.id.bottom_sheet_container), false))
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(items: List<String>): ItemListDialogFragment {
            return ItemListDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList("itemsList", ArrayList(items))
                }
            }
        }
    }
}
