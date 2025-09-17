<a href="https://github.com/aliencodingjava/my-app-update/releases/tag/v0.2.213">
  <img src="https://github.com/user-attachments/assets/53971440-9b73-447e-b7eb-68436d034f77" alt="Jack Flight Studio banner" width="100%">
</a>




# ✈️ Jack Flight Studio
  <defs>
    <linearGradient id="dbg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#0F172A"/>
      <stop offset="50%" stop-color="#111827"/>
      <stop offset="100%" stop-color="#0B1220"/>
    </linearGradient>
    <radialGradient id="dglow1" cx="0.2" cy="0.2" r="0.7">
      <stop offset="0%" stop-color="#22D3EE" stop-opacity="0.25"/>
      <stop offset="100%" stop-color="#22D3EE" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="dglow2" cx="0.85" cy="0.25" r="0.8">
      <stop offset="0%" stop-color="#A78BFA" stop-opacity="0.25"/>
      <stop offset="100%" stop-color="#A78BFA" stop-opacity="0"/>
    </radialGradient>
    <filter id="dglass-blur" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur in="SourceGraphic" stdDeviation="8" result="blur"/>
    </filter>
  </defs>

  <rect width="1600" height="420" fill="url(#dbg)"/>
  <rect width="1600" height="420" fill="url(#dglow1)"/>
  <rect width="1600" height="420" fill="url(#dglow2)"/>

  <!-- Glass card background (blurred copy) -->
  <g transform="translate(120,90)">
    <rect x="0" y="0" width="1360" height="240" rx="28" fill="#0B1220" opacity="0.35" filter="url(#dglass-blur)"/>
    <!-- Glass card foreground -->
    <rect x="0" y="0" width="1360" height="240" rx="28" fill="#0B1220" opacity="0.48"/>
    <rect x="0.5" y="0.5" width="1359" height="239" rx="27.5" fill="none" stroke="#93C5FD" stroke-opacity="0.35"/>
  </g>

  <!-- Title & subtitle -->
  <g transform="translate(180,175)">
    <text x="0" y="0" font-family="Inter, Segoe UI, Roboto, Helvetica, Arial, sans-serif" font-weight="800" font-size="64" fill="#F8FAFC">Jack Flight Studio</text>
    <text x="0" y="56" font-family="Inter, Segoe UI, Roboto, Helvetica, Arial, sans-serif" font-weight="500" font-size="26" fill="#E5E7EB" fill-opacity="0.85">
      Real-time flight tracking · Travel notes · LiquidGlass UI
    </text>
  </g>

  <!-- Accent pill -->
  <g transform="translate(1220,155)">
    <rect x="0" y="0" width="220" height="54" rx="27" fill="#60A5FA" opacity="0.15"/>
    <rect x="0.5" y="0.5" width="219" height="53" rx="26.5" fill="none" stroke="#60A5FA" stroke-opacity="0.45"/>
    <text x="110" y="35" text-anchor="middle" font-family="Inter, Segoe UI, Roboto, Helvetica, Arial, sans-serif" font-weight="700" font-size="20" fill="#E5E7EB">Alpha Preview</text>
  </g>
</svg>


[![Status](https://img.shields.io/badge/status-alpha-orange)](https://github.com/aliencodingjava/my-app-update/releases/tag/v0.2.213)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-IDE-3DDC84?logo=androidstudio&logoColor=white)](https://developer.android.com/studio)
[![Kotlin](https://img.shields.io/badge/Kotlin-✓-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-✓-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase-✓-3FCF8E?logo=supabase&logoColor=white)](https://supabase.com/)



**Jack Flight Studio** is an Android app for tracking flights in real time and managing travel notes.  
Built with Jetpack Compose and a custom **LiquidGlass** design, it delivers a smooth, modern experience.

> [!NOTE]
> Still in **alpha** — features and UI are evolving.

---

## ✨ Features
- Live flight tracking with dynamic map and search
- Personal notes and reminders synced with Supabase
- Beautiful glass-blur UI powered by AndroidLiquidGlass
- Dark & light themes with adaptive colors

---

## 📸 Screenshots

> Click any thumbnail to open the full image.


<p align="center"> <img src="https://github.com/user-attachments/assets/7acfdc3c-b4d8-41ec-a3d3-3ac7abee81e2" alt="Screenshot 1" width="200"/> <img src="https://github.com/user-attachments/assets/7e5030a3-e93d-4611-8e69-79c4b18c46ed" alt="Screenshot 2" width="200"/> <img src="https://github.com/user-attachments/assets/6ceca656-20d7-46f7-aab1-bb16bb9936da" alt="Screenshot 3" width="200"/> <img src="https://github.com/user-attachments/assets/27f37af1-6952-4871-825b-d53b9fc89d6e" alt="Screenshot 4" width="200"/> <img src="https://github.com/user-attachments/assets/d936f002-784b-4512-bf97-65ee000d3265" alt="Screenshot 5" width="200"/> <img src="https://github.com/user-attachments/assets/729102ec-310d-4800-843b-6ca41bfa2a91" alt="Screenshot 6" width="200"/> </p>

---

## 🧩 Tech Stack
- **Kotlin**, **Jetpack Compose**
- **Supabase** (Auth, PostgREST, Realtime)
- **AndroidLiquidGlass** for glass-blur & adaptive text color
- **MotionLayout** animations
- **Glide** image caching

---

## 🙏 Acknowledgements

Huge thanks to [**Kyant0**](https://github.com/Kyant0) for creating  
**[AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)** — the library behind the Liquid Glass effects in Jack Flight Studio.

[![AndroidLiquidGlass](https://img.shields.io/github/stars/Kyant0/AndroidLiquidGlass?label=AndroidLiquidGlass&style=flat-square)](https://github.com/Kyant0/AndroidLiquidGlass)
[![Kyant0](https://img.shields.io/badge/GitHub-Kyant0-000?logo=github&style=flat-square)](https://github.com/Kyant0)

