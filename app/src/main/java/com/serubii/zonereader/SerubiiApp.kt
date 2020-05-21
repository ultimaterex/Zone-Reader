package com.serubii.zonereader

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp

class SerubiiApp : Application() {

    init {
        instance = this
    }


    companion object {
        private var instance: SerubiiApp? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }


    override fun onCreate() {
        super.onCreate()
        // initialize for any

        // Use ApplicationContext.
        // example: SharedPreferences etc...
        val context: Context = applicationContext()


        // FIREBASE INITIALIZATION
        FirebaseApp.initializeApp(context)

    }

}