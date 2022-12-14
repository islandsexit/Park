package ru.vigtech.android.vigpark.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import java.util.*
import java.util.concurrent.Executors



class CrimeRepository private constructor(context: Context) {
    private val DATABASE_NAME = "crime-database"

    private val database : CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(migration_1_2).addMigrations((migration_2_3))
        .addMigrations(migration_3_4)
        .addMigrations(migration_4_5)
        .addMigrations(migration_5_6)
        .addMigrations(migration_6_7)
        .addMigrations(migration_7_8)
        .addMigrations(migration_8_9)
        .addMigrations(migration_9_10)
        .build()
    private val crimeDao = database.crimeDao()
    private val executor = Executors.newSingleThreadExecutor()

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()
    fun getUnsendCrimes(): LiveData<List<Crime>> = crimeDao.getUnsendCrimes()
    fun getUnsendAndNotRememberesCrimes():LiveData<List<Crime>> = crimeDao.getUnsendAndNotRememberedCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)



    fun getCrimeFromPosition(position: Int): Crime = crimeDao.getCrimeFromPosition(position)

    fun deleteCrime(crime: Crime) = crimeDao.deleteCrime(crime)

    fun updateCrime(crime: Crime) {
        executor.execute {
            crimeDao.updateCrime(crime)
        }
    }

    fun addCrime(crime: Crime) {
        executor.execute {
            crimeDao.addCrime(crime)
        }
    }
    
    companion object {
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?:
            throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}