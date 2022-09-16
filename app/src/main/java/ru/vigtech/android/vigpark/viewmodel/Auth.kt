package ru.vigtech.android.vigpark.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import androidx.work.impl.utils.Preferences
import ru.vigtech.android.vigpark.api.ApiClient
import java.util.*


class Auth: ViewModel(){
    private val VERSION = "1.0"
    private val UUIDKEY = "uuidKey"
    private val SECUREKEY = "secureKey"
    private val AUTHSUCCESS = "authSuccess"
    private val LISTOFALIAS = "listOfAlias"
    private val APPVERSION = "listOfAlias"

    lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var editor:SharedPreferences.Editor


    var version: MutableLiveData<String> = MutableLiveData(VERSION)
        set(value) {
            field = value
            editor.putString(APPVERSION, value.value!!).commit()
        }


    var authSuccess: MutableLiveData<Int> = MutableLiveData()
        set(value) {
            field = value
            editor.putInt(AUTHSUCCESS, value.value!!).commit()
        }

    var listOfAlias: MutableLiveData<Set<String>> = MutableLiveData()
        set(value){
            field = value

        }

    var aliveSwitch:MutableLiveData<Boolean> = MutableLiveData(false)


    fun MutableLiveData<Int>.savePreferences(num:Int){

        this.postValue(num)
        editor.putInt(AUTHSUCCESS, num).commit()
    }

    fun savePreferences(setOfString: Set<String>) {
        listOfAlias.postValue(setOfString)
        editor.putStringSet(LISTOFALIAS, setOfString).commit()
    }


    var uuidKey: String = ""
        get() = preferences.getString(UUIDKEY, "").toString()
        set(value){
            field = value
            editor.putString(UUIDKEY, value).commit()
        }
    var secureKey: String = ""
        get() = preferences.getString(SECUREKEY, "").toString()
        set(value) {
            field = value
            editor.putString(SECUREKEY, value).commit()
        }



            fun initViewModel(){
                preferences =
                    PreferenceManager.getDefaultSharedPreferences(
                        context
                    )

                editor = preferences.edit()

                if (uuidKey == ""){
                    uuidKey = UUID.randomUUID().toString()
                    editor.putString(UUIDKEY, uuidKey).commit()
                }

                authSuccess = MutableLiveData(preferences.getInt(AUTHSUCCESS, 1))
                listOfAlias = MutableLiveData(preferences.getStringSet(LISTOFALIAS, setOf("1", "2", "3")))


                Log.i("AUTHC", "uuidKey: ${preferences.getString(UUIDKEY, "none")}, authSucess: ${preferences.getInt(
                    AUTHSUCCESS, 4)}, secKey:${preferences.getString(SECUREKEY, "none")}, listOfAlias: ${preferences.getStringSet(LISTOFALIAS, setOf("none"))}")
            }

            fun onCheckLicence(isSuccess: Boolean){
                when(isSuccess){
                    true -> authSuccess.savePreferences(3)
                    false -> when(authSuccess.value){
                        1-> authSuccess.savePreferences(2)
                        2, 3-> authSuccess.savePreferences(1)

                    }
                }


                Log.i("AUTHC", "uuidKey: ${preferences.getString(UUIDKEY, "none")}, authSucess: ${preferences.getInt(
                    AUTHSUCCESS, 4)}, secKey:${preferences.getString(SECUREKEY, "none")}")

            }
        }









