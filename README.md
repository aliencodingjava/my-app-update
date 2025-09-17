<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/banner-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/banner-light.svg">
  <img alt="Jack Flight Studio banner" src="docs/banner-light.svg">" width="100%">
</picture>

# ✈️ Jack Flight Studio

[![Status](https://img.shields.io/badge/status-alpha-orange)](#-status)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#-getting-started)
[![Kotlin](https://img.shields.io/badge/Kotlin-✓-7F52FF?logo=kotlin&logoColor=white)](#-tech-stack)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-✓-4285F4?logo=jetpackcompose&logoColor=white)](#-tech-stack)
[![Supabase](https://img.shields.io/badge/Supabase-✓-3FCF8E?logo=supabase&logoColor=white)](#-tech-stack)

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


| <a href="https://github.com/user-attachments/assets/7acfdc3c-b4d8-41ec-a3d3-3ac7abee81e2"><img src="https://github.com/user-attachments/assets/7acfdc3c-b4d8-41ec-a3d3-3ac7abee81e2" alt="Home — LiquidGlass bottom bar" width="220" loading="lazy"/></a><br/><sub>Home — LiquidGlass bottom bar</sub> | <a href="https://github.com/user-attachments/assets/7e5030a3-e93d-4611-8e69-79c4b18c46ed"><img src="https://github.com/user-attachments/assets/7e5030a3-e93d-4611-8e69-79c4b18c46ed" alt="Flight search" width="220" loading="lazy"/></a><br/><sub>Flight search</sub> | <a href="https://github.com/user-attachments/assets/6ceca656-20d7-46f7-aab1-bb16bb9936da"><img src="https://github.com/user-attachments/assets/6ceca656-20d7-46f7-aab1-bb16bb9936da" alt="Map — live tracking" width="220" loading="lazy"/></a><br/><sub>Map — live tracking</sub> |
| <a href="https://github.com/user-attachments/assets/27f37af1-6952-4871-825b-d53b9fc89d6e"><img src="https://github.com/user-attachments/assets/27f37af1-6952-4871-825b-d53b9fc89d6e" alt="Notes list" width="220" loading="lazy"/></a><br/><sub>Notes list</sub> | <a href="https://github.com/user-attachments/assets/d936f002-784b-4512-bf97-65ee000d3265"><img src="https://github.com/user-attachments/assets/d936f002-784b-4512-bf97-65ee000d3265" alt="Note details — image viewer" width="220" loading="lazy"/></a><br/><sub>Note details — image viewer</sub> | <a href="https://github.com/user-attachments/assets/729102ec-310d-4800-843b-6ca41bfa2a91"><img src="https://github.com/user-attachments/assets/729102ec-310d-4800-843b-6ca41bfa2a91" alt="Reminders — bottom sheet" width="220" loading="lazy"/></a><br/><sub>Reminders — bottom sheet</sub> |

---

## 🧩 Tech Stack
- **Kotlin**, **Jetpack Compose**
- **Supabase** (Auth, PostgREST, Realtime)
- **AndroidLiquidGlass** for glass-blur & adaptive text color
- **MotionLayout** animations
- **Glide** image caching

---

🙏 Acknowledgements

Huge thanks to Kyant0
for AndroidLiquidGlass
— the library behind the Liquid Glass effects in Jack Flight Studio.
