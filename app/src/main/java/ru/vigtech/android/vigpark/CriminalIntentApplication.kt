package ru.vigtech.android.vigpark

import android.app.Application
import ru.vigtech.android.vigpark.database.CrimeRepository
import ru.vigtech.globalcrashhandler.handler.GlobalExceptionHandler

class CriminalIntentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
        GlobalExceptionHandler.init(this,MainActivity::class.java)
    }
}