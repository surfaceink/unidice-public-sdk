package com.unidice.scanandcontrolexample

import android.app.Application
import android.content.Context
import com.unidice.sdk.api.UnidiceController

class ExampleApplication : Application() {

    // Manages our game's connection to the Unidice
    private val unidiceController = UnidiceController()

    fun getUnidiceController(): UnidiceController {
        return unidiceController
    }

    init {
        instance = this
    }

    companion object {
        private var instance: ExampleApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        unidiceController.initController(this)
    }
}