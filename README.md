## ✨ What’s New (Now Available)

# ✨ Flights Studio — UI & Animation Update

The latest UI and animation improvements have been successfully implemented and are now live in the app.

---

## ✨ Major Enhancements

### 🎨 Glass & Visual System
- Refined **glassmorphism rendering** with improved blur diffusion and light scattering
- Enhanced **chromatic refraction & lens depth** across buttons and surfaces
- Adaptive luminance highlights for better contrast in both Light and Dark modes
- Warmer surface blending in Light mode to prevent over-bright glass

### 🧠 AI & Smart Interactions
- **AI-Powered Title Suggestions**
  - Real-time Gemini title placeholders
  - Shimmering adaptive preview text
  - Tap-to-accept or manual override
  - Context-aware dynamic icon styling

### 🚀 Motion & Physics

- Migrated `NoteItem` to the new **InteractiveHighlight system**
  - Faster press scaling response
  - Spring-based 3D lens deformation
  - Magnetic drag offsets
  - Improved snap-back timing

- Implemented **OffsetOverscrollEffect**
  - Tanh-based rubber-band physics
  - Continuous stretch without hard stop
  - Samsung-style elastic bounce

- Refined screen transitions for smoother visual continuity

---

## 📱 Feature Additions

### 🪟 Notes Onboarding Overlay
- Introduced `NotesWelcomeOnboardingOverlay`
- Glass-morphic feature grid
- Animated highlights
- First-launch smart detection

### 🎥 Liquid Gallery Improvements
- Draggable video menu with elastic reveal
- Fluid drag gestures on image previews
- Improved glass elevation animation for sheets & menus

---

## ⚡ Performance Improvements

- Optimized image URI loading using `produceState + Dispatchers.IO`
- Rebuilt `ShimmerThinkingText` using `drawWithCache`
- Reduced unnecessary recompositions in note list
- Improved compact mode density

---

## 🛠 Fixes & Polish

- Fixed pill split “fly-back” state issue
- Added haptic feedback refinement
- Standardized `ProfileBackdropImageLayer` across note screens
- Unified grid background resources (Light/Dark optimized)
- Minor layout and spacing refinements

---

## 📦 Dependency Updates

- `io.github.kyant0:backdrop` → `1.0.6`
- Compose Material3 → `1.5.0-alpha14`
- Activity → `1.12.4`
- Other AndroidX libraries updated

---

## ℹ️ Update Notes

- AI title suggestions require network connectivity.
- Onboarding overlay appears only for empty note lists.
- Improved responsiveness across lists and gestures.


---

Working on next update

---

> ℹ️ **Note:**  
> The section above highlights **upcoming / in-progress features**.  
> Everything below reflects the **current, existing implementation**.

---
## 📸 Preview

<table align="center" cellspacing="6" cellpadding="0">
  <tr>
    <td><img src="https://github.com/user-attachments/assets/a210e782-1011-471a-b828-f1516eed4939" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/cf8ac6bd-9c19-4437-b75a-ac5ef36ea4d8" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/cd7f5624-0b57-49ad-ab71-da1ba0794b6a" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/3fcaae20-848d-4f8c-98e0-14db771155d0" width="200"/></td>
  </tr>

  <tr>
    <td><img src="https://github.com/user-attachments/assets/4ca04e57-b8f4-46dd-819a-c507ef191c31" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/ee5b4f8c-b4fc-47dd-a7e2-c6af9087b4ae" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/ccd14678-1270-4a26-9bc8-c5ec4329b82a" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/85f01932-bdce-4069-9636-b2f1294ded68" width="200"/></td>
  </tr>

  <tr>
    <td><img src="https://github.com/user-attachments/assets/5ba7da30-c838-4f80-a14a-81548684a7b9" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/134b1219-51fe-42f5-ba97-11666e26e69f" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/bb712e7b-c25a-41a0-91bb-cf31513a16d2" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/53fecc52-410d-44e2-a2f7-ff703a090c50" width="200"/></td>
  </tr>
</table>


---

<div align="center">
  <h1>✈️ Jack Flight Studio</h1>
  <p>
    <a href="#status"><img alt="Status" src="https://img.shields.io/badge/status-stable-brightgreen"></a>
    <a href="#tech-stack"><img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white"></a>
    <a href="#tech-stack"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-%E2%9C%93-7F52FF?logo=kotlin&logoColor=white"></a>
    <a href="#tech-stack"><img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-%E2%9C%93-4285F4?logo=jetpackcompose&logoColor=white"></a>
    <a href="#tech-stack"><img alt="Supabase" src="https://img.shields.io/badge/Supabase-%E2%9C%93-3FCF8E?logo=supabase&logoColor=white"></a>
  </p>
  <p><strong>Jack Flight Studio</strong> is an Android app for tracking flights in real time and managing travel notes.<br>
  Built with Jetpack Compose and a custom <strong>LiquidGlass</strong> design for a smooth, modern experience.</p>
</div>

<hr>

<h2 id="status">🚥 Status</h2>
<p><strong>Stable:</strong> ready for everyday use. Ongoing updates focus on performance, UX polish, and new features without breaking core workflows.</p>

<hr>

<h2>✨ Features</h2>
<ul>
  <li>Live flight tracking with dynamic map and search</li>
  <li>Personal notes and reminders synced with Supabase</li>
  <li>Beautiful glass-blur UI powered by AndroidLiquidGlass</li>
  <li>Dark &amp; light themes with adaptive colors</li>
</ul>

<hr>

<h2 id="tech-stack">🧩 Tech Stack</h2>
<ul>
  <li><strong>Kotlin</strong>, <strong>Jetpack Compose</strong></li>
  <li><strong>Supabase</strong> (Auth, PostgREST, Realtime)</li>
  <li><strong>AndroidLiquidGlass</strong> for glass-blur &amp; adaptive text color</li>
  <li><strong>MotionLayout</strong> animations</li>
  <li><strong>Glide</strong> image caching</li>
</ul>

<hr>

<hr>

<div align="center">
  <h3>🙏 Credits &amp; Thanks</h3>
  <p>
    Glass effects are powered by
    <a href="https://github.com/Kyant0/AndroidLiquidGlass"><strong>AndroidLiquidGlass</strong></a>
    by <a href="https://github.com/Kyant0"><strong>@Kyant0</strong></a>.
  </p>

  <!-- "Love" actions -->
  <p>
    <!-- Star the repo -->
    <a href="https://github.com/Kyant0/AndroidLiquidGlass" title="Star AndroidLiquidGlass">
      <img alt="Star" src="https://img.shields.io/github/stars/Kyant0/AndroidLiquidGlass?style=social">
    </a>
    <!-- Follow the author -->
    <a href="https://github.com/Kyant0" title="Follow @Kyant0 on GitHub">
      <img alt="Follow @Kyant0" src="https://img.shields.io/badge/Follow-@Kyant0-181717?logo=github&logoColor=white">
    </a>
    <!-- Watch updates -->
<a href="https://github.com/Kyant0/AndroidLiquidGlass/subscription" title="Watch for updates">
  <img alt="Watch updates" src="https://img.shields.io/badge/Watch-updates-1f6feb?logo=github&logoColor=white">
</a>
    <!-- Say thanks (opens a prefilled issue) -->
    <a href="https://github.com/Kyant0/AndroidLiquidGlass/issues/new?title=Thanks%20for%20AndroidLiquidGlass&body=%F0%9F%92%99%20Thanks%20for%20AndroidLiquidGlass!%20It%20helped%20me%20with%20...">
      <img alt="Say thanks" src="https://img.shields.io/badge/Say%20thanks-%F0%9F%92%9A-blueviolet">
    </a>
  </p>

  <p>
    <a href="https://github.com/Kyant0">
      <img src="https://github.com/Kyant0.png?size=96" width="72" height="72" alt="@Kyant0 avatar">
    </a>
  </p>
</div>


