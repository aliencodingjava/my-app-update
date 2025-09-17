package com.flights.studio

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class PalettePickerBottomSheet(
    private val onPaletteSelected: (ContactsAdapter.ColorPalette) -> Unit
) : BottomSheetDialogFragment() {

    // Create a combined list including null as the "remove color" option
    private var selectedPaletteIndex = -1 // Default: no selection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.holo_color_picker, container, false)
    }

    data class NamedPalette(
        val name: String,
        val palette: ContactsAdapter.ColorPalette?
    )

    private val allPalettes = listOf(
        NamedPalette("Reset to Default", null),
        // 🌎 North America & Central America
        NamedPalette("🇺🇸 United States of America", ContactsAdapter.ColorPalette("#3C3B6E".toColorInt(), "#B22234".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇲🇽 Mexico", ContactsAdapter.ColorPalette("#006847".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇨🇦 Canada", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFFFF".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇩🇴 Dominican Republic", ContactsAdapter.ColorPalette("#002D62".toColorInt(), "#FFFFFF".toColorInt(), "#D62828".toColorInt())),
        NamedPalette("🇸🇻 El Salvador", ContactsAdapter.ColorPalette("#0047AB".toColorInt(), "#FFFFFF".toColorInt(), "#0047AB".toColorInt())),
        NamedPalette("🇬🇹 Guatemala", ContactsAdapter.ColorPalette("#4997D0".toColorInt(), "#FFFFFF".toColorInt(), "#4997D0".toColorInt())),
        NamedPalette("🇭🇳 Honduras", ContactsAdapter.ColorPalette("#0073CF".toColorInt(), "#FFFFFF".toColorInt(), "#0073CF".toColorInt())),
        NamedPalette("🇭🇹 Haiti", ContactsAdapter.ColorPalette("#00209F".toColorInt(), "#D21034".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇨🇺 Cuba", ContactsAdapter.ColorPalette("#002A8F".toColorInt(), "#FFFFFF".toColorInt(), "#CF142B".toColorInt())),
        NamedPalette("🇳🇮 Nicaragua", ContactsAdapter.ColorPalette("#0067C6".toColorInt(), "#FFFFFF".toColorInt(), "#0067C6".toColorInt())),
        NamedPalette("🇵🇦 Panama", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#005AA7".toColorInt(), "#FF0000".toColorInt())), // Optional
        NamedPalette("🇦🇬 Antigua and Barbuda", ContactsAdapter.ColorPalette("#007BFF".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇧🇸 Bahamas", ContactsAdapter.ColorPalette("#00ABC9".toColorInt(), "#FFD700".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇧🇧 Barbados", ContactsAdapter.ColorPalette("#00267F".toColorInt(), "#FFC726".toColorInt(), "#00267F".toColorInt())),
        NamedPalette("🇧🇿 Belize", ContactsAdapter.ColorPalette("#003F87".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇨🇷 Costa Rica", ContactsAdapter.ColorPalette("#002B7F".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇩🇲 Dominica", ContactsAdapter.ColorPalette("#006B3F".toColorInt(), "#FFD100".toColorInt(), "#C8102E".toColorInt())),
        NamedPalette("🇬🇩 Grenada", ContactsAdapter.ColorPalette("#FCD116".toColorInt(), "#CE1126".toColorInt(), "#007847".toColorInt())),
        NamedPalette("🇯🇲 Jamaica", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFD100".toColorInt(), "#007847".toColorInt())),
        NamedPalette("🇰🇳 Saint Kitts and Nevis", ContactsAdapter.ColorPalette("#FFD100".toColorInt(), "#000000".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇱🇨 Saint Lucia", ContactsAdapter.ColorPalette("#66CCFF".toColorInt(), "#000000".toColorInt(), "#FFD700".toColorInt())),
        NamedPalette("🇻🇨 Saint Vincent and the Grenadines", ContactsAdapter.ColorPalette("#FCD116".toColorInt(), "#009E49".toColorInt(), "#0072C6".toColorInt())),
        NamedPalette("🇹🇹 Trinidad and Tobago", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),


        // 🌎 South America
        NamedPalette("🇵🇪 Peru", ContactsAdapter.ColorPalette("#D91023".toColorInt(), "#FFFFFF".toColorInt(), "#D91023".toColorInt())),
        NamedPalette("🇨🇱 Chile", ContactsAdapter.ColorPalette("#0033A0".toColorInt(), "#FFFFFF".toColorInt(), "#D52B1E".toColorInt())),
        NamedPalette("🇦🇷 Argentina", ContactsAdapter.ColorPalette("#74ACDF".toColorInt(), "#FFFFFF".toColorInt(), "#F6B40E".toColorInt())),
        NamedPalette("🇪🇨 Ecuador", ContactsAdapter.ColorPalette("#FFD100".toColorInt(), "#003893".toColorInt(), "#D72828".toColorInt())),
        NamedPalette("🇧🇴 Bolivia", ContactsAdapter.ColorPalette("#D52B1E".toColorInt(), "#FFD100".toColorInt(), "#007934".toColorInt())),
        NamedPalette("🇺🇾 Uruguay", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#5CC4E0".toColorInt(), "#FFD700".toColorInt())),
        NamedPalette("🇵🇾 Paraguay", ContactsAdapter.ColorPalette("#D52B1E".toColorInt(), "#FFFFFF".toColorInt(), "#0038A8".toColorInt())),
        NamedPalette("🇨🇴 Colombia", ContactsAdapter.ColorPalette("#FCD116".toColorInt(), "#003893".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇧🇷 Brazil", ContactsAdapter.ColorPalette("#009C3B".toColorInt(), "#FFDF00".toColorInt(), "#002776".toColorInt())),
        NamedPalette("🇬🇾 Guyana", ContactsAdapter.ColorPalette("#009739".toColorInt(), "#FFD100".toColorInt(), "#EF3340".toColorInt())),
        NamedPalette("🇸🇷 Suriname", ContactsAdapter.ColorPalette("#377E3F".toColorInt(), "#FFFFFF".toColorInt(), "#B40A2D".toColorInt())),
        NamedPalette("🇻🇪 Venezuela", ContactsAdapter.ColorPalette("#FDB913".toColorInt(), "#00247D".toColorInt(), "#CF142B".toColorInt())),
        NamedPalette("🇫🇰 Falkland Islands", ContactsAdapter.ColorPalette("#00247D".toColorInt(), "#FFFFFF".toColorInt(), "#CF142B".toColorInt())),
                // 🌍 Europe
        NamedPalette("🇬🇧 United Kingdom", ContactsAdapter.ColorPalette("#00247D".toColorInt(), "#FFFFFF".toColorInt(), "#CF142B".toColorInt())),
        NamedPalette("🇩🇪 Germany", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#DD0000".toColorInt(), "#FFCE00".toColorInt())),
        NamedPalette("🇫🇷 France", ContactsAdapter.ColorPalette("#0055A4".toColorInt(), "#FFFFFF".toColorInt(), "#EF4135".toColorInt())),
        NamedPalette("🇪🇸 Spain", ContactsAdapter.ColorPalette("#AA151B".toColorInt(), "#F1BF00".toColorInt(), "#AA151B".toColorInt())),
        NamedPalette("🇵🇹 Portugal", ContactsAdapter.ColorPalette("#006600".toColorInt(), "#FF0000".toColorInt(), "#FFCC00".toColorInt())),
        NamedPalette("🇮🇹 Italy", ContactsAdapter.ColorPalette("#008C45".toColorInt(), "#F4F5F0".toColorInt(), "#CD212A".toColorInt())),
        NamedPalette("🇵🇱 Poland", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#DC143C".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇷🇴 Romania", ContactsAdapter.ColorPalette("#002B7F".toColorInt(), "#FCD116".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇲🇩 Moldova", ContactsAdapter.ColorPalette("#0033A0".toColorInt(), "#FFD700".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇷🇺 Russia", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#0039A6".toColorInt(), "#D52B1E".toColorInt())),
        NamedPalette("🇨🇭 Switzerland", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFFFF".toColorInt(), "#FF0000".toColorInt())), // Optional
        NamedPalette("🇸🇪 Sweden", ContactsAdapter.ColorPalette("#006AA7".toColorInt(), "#FECC00".toColorInt(), "#006AA7".toColorInt())), // Optional
        NamedPalette("🇧🇪 Belgium", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFD700".toColorInt(), "#EF3340".toColorInt())), // Optional
        NamedPalette("🇬🇷 Greece", ContactsAdapter.ColorPalette("#0D5EAF".toColorInt(), "#FFFFFF".toColorInt(), "#0D5EAF".toColorInt())),
        NamedPalette("🇦🇱 Albania", ContactsAdapter.ColorPalette("#E41E20".toColorInt(), "#000000".toColorInt(), "#E41E20".toColorInt())),
        NamedPalette("🇦🇩 Andorra", ContactsAdapter.ColorPalette("#FFD700".toColorInt(), "#0033A0".toColorInt(), "#D1001C".toColorInt())),
        NamedPalette("🇦🇹 Austria", ContactsAdapter.ColorPalette("#ED2939".toColorInt(), "#FFFFFF".toColorInt(), "#ED2939".toColorInt())),
        NamedPalette("🇧🇦 Bosnia and Herzegovina", ContactsAdapter.ColorPalette("#002395".toColorInt(), "#FECB00".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇧🇬 Bulgaria", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#00966E".toColorInt(), "#D62612".toColorInt())),
        NamedPalette("🇭🇷 Croatia", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFFFF".toColorInt(), "#171796".toColorInt())),
        NamedPalette("🇨🇿 Czechia (Czech Republic)", ContactsAdapter.ColorPalette("#D7141A".toColorInt(), "#FFFFFF".toColorInt(), "#11457E".toColorInt())),
        NamedPalette("🇩🇰 Denmark", ContactsAdapter.ColorPalette("#C60C30".toColorInt(), "#FFFFFF".toColorInt(), "#C60C30".toColorInt())),
        NamedPalette("🇪🇪 Estonia", ContactsAdapter.ColorPalette("#0072CE".toColorInt(), "#000000".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇫🇮 Finland", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#002F6C".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇭🇺 Hungary", ContactsAdapter.ColorPalette("#CD2A3E".toColorInt(), "#FFFFFF".toColorInt(), "#436F4D".toColorInt())),
        NamedPalette("🇮🇸 Iceland", ContactsAdapter.ColorPalette("#02529C".toColorInt(), "#FFFFFF".toColorInt(), "#DC1E35".toColorInt())),
        NamedPalette("🇮🇪 Ireland", ContactsAdapter.ColorPalette("#169B62".toColorInt(), "#FFFFFF".toColorInt(), "#FF883E".toColorInt())),
        NamedPalette("🇽🇰 Kosovo", ContactsAdapter.ColorPalette("#244AA5".toColorInt(), "#FFFFFF".toColorInt(), "#D4AF37".toColorInt())),
        NamedPalette("🇱🇻 Latvia", ContactsAdapter.ColorPalette("#9E3039".toColorInt(), "#FFFFFF".toColorInt(), "#9E3039".toColorInt())),
        NamedPalette("🇱🇮 Liechtenstein", ContactsAdapter.ColorPalette("#002171".toColorInt(), "#FF0000".toColorInt(), "#FFD700".toColorInt())),
        NamedPalette("🇱🇹 Lithuania", ContactsAdapter.ColorPalette("#FDB913".toColorInt(), "#006A44".toColorInt(), "#C1272D".toColorInt())),
        NamedPalette("🇱🇺 Luxembourg", ContactsAdapter.ColorPalette("#ED2939".toColorInt(), "#FFFFFF".toColorInt(), "#00A1DE".toColorInt())),
        NamedPalette("🇲🇹 Malta", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#C8102E".toColorInt(), "#C8102E".toColorInt())),
        NamedPalette("🇲🇨 Monaco", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇲🇪 Montenegro", ContactsAdapter.ColorPalette("#D81E05".toColorInt(), "#FCD116".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇳🇱 Netherlands", ContactsAdapter.ColorPalette("#21468B".toColorInt(), "#FFFFFF".toColorInt(), "#AE1C28".toColorInt())),
        NamedPalette("🇲🇰 North Macedonia", ContactsAdapter.ColorPalette("#D20000".toColorInt(), "#FFE600".toColorInt(), "#D20000".toColorInt())),
        NamedPalette("🇳🇴 Norway", ContactsAdapter.ColorPalette("#BA0C2F".toColorInt(), "#FFFFFF".toColorInt(), "#00205B".toColorInt())),
        NamedPalette("🇸🇰 Slovakia", ContactsAdapter.ColorPalette("#0B4EA2".toColorInt(), "#FFFFFF".toColorInt(), "#EE1C25".toColorInt())),
        NamedPalette("🇸🇮 Slovenia", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#005CBF".toColorInt(), "#ED1C24".toColorInt())),
        NamedPalette("🇸🇲 San Marino", ContactsAdapter.ColorPalette("#5EB6E4".toColorInt(), "#FFFFFF".toColorInt(), "#FFD700".toColorInt())),
        NamedPalette("🇷🇸 Serbia", ContactsAdapter.ColorPalette("#C6363C".toColorInt(), "#0C4076".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇺🇦 Ukraine", ContactsAdapter.ColorPalette("#005BBB".toColorInt(), "#FFD500".toColorInt(), "#005BBB".toColorInt())),
        NamedPalette("🇻🇦 Vatican City", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#FFD700".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇧🇾 Belarus", ContactsAdapter.ColorPalette("#D22730".toColorInt(), "#FFFFFF".toColorInt(), "#007C30".toColorInt())),

        // 🌏 Asia
        NamedPalette("🇮🇳 India", ContactsAdapter.ColorPalette("#FF9933".toColorInt(), "#FFFFFF".toColorInt(), "#138808".toColorInt())),
        NamedPalette("🇨🇳 China", ContactsAdapter.ColorPalette("#DE2910".toColorInt(), "#FFDE00".toColorInt(), "#DE2910".toColorInt())),
        NamedPalette("🇯🇵 Japan", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#BC002D".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇮🇩 Indonesia", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFFFF".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇵🇭 Philippines", ContactsAdapter.ColorPalette("#0038A8".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇰🇷 South Korea", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#003478".toColorInt(), "#C60C30".toColorInt())),
        NamedPalette("🇹🇷 Turkey", ContactsAdapter.ColorPalette("#E30A17".toColorInt(), "#FFFFFF".toColorInt(), "#E30A17".toColorInt())),
        NamedPalette("🇮🇱 Israel", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#0038B8".toColorInt(), "#0038B8".toColorInt())),
        NamedPalette("🇵🇰 Pakistan", ContactsAdapter.ColorPalette("#01411C".toColorInt(), "#FFFFFF".toColorInt(), "#01411C".toColorInt())), // Optional
        NamedPalette("🇹🇭 Thailand", ContactsAdapter.ColorPalette("#A51931".toColorInt(), "#FFFFFF".toColorInt(), "#2D2A4A".toColorInt())), // Optional
        NamedPalette("🇧🇩 Bangladesh", ContactsAdapter.ColorPalette("#006A4E".toColorInt(), "#F42A41".toColorInt(), "#006A4E".toColorInt())), // Optional
        NamedPalette("🇦🇫 Afghanistan", ContactsAdapter.ColorPalette("#007A5E".toColorInt(), "#000000".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇦🇲 Armenia", ContactsAdapter.ColorPalette("#D90012".toColorInt(), "#0033A0".toColorInt(), "#F2A800".toColorInt())),
        NamedPalette("🇦🇿 Azerbaijan", ContactsAdapter.ColorPalette("#00B3E3".toColorInt(), "#EF3340".toColorInt(), "#509E2F".toColorInt())),
        NamedPalette("🇧🇭 Bahrain", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#D71A28".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇧🇹 Bhutan", ContactsAdapter.ColorPalette("#FFCC00".toColorInt(), "#FF6600".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇧🇳 Brunei", ContactsAdapter.ColorPalette("#FFD100".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇰🇭 Cambodia", ContactsAdapter.ColorPalette("#032EA1".toColorInt(), "#E00025".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇨🇾 Cyprus", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#D57800".toColorInt(), "#228B22".toColorInt())),
        NamedPalette("🇬🇪 Georgia", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#FF0000".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇮🇷 Iran", ContactsAdapter.ColorPalette("#239F40".toColorInt(), "#FFFFFF".toColorInt(), "#DA0000".toColorInt())),
        NamedPalette("🇮🇶 Iraq", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#000000".toColorInt(), "#D32011".toColorInt())),
        NamedPalette("🇯🇴 Jordan", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFFFFF".toColorInt(), "#007A3D".toColorInt())),
        NamedPalette("🇰🇿 Kazakhstan", ContactsAdapter.ColorPalette("#00AFCA".toColorInt(), "#F8D70F".toColorInt(), "#00AFCA".toColorInt())),
        NamedPalette("🇰🇼 Kuwait", ContactsAdapter.ColorPalette("#007A3D".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇰🇬 Kyrgyzstan", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFF00".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇱🇦 Laos", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#002868".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇱🇧 Lebanon", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#FF0000".toColorInt(), "#007A3D".toColorInt())),
        NamedPalette("🇲🇾 Malaysia", ContactsAdapter.ColorPalette("#010066".toColorInt(), "#FFCC00".toColorInt(), "#C8102E".toColorInt())),
        NamedPalette("🇲🇻 Maldives", ContactsAdapter.ColorPalette("#D21034".toColorInt(), "#007E3A".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇲🇳 Mongolia", ContactsAdapter.ColorPalette("#C4272F".toColorInt(), "#015197".toColorInt(), "#FFD900".toColorInt())),
        NamedPalette("🇲🇲 Myanmar", ContactsAdapter.ColorPalette("#FFD700".toColorInt(), "#34B233".toColorInt(), "#EA2839".toColorInt())),
        NamedPalette("🇳🇵 Nepal", ContactsAdapter.ColorPalette("#DC143C".toColorInt(), "#000080".toColorInt(), "#FFFFFF".toColorInt())),
        NamedPalette("🇴🇲 Oman", ContactsAdapter.ColorPalette("#C8102E".toColorInt(), "#FFFFFF".toColorInt(), "#007A3D".toColorInt())),
        NamedPalette("🇶🇦 Qatar", ContactsAdapter.ColorPalette("#8D1B3D".toColorInt(), "#FFFFFF".toColorInt(), "#8D1B3D".toColorInt())),
        NamedPalette("🇸🇦 Saudi Arabia", ContactsAdapter.ColorPalette("#006C35".toColorInt(), "#FFFFFF".toColorInt(), "#006C35".toColorInt())),
        NamedPalette("🇸🇬 Singapore", ContactsAdapter.ColorPalette("#EF3340".toColorInt(), "#FFFFFF".toColorInt(), "#EF3340".toColorInt())),
        NamedPalette("🇱🇰 Sri Lanka", ContactsAdapter.ColorPalette("#FCBC00".toColorInt(), "#C8102E".toColorInt(), "#006A44".toColorInt())),
        NamedPalette("🇸🇾 Syria", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#000000".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇹🇯 Tajikistan", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#FFFFFF".toColorInt(), "#228B22".toColorInt())),
        NamedPalette("🇹🇱 Timor-Leste", ContactsAdapter.ColorPalette("#FF0000".toColorInt(), "#000000".toColorInt(), "#FAD201".toColorInt())),
        NamedPalette("🇹🇲 Turkmenistan", ContactsAdapter.ColorPalette("#009639".toColorInt(), "#FFFFFF".toColorInt(), "#FFCC00".toColorInt())),
        NamedPalette("🇦🇪 United Arab Emirates", ContactsAdapter.ColorPalette("#00732F".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇺🇿 Uzbekistan", ContactsAdapter.ColorPalette("#1EB53A".toColorInt(), "#FFFFFF".toColorInt(), "#0099B5".toColorInt())),
        NamedPalette("🇻🇳 Vietnam", ContactsAdapter.ColorPalette("#DA251D".toColorInt(), "#FFFF00".toColorInt(), "#DA251D".toColorInt())),
        NamedPalette("🇾🇪 Yemen", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇰🇵 North Korea", ContactsAdapter.ColorPalette("#024FA2".toColorInt(), "#FFFFFF".toColorInt(), "#ED1C27".toColorInt())),
        NamedPalette("🇹🇼 Taiwan", ContactsAdapter.ColorPalette("#FE0000".toColorInt(), "#FFFFFF".toColorInt(), "#000095".toColorInt())),
        NamedPalette("🇵🇸 Palestine", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFFFFF".toColorInt(), "#007A3D".toColorInt())),



        // 🌍 Africa
        NamedPalette("🇰🇪 Kenya", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#BB0000".toColorInt(), "#006600".toColorInt())),
        NamedPalette("🇳🇬 Nigeria", ContactsAdapter.ColorPalette("#008751".toColorInt(), "#FFFFFF".toColorInt(), "#008751".toColorInt())),
        NamedPalette("🇪🇬 Egypt", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())), // Optional
        NamedPalette("🇲🇦 Morocco", ContactsAdapter.ColorPalette("#C1272D".toColorInt(), "#006233".toColorInt(), "#C1272D".toColorInt())), // Optional
        NamedPalette("🇹🇳 Tunisia", ContactsAdapter.ColorPalette("#E70013".toColorInt(), "#FFFFFF".toColorInt(), "#E70013".toColorInt())), // Optional
        NamedPalette("🇿🇦 South Africa", ContactsAdapter.ColorPalette("#007847".toColorInt(), "#FFD100".toColorInt(), "#DA1212".toColorInt())),
        NamedPalette("🇩🇿 Algeria", ContactsAdapter.ColorPalette("#006233".toColorInt(), "#FFFFFF".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇦🇴 Angola", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FF0000".toColorInt(), "#FFD700".toColorInt())),
        NamedPalette("🇧🇯 Benin", ContactsAdapter.ColorPalette("#FCD116".toColorInt(), "#FFFFFF".toColorInt(), "#E8112D".toColorInt())),
        NamedPalette("🇧🇼 Botswana", ContactsAdapter.ColorPalette("#6AA9E9".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇧🇫 Burkina Faso", ContactsAdapter.ColorPalette("#EF2B2D".toColorInt(), "#009E49".toColorInt(), "#FDB913".toColorInt())),
        NamedPalette("🇧🇮 Burundi", ContactsAdapter.ColorPalette("#CF0921".toColorInt(), "#FFFFFF".toColorInt(), "#1EB53A".toColorInt())),
        NamedPalette("🇨🇻 Cabo Verde", ContactsAdapter.ColorPalette("#003893".toColorInt(), "#FFFFFF".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇨🇫 Central African Republic", ContactsAdapter.ColorPalette("#002395".toColorInt(), "#FFFFFF".toColorInt(), "#FECB00".toColorInt())),
        NamedPalette("🇹🇩 Chad", ContactsAdapter.ColorPalette("#002664".toColorInt(), "#FECB00".toColorInt(), "#C60C30".toColorInt())),
        NamedPalette("🇰🇲 Comoros", ContactsAdapter.ColorPalette("#3A75C4".toColorInt(), "#FFD700".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇨🇬 Republic of the Congo", ContactsAdapter.ColorPalette("#009543".toColorInt(), "#FBDE4A".toColorInt(), "#DC241F".toColorInt())),
        NamedPalette("🇨🇩 Democratic Republic of the Congo", ContactsAdapter.ColorPalette("#007FFF".toColorInt(), "#F7D618".toColorInt(), "#CE1021".toColorInt())),
        NamedPalette("🇨🇮 Côte d'Ivoire", ContactsAdapter.ColorPalette("#F77F00".toColorInt(), "#FFFFFF".toColorInt(), "#009E60".toColorInt())),
        NamedPalette("🇩🇯 Djibouti", ContactsAdapter.ColorPalette("#6AB2E7".toColorInt(), "#FFFFFF".toColorInt(), "#12AD2B".toColorInt())),
        NamedPalette("🇬🇶 Equatorial Guinea", ContactsAdapter.ColorPalette("#007847".toColorInt(), "#FFFFFF".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇪🇷 Eritrea", ContactsAdapter.ColorPalette("#EF2118".toColorInt(), "#00ADEF".toColorInt(), "#FFD200".toColorInt())),
        NamedPalette("🇸🇿 Eswatini", ContactsAdapter.ColorPalette("#3E5EB9".toColorInt(), "#FFD900".toColorInt(), "#D82300".toColorInt())),
        NamedPalette("🇪🇹 Ethiopia", ContactsAdapter.ColorPalette("#078930".toColorInt(), "#FCD116".toColorInt(), "#E30B17".toColorInt())),
        NamedPalette("🇬🇦 Gabon", ContactsAdapter.ColorPalette("#009E60".toColorInt(), "#FCD116".toColorInt(), "#3A75C4".toColorInt())),
        NamedPalette("🇬🇲 Gambia", ContactsAdapter.ColorPalette("#3A75C4".toColorInt(), "#FFFFFF".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇬🇭 Ghana", ContactsAdapter.ColorPalette("#006B3F".toColorInt(), "#FCD116".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇬🇳 Guinea", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#FCD116".toColorInt(), "#009E60".toColorInt())),
        NamedPalette("🇬🇼 Guinea-Bissau", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#FCD116".toColorInt(), "#009E60".toColorInt())),
        NamedPalette("🇱🇸 Lesotho", ContactsAdapter.ColorPalette("#00209F".toColorInt(), "#FFFFFF".toColorInt(), "#009543".toColorInt())),
        NamedPalette("🇱🇷 Liberia", ContactsAdapter.ColorPalette("#BF0A30".toColorInt(), "#FFFFFF".toColorInt(), "#002868".toColorInt())),
        NamedPalette("🇲🇬 Madagascar", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#F14A00".toColorInt(), "#007E3A".toColorInt())),
        NamedPalette("🇲🇼 Malawi", ContactsAdapter.ColorPalette("#CE1126".toColorInt(), "#000000".toColorInt(), "#008751".toColorInt())),
        NamedPalette("🇲🇱 Mali", ContactsAdapter.ColorPalette("#14B53A".toColorInt(), "#FCD116".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇲🇷 Mauritania", ContactsAdapter.ColorPalette("#006233".toColorInt(), "#FFD700".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇲🇺 Mauritius", ContactsAdapter.ColorPalette("#EA2839".toColorInt(), "#FFD500".toColorInt(), "#1A206D".toColorInt())),
        NamedPalette("🇲🇿 Mozambique", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FCE100".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇳🇦 Namibia", ContactsAdapter.ColorPalette("#003580".toColorInt(), "#FFD100".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇳🇪 Niger", ContactsAdapter.ColorPalette("#0DB02B".toColorInt(), "#FFFFFF".toColorInt(), "#E05206".toColorInt())),
        NamedPalette("🇷🇼 Rwanda", ContactsAdapter.ColorPalette("#00A1DE".toColorInt(), "#FFD100".toColorInt(), "#007847".toColorInt())),
        NamedPalette("🇸🇹 Sao Tome and Principe", ContactsAdapter.ColorPalette("#12AD2B".toColorInt(), "#FFD700".toColorInt(), "#FF0000".toColorInt())),
        NamedPalette("🇸🇳 Senegal", ContactsAdapter.ColorPalette("#00853F".toColorInt(), "#FDEF42".toColorInt(), "#E31B23".toColorInt())),
        NamedPalette("🇸🇨 Seychelles", ContactsAdapter.ColorPalette("#D62828".toColorInt(), "#0033A0".toColorInt(), "#FCD116".toColorInt())),
        NamedPalette("🇸🇱 Sierra Leone", ContactsAdapter.ColorPalette("#1EB53A".toColorInt(), "#FFFFFF".toColorInt(), "#0072C6".toColorInt())),
        NamedPalette("🇸🇴 Somalia", ContactsAdapter.ColorPalette("#4189DD".toColorInt(), "#FFFFFF".toColorInt(), "#4189DD".toColorInt())),
        NamedPalette("🇸🇸 South Sudan", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFD100".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇸🇩 Sudan", ContactsAdapter.ColorPalette("#D21034".toColorInt(), "#FFFFFF".toColorInt(), "#000000".toColorInt())),
        NamedPalette("🇹🇿 Tanzania", ContactsAdapter.ColorPalette("#17B636".toColorInt(), "#000000".toColorInt(), "#00A3DD".toColorInt())),
        NamedPalette("🇹🇬 Togo", ContactsAdapter.ColorPalette("#006A4E".toColorInt(), "#FFFF00".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇹🇳 Tunisia", ContactsAdapter.ColorPalette("#E70013".toColorInt(), "#FFFFFF".toColorInt(), "#E70013".toColorInt())),
        NamedPalette("🇺🇬 Uganda", ContactsAdapter.ColorPalette("#FFD700".toColorInt(), "#000000".toColorInt(), "#D90000".toColorInt())),
        NamedPalette("🇿🇲 Zambia", ContactsAdapter.ColorPalette("#198A00".toColorInt(), "#FF0000".toColorInt(), "#FF6600".toColorInt())),
        NamedPalette("🇿🇼 Zimbabwe", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#FFD700".toColorInt(), "#D40000".toColorInt())),
        NamedPalette("🇨🇲 Cameroon", ContactsAdapter.ColorPalette("#007A5E".toColorInt(), "#FFCD00".toColorInt(), "#CE1126".toColorInt())),
        NamedPalette("🇱🇾 Libya", ContactsAdapter.ColorPalette("#239E46".toColorInt(), "#000000".toColorInt(), "#E70013".toColorInt())),



        // 🌏 Oceania
        NamedPalette("🇦🇺 Australia", ContactsAdapter.ColorPalette("#002868".toColorInt(), "#FFFFFF".toColorInt(), "#FF0000".toColorInt())), // Optional
        NamedPalette("🇳🇿 New Zealand", ContactsAdapter.ColorPalette("#00247D".toColorInt(), "#FFFFFF".toColorInt(), "#CC142B".toColorInt())), // Optional
        NamedPalette("🇫🇯 Fiji", ContactsAdapter.ColorPalette("#66C6D9".toColorInt(), "#FFFFFF".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇵🇬 Papua New Guinea", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFD100".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇫🇯 Fiji", ContactsAdapter.ColorPalette("#66C6D9".toColorInt(), "#FFFFFF".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇵🇬 Papua New Guinea", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFD100".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇼🇸 Samoa", ContactsAdapter.ColorPalette("#002B7F".toColorInt(), "#FFFFFF".toColorInt(), "#D21034".toColorInt())),
        NamedPalette("🇨🇳 Tonga", ContactsAdapter.ColorPalette("#C10000".toColorInt(), "#FFFFFF".toColorInt(), "#C10000".toColorInt())),
        NamedPalette("🇦🇼 Kiribati", ContactsAdapter.ColorPalette("#FFFFFF".toColorInt(), "#D71A28".toColorInt(), "#002868".toColorInt())),
        NamedPalette("🇵🇾 Solomon Islands", ContactsAdapter.ColorPalette("#0051BA".toColorInt(), "#FFD100".toColorInt(), "#007847".toColorInt())),
        NamedPalette("🇼🇫 Vanuatu", ContactsAdapter.ColorPalette("#000000".toColorInt(), "#FFD100".toColorInt(), "#C8102E".toColorInt())),
        NamedPalette("🇸🇲 Tuvalu", ContactsAdapter.ColorPalette("#00247D".toColorInt(), "#FFFFFF".toColorInt(), "#FFD700".toColorInt())),
        NamedPalette("🇼🇲 Micronesia", ContactsAdapter.ColorPalette("#75B2DD".toColorInt(), "#FFFFFF".toColorInt(), "#75B2DD".toColorInt())),
        NamedPalette("🇨🇺 Palau", ContactsAdapter.ColorPalette("#FFD100".toColorInt(), "#4AADD6".toColorInt(), "#FFD100".toColorInt())),
        NamedPalette("🇳🇿 Marshall Islands", ContactsAdapter.ColorPalette("#002B7F".toColorInt(), "#FFFFFF".toColorInt(), "#FFB612".toColorInt())),
    )

    private var filteredPalettes = allPalettes.toMutableList()


    private fun getColorName(color: Int): String {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        return when {
            value < 0.15f -> "Black"
            value > 0.95f && sat < 0.1f -> "White"
            sat < 0.2f -> "Gray"

            hue in 15.0..45.0 && value < 0.5f -> "Brown" // NEW
            hue < 15 || hue >= 345 -> "Red"
            hue in 15.0..45.0 -> "Orange"
            hue in 45.0..65.0 -> "Yellow"
            hue in 65.0..85.0 -> "Lime"
            hue in 85.0..150.0 -> "Green"
            hue in 150.0..180.0 -> "Turquoise"
            hue in 180.0..210.0 -> "Cyan"
            hue in 210.0..250.0 -> "Blue"
            hue in 250.0..290.0 -> "Purple"
            hue in 290.0..320.0 -> "Magenta"
            hue in 320.0..345.0 -> "Pink"

            else -> "Custom"
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                val screenHeight = requireContext().resources.displayMetrics.heightPixels
                val desiredHeight = (screenHeight * 0.85).toInt()
                it.layoutParams.height = desiredHeight
                it.requestLayout()

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false

                it.translationY = screenHeight.toFloat()
                it.animate().translationY(0f).setDuration(350).start()
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listView = view.findViewById<ListView>(R.id.holo_color_list)

        val adapter = object : ArrayAdapter<NamedPalette>(
            requireContext(),
            R.layout.list_item_palette,
            filteredPalettes
        ) {
            override fun getCount(): Int = filteredPalettes.size
            override fun getItem(position: Int): NamedPalette? = filteredPalettes[position]

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_palette, parent, false)

                val rect1 = row.findViewById<View>(R.id.color_rect_1)
                val rect2 = row.findViewById<View>(R.id.color_rect_2)
                val rect3 = row.findViewById<View>(R.id.color_rect_3)

                val label1 = row.findViewById<TextView>(R.id.label_color_1)
                val label2 = row.findViewById<TextView>(R.id.label_color_2)
                val label3 = row.findViewById<TextView>(R.id.label_color_3)
                val title = row.findViewById<TextView>(R.id.palette_title)

                val previewContainer = row.findViewById<View>(R.id.mini_preview)
                val previewHeader = previewContainer.findViewById<MaterialCardView>(R.id.collapsing_text_container)

                val previewContent = previewContainer.findViewById<MaterialCardView>(R.id.expandable_content)
                val btn1 = previewContainer.findViewById<MaterialCardView>(R.id.fab_call)
                val btn2 = previewContainer.findViewById<MaterialCardView>(R.id.fab_update)
                val btn3 = previewContainer.findViewById<MaterialCardView>(R.id.fab_delete)

                val item = getItem(position)
                title.text = item?.name ?: "Unnamed Palette"
                val palette = item?.palette

                val labelReset = context.getString(R.string.label_reset)

                val mainColor = palette?.mainColor ?: Color.LTGRAY
                val overlayColor = palette?.overlayColor ?: Color.LTGRAY
                val buttonColor = palette?.buttonColor ?: Color.LTGRAY

                val searchView = view.findViewById<SearchView>(R.id.search_palette)
                val titleText = view.findViewById<TextView>(R.id.text_select_color)

                searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
                    titleText.visibility = if (hasFocus) View.GONE else View.VISIBLE
                }

                applyRoundedColor(rect1, mainColor)
                applyRoundedColor(rect2, overlayColor)
                applyRoundedColor(rect3, buttonColor)

                label1.text = if (palette == null) labelReset else getColorName(mainColor)
                label2.text = if (palette == null) labelReset else getColorName(overlayColor)
                label3.text = if (palette == null) labelReset else getColorName(buttonColor)

                tintViewDrawable(previewHeader, mainColor)
                tintViewDrawable(previewContent, overlayColor)
                tintViewDrawable(btn1, buttonColor)
                tintViewDrawable(btn2, buttonColor)
                tintViewDrawable(btn3, buttonColor)

                val card = row.findViewById<View>(R.id.palette_card)
                card.background = if (position == selectedPaletteIndex)
                    ContextCompat.getDrawable(context, R.drawable.rounded_palette_circle)
                else
                    ContextCompat.getDrawable(context, R.drawable.frame_background)


                return row
            }

        }


        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedPaletteIndex = position
            adapter.notifyDataSetChanged()
            val selected = filteredPalettes[position].palette
            val palette = selected ?: ContactsAdapter.ColorPalette(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
            onPaletteSelected(palette)
            dismiss()
        }
        val searchView = view.findViewById<SearchView>(R.id.search_palette)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText.orEmpty().trim().lowercase()
                filteredPalettes.clear()
                filteredPalettes.addAll(
                    if (query.isEmpty()) {
                        allPalettes
                    } else {
                        allPalettes.filter { it.name.lowercase().contains(query) }
                    }
                )
                adapter.notifyDataSetChanged()
                return true
            }
        })


    }
    private fun tintViewDrawable(view: View, color: Int) {
        val background = view.background?.mutate()
        if (background != null) {
            val wrapped = androidx.core.graphics.drawable.DrawableCompat.wrap(background)
            androidx.core.graphics.drawable.DrawableCompat.setTint(wrapped, color)
            view.background = wrapped
        }
    }



    private fun applyRoundedColor(view: View, color: Int) {
        val original = ContextCompat.getDrawable(view.context, R.drawable.dialog_background)
        if (original is android.graphics.drawable.GradientDrawable) {
            val drawable = original.constantState?.newDrawable()?.mutate() as? android.graphics.drawable.GradientDrawable
            drawable?.setColor(color)
            view.background = drawable
        }
    }

}
