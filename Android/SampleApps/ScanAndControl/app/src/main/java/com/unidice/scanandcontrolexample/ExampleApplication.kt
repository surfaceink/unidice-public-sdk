package com.unidice.scanandcontrolexample

import android.app.Application
import android.content.Context
import com.unidice.sdk.internal.UnidiceControllerBase

class ExampleApplication : Application() {

    // Manages our game's connection to the Unidice
    private val unidiceControllerBase = UnidiceControllerBase()

    fun getUnidiceController(): UnidiceControllerBase {
        return unidiceControllerBase
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
        unidiceControllerBase.initController(this)
    }
}