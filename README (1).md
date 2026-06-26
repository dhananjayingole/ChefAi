# 🍳 ChefAI — Android AI Cooking Companion

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blue?logo=android)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Auth-Firebase-orange?logo=firebase)](https://firebase.google.com)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-green)](https://developer.android.com/topic/architecture)
[![MinSDK](https://img.shields.io/badge/MinSDK-24_(Android_7)-lightgrey)](https://developer.android.com/about/versions/nougat)

> The native Android client for **NutriBot** — a multi-agent AI meal assistant. Chat with your personal AI chef, scan your fridge to auto-fill your pantry, generate Indian recipes tailored to your diet and budget, and track nutrition — all backed by a LangGraph multi-agent Python backend.

---

## 📸 Screens

| Chef AI (Chat) | Fridge Scanner | Pantry | Recipes | Nutrition |
|---|---|---|---|---|
| Talk to NutriBot, get recipes inline | Snap a photo, auto-detect & add items | Categorized inventory with quantities | AI-generated recipe with cost + macros | Calorie & macro tracking per serving |

---

## ✨ Features

- 💬 **Chef AI chat** — a conversational interface to NutriBot; ask for recipes, log groceries in plain English ("I bought 500g paneer, 1kg tomato"), or ask nutrition/budget questions
- 📷 **Fridge Scanner** — take or upload a photo of your fridge; the backend vision agent detects every item, filters by your dietary rules, and adds it straight to your pantry with a detected/added/blocked/accuracy summary
- 📦 **Pantry management** — categorized grocery list (dairy, vegetables, fruits, etc.) with quantity, manual add, swipe/tap delete, and clear-all
- 🍲 **AI recipe generation** — search or describe what you want to cook; NutriBot returns a full markdown recipe with ingredient-level ₹ pricing, respecting vegetarian/vegan/diet constraints
- 📊 **Nutrition per serving** — live macro split (protein/carbs/fat), calories, fiber, and sodium for every generated recipe
- 💰 **Cost estimate & budget tracking** — per-recipe and per-serving ₹ cost compared against your weekly budget, with a budget-used progress bar
- 🌱 **Eco impact score** — CO₂ used/saved per recipe with a letter grade, and comparisons like "vegetarian meal saves ~1.5kg CO₂ vs beef"
- ⭐ **Recipe rating** — 1–5 star rating with optional feedback text, feeding back into NutriBot's taste-learning
- 📅 **Save to meal plan** — one-tap save of any recipe as breakfast/lunch/dinner/snack, or just tell the chat "save this as dinner"
- 🎙️ **Voice input** — speak your request using Android's native SpeechRecognizer; animated mic button with live listening indicator
- 🔐 **Firebase Authentication** — email/password sign-up & login, or anonymous guest sign-in with local fallback if Firebase is unreachable
- 👤 **Profile** — view and manage your NutriBot-learned dietary profile
- 🌗 **Material 3 design** — warm saffron/herb-green "chef" palette, full Compose UI, spring-animated bottom navigation

---

## 🏗️ Architecture — MVVM

```
┌────────────────────────────────────────────────┐
│                   UI LAYER                     │
│   Jetpack Compose · Material 3 · Nav Compose   │
│                                                │
│  ChatScreen   FridgeScanScreen   PantryScreen  │
│  RecipesScreen   NutritionScreen   ProfileScreen│
│  ChatVM   FridgeScanVM   PantryVM   RecipeVM    │
│  NutritionVM   ProfileVM                        │
└───────────────────────┬─────────────────────────┘
                        │ StateFlow / collectAsState
┌───────────────────────▼─────────────────────────┐
│                  AUTH LAYER                     │
│   UserManager (Firebase Auth + SharedPrefs      │
│   fallback) · LoginScreen · AuthState           │
└───────────────────────┬─────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────┐
│                  DATA LAYER                     │
│                                                  │
│   NutriBotRepository (single source of truth)   │
│         ┌────────────┴────────────┐             │
│   ApiService (Retrofit)      ApiDtos/Models     │
│   ApiClient (OkHttp + Gson)                     │
│   → Render-deployed NutriBot backend            │
└──────────────────────────────────────────────────┘
```

No DI framework or local database here — `NutriBotRepository` is constructed directly by each ViewModel's factory, and all persistent state (pantry, profile, meal plans, nutrition history) lives server-side, keyed by Firebase `userId`. This keeps the client a thin, fast UI over the multi-agent backend.

---

## 🛠️ Tech Stack

| Category | Library / Tool |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Navigation | Navigation Compose |
| Network | Retrofit 2.9 + OkHttp logging interceptor |
| Serialization | Gson |
| Auth | Firebase Authentication (email + anonymous) + FirebaseUI |
| Camera | CameraX (camera2, lifecycle, view) |
| Permissions | Accompanist Permissions |
| Image loading | Coil |
| Voice | Android `SpeechRecognizer` (on-device) |
| Crash reporting | Firebase Crashlytics |
| Async | Coroutines + StateFlow |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## 📂 Project Structure

```
app/src/main/java/eu/tutorials/chefproj/
│
├── MainActivity.kt                # NavHost, auth-state routing, bottom nav scaffold
├── NutriBotApplication.kt
│
├── Auth/
│   ├── LoginScreen.kt              # Email/password + anonymous sign-in UI
│   └── UserManager.kt              # Firebase Auth wrapper, AuthState, SharedPrefs fallback
│
├── Data/
│   ├── api/
│   │   ├── ApiClient.kt            # Retrofit + OkHttp setup, base URL
│   │   ├── ApiService.kt           # All backend endpoints (chat, pantry, recipes, vision...)
│   │   └── Models.kt               # Request/response DTOs
│   └── repository/
│       └── NutriBotRepository.kt   # Single source of truth, wraps ApiService calls
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                 # Chef Orange / Herb Green palette
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── components/
│   │   ├── BottomNavigationBar.kt   # Chef AI · Pantry · Recipes · Nutrition · Profile
│   │   ├── ChatBubble.kt
│   │   ├── BudgetCard.kt
│   │   ├── EcoCard.kt
│   │   ├── NutritionCard.kt
│   │   └── LoadingIndicator.kt
│   ├── screens/
│   │   ├── ChatScreen.kt            # Chef AI conversational interface
│   │   ├── FridgeScanScreen.kt      # Photo capture/upload → vision agent → pantry
│   │   ├── PantryScreen.kt          # Categorized inventory CRUD
│   │   ├── RecipesScreen.kt         # Search/generate, nutrition, cost, save, rate
│   │   ├── NutritionScreen.kt       # Daily/weekly macro tracking
│   │   ├── ProfileScreen.kt         # Dietary profile view/edit
│   │   └── VoiceInputButton.kt      # SpeechRecognizer mic button + listening UI
│   └── viewmodels/
│       ├── ChatViewModel.kt
│       ├── FridgeScanViewModel.kt
│       ├── PantryViewModel.kt
│       ├── RecipeViewModel.kt
│       ├── NutritionViewModel.kt
│       └── ProfileViewModel.kt
```

---

## 🚀 Setup & Run

### Prerequisites
- Android Studio (latest stable)
- Android device / emulator (API 24+)
- A Firebase project with Authentication enabled (or use your own `google-services.json`)
- Backend deployed (or running locally) — see [Backend Connection](#-backend-connection)

### Steps

```bash
# 1. Clone
git clone https://github.com/dhananjayingole/ChefAi.git

# 2. Open in Android Studio
# File → Open → select the project folder

# 3. Add your own google-services.json
# Place it in app/google-services.json (Firebase Console → Project Settings)

# 4. Wait for Gradle sync

# 5. (Optional) Point to your own backend
# Edit BASE_URL in Data/api/ApiClient.kt

# 6. Run on device or emulator
# Press ▶ or Shift+F10
```

---

## 🔌 Backend Connection

This app is the Android client for the [NutriBot backend](https://github.com/dhananjayingole/mgmhackthon) — a Streamlit-orchestrated, LangGraph-driven multi-agent system (Memory, Intent Router, Recipe, Pantry, Nutrition, Budget, Eco, Health, Planner, Shopping, Cooking, Taste agents) running on Groq LLaMA 3.3.

| Setting | Value |
|---|---|
| API Base URL | `https://mgmhackthon-2.onrender.com/` |
| Timeout (read/write) | 60 seconds (AI responses + image analysis take time) |
| Timeout (connect) | 30 seconds |
| Auth | Firebase `userId` passed as query param / body field on every request |

Key endpoints consumed by this client: `/chat`, `/pantry`, `/recipe/generate`, `/recipe/rate`, `/mealplan`, `/nutrition/today`, `/budget/*`, `/fridge/scan`, `/eco/calculate`, `/profile/{user_id}`.

---

## 🎨 UI Design Decisions

### Chef palette
| Role | Color | Hex |
|---|---|---|
| Primary (Saffron/Turmeric) | ChefOrange | `#E8541E` |
| Secondary (Herb Green) | HerbGreen | `#3D7A4A` |
| Background | Cream | `#FFF8F0` |
| Accent | Terracotta / Gold | `#CC5C3A` / `#D4A017` |

### Macro colors
Protein `#3B82F6` · Carbs `#16A34A` · Fat `#9333EA` · Fiber `#0D9488` · Sodium `#EA580C`

### Animations
- Bottom nav: **spring bounce** on tab select (`dampingRatio = MediumBouncy`)
- Voice button: **pulsing radial gradient** while listening, animated mic ↔ stop icon
- Chat: streamed AI responses rendered as markdown bubbles with inline nutrition/budget/eco cards

---

## 🧪 Testing the App

Once running and signed in (or as a guest), try:

| Action | Where |
|---|---|
| "I bought 500g paneer, 1kg tomato, 1kg potato" | Chef AI chat → auto-adds to Pantry |
| "Make me palak paneer" | Chef AI chat or Recipes tab → full recipe + macros + cost |
| Upload a fridge photo | Fridge Scanner → detected items added with confidence % |
| "Save this as dinner" | Chef AI chat → saved to today's meal plan |
| Rate a recipe 5 stars | Recipes tab → feeds NutriBot's taste learning |

---

## 📈 What This Demonstrates to Recruiters

| Android Skill | Shown By |
|---|---|
| **Jetpack Compose** | All 6 screens fully in Compose |
| **Material 3** | Custom theme, animated nav, cards, dialogs |
| **MVVM** | ViewModel + StateFlow per screen, factory-injected repository |
| **Retrofit + OkHttp** | Full multi-agent backend integration incl. multipart image upload |
| **Firebase Auth** | Email/password + anonymous sign-in with local fallback |
| **CameraX** | Fridge photo capture pipeline |
| **Android SpeechRecognizer** | On-device voice input with permission handling |
| **Navigation Compose** | Bottom-nav-driven NavHost |
| **Coroutines + Flow** | StateFlow, viewModelScope, async repository calls |
| **AI Integration** | Real multi-agent LLM backend (recipes, vision, nutrition, budget, eco) |

---

## 🤝 Related

🧠 **Backend** → [mgmhackthon (NutriBot)](https://github.com/dhananjayingole/mgmhackthon)
Python · Streamlit · LangGraph multi-agent pipeline · Groq LLaMA 3.3 · USDA FoodData Central

---

## 📄 License

MIT — free to use, modify and distribute.
