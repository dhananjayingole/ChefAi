package eu.tutorials.chefproj

import android.app.Application
import com.google.firebase.FirebaseApp

class NutriBotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}