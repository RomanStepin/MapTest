package com.example.maptest

import android.app.Application

class App: Application() {

    init {
        instance = this
    }

    companion object {
        public lateinit var instance: App
    }

}