package ru.vigtech.android.vigpark.api


import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.google.android.gms.tasks.OnFailureListener
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.vigtech.android.vigpark.viewmodel.Auth
import ru.vigtech.android.vigpark.MainActivity
import ru.vigtech.android.vigpark.api.PostInterface
import ru.vigtech.android.vigpark.api.PostPhoto
import ru.vigtech.android.vigpark.database.Crime
import ru.vigtech.android.vigpark.database.CrimeRepository
import ru.vigtech.android.vigpark.fragment.CrimeListFragment
import ru.vigtech.android.vigpark.tools.Diagnostics
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.coroutineContext


//@SuppressLint("StaticFieldLeak")
object ApiClient {
    var baseUrl = "http://95.182.74.37:1234/"
    var retrofit: Retrofit = getRetroInstance(baseUrl)

    lateinit var authModel: Auth
    lateinit var okHttpClient: OkHttpClient






    private fun getRetroInstance(baseUrl: String): Retrofit {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(29, TimeUnit.SECONDS)
            .readTimeout(29, TimeUnit.SECONDS)
            .writeTimeout(29, TimeUnit.SECONDS)
            .build()
            val gsonBuilder = GsonBuilder()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                .build()
        }

    fun reBuildRetrofit(ip: String){
        retrofit = getRetroInstance(ip)
        baseUrl = ip
    }

    fun cancelApiCalls(){
        okHttpClient.dispatcher.cancelAll()
    }



    fun POST_img64( img64_full: String, img_path: String, img_plate_path:String, zone:Int,long: Double, lat:Double, ) {
        val crime = Crime(title = "Отправка на сервер", img_path = img_path, img_path_full = img_plate_path, send = true, found = true, Zone=zone, lon = long, lat = lat, Rect = ArrayList<String?>())
        CrimeRepository.get().addCrime(crime)
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        Log.w("Create", "create $crime")
        val call: Call<PostPhoto> = post_api.postPlate(img64_full,zone, long, lat, authModel.uuidKey, authModel.secureKey,
            SimpleDateFormat("YY-MM-dd HH:mm:ss").format(crime.date))

        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        val info = POST_PHOTO?.info.toString()
                        val Rect = POST_PHOTO?.Rect
                        Log.w("POST", "onResponse| response: Result: $RESULT msg: $msg, -- , $crime")
                        if (RESULT == "SUCCESS") {
                            crime.title = msg
                            crime.info = info
                            crime.send = true
                            crime.found = true
                            if ((crime.Rect?.size ?: 1) != 4){
                                crime.Rect = Rect
                            }

                            CrimeRepository.get().updateCrime(crime)
                        } else if (RESULT == "INVALID"){
                            crime.title = "Ошибка лицензирования"
                            crime.send = true
                            crime.found = true
                            crime.info = "Данное программное обеспечение защищено правом пользования. Произошла ошибка в ключе лицензирования при проверк на сервере. Просьба использовать программу в соответсвии договра пользования."
                            authModel.onCheckLicence(false)
                            CrimeRepository.get().updateCrime(crime)
                        }
                        else if (RESULT == "WARNING"){
                            crime.title = "Ошибка на сервере"
                            crime.send = true
                            crime.found = true
                            crime.info = "Произошла ошибка на стороне сервера. Пожалуйста позвоните системному администратору и уведомите его. До звонка прошу не продолжать фиксирование."
                            CrimeRepository.get().updateCrime(crime)
                        }else{
                            crime.title = "Не распознан номер"
                            crime.send = true
                            crime.found = false
                            CrimeRepository.get().updateCrime(crime)
                        }

                        logApi(POST_PHOTO, crime)

                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        crime.title = "Сервер недоступен"
                        crime.send = false
                        crime.found = false
                        CrimeRepository.get().updateCrime(crime)
                        logApi(crime = crime)
                    }
                } catch (e: Exception) {
                    Diagnostics.appendLog("${SimpleDateFormat("YYMMdd").format(Date())}_Api", "crime:$crime;uuid:${authModel.uuidKey};secureKey:${authModel.secureKey};isAuthSuccess:${authModel.authSuccess.value};error:$e")
                    Log.e("POST", "onResponse | exception", e)
                    crime.title = "Сервер недоступен"
                    crime.send = false
                    crime.found = false
                    CrimeRepository.get().updateCrime(crime)
                    logApi(crime=crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                crime.title = "Сервер недоступен"
                crime.send = false
                crime.found = false
                CrimeRepository.get().updateCrime(crime)
                authModel.aliveSwitch.postValue(false)
                logApi(crime=crime, onFailure = true)
            }
        })


    }

    fun POST_img64(img64: String, crime: Crime) {
        crime.title = "Отправка на сервер"
        crime.send = true
        crime.found = true
        crime.info = ""
        CrimeRepository.get().updateCrime(crime)
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val call: Call<PostPhoto> = post_api.postPlate(img64,crime.Zone, crime.lon, crime.lat, authModel.uuidKey, authModel.secureKey,SimpleDateFormat("YY-MM-dd HH:mm:ss").format(crime.date))
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {

                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        val info = POST_PHOTO?.info.toString()
                        val Rect = POST_PHOTO?.Rect
                        Log.w("POST", "onResponse| response: Result: $RESULT msg: $msg")
                        if (RESULT == "SUCCESS") {
                            crime.send = true
                            crime.title = msg
                            crime.info = info
                            crime.found = true
                            if ((crime.Rect?.size ?: 1) != 4){
                                crime.Rect = Rect
                            }
                            CrimeRepository.get().updateCrime(crime)
                        } else if (RESULT == "INVALID"){
                            crime.title = "Ошибка лицензирования"
                            crime.send = true
                            crime.found = true
                            crime.info = "Данное программное обеспечение защищено правом пользования. Произошла ошибка в ключе лицензирования при проверк на сервере. Просьба использовать программу в соответсвии договра пользования."
                            authModel.onCheckLicence(false)
                            CrimeRepository.get().updateCrime(crime)
                        }else if (RESULT == "WARNING"){
                            crime.title = "Ошибка на сервере"
                            crime.send = true
                            crime.found = true
                            crime.info = "Произошла ошибка на стороне сервера. Пожалуйста позвоните системному администратору и уведомите его. До звонка прошу не продолжать фиксирование."
                            CrimeRepository.get().updateCrime(crime)
                        }else {
                            crime.send = true
                            crime.title = "Не распознан номер"
                            crime.found = false
                            crime.info = "null"
                            CrimeRepository.get().updateCrime(crime)
                        }
                        logApi(POST_PHOTO, crime)

                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        crime.send = false
                        crime.title  = "Сервер недоступен"
                        crime.info = ""
                        CrimeRepository.get().updateCrime(crime)
                        logApi(crime=crime)
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    crime.send = false
                    crime.info=""
                    crime.title  = "Сервер недоступен"
                    CrimeRepository.get().updateCrime(crime)
                    Diagnostics.appendLog("${SimpleDateFormat("YYMMdd").format(Date())}_Api", "crime:$crime;uuid:${authModel.uuidKey};secureKey:${authModel.secureKey};isAuthSuccess:${authModel.authSuccess.value.toString()};error:$e")
                    logApi(crime=crime)
                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                crime.send = false
                crime.title = "Сервер недоступен"
                crime.info = ""
                CrimeRepository.get().updateCrime(crime)
                authModel.aliveSwitch.postValue(false)
                logApi(crime=crime, onFailure = true)
            }
        })


    }


    fun POST_img64_with_edited_text(img64: String,img64_full: String, crime: Crime) {
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        crime.send = true
        crime.found = true
        val new_plate = crime.title
        crime.title = "Отправка на сервер"
        crime.info = ""
        CrimeRepository.get().updateCrime(crime)
        val call: Call<PostPhoto> =
            post_api.postPlateEdited(img64, new_plate, crime.Zone, crime.lon, crime.lat, authModel.uuidKey, authModel.secureKey,SimpleDateFormat("YY-MM-dd HH:mm:ss").format(crime.date))
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {

                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        val info = POST_PHOTO?.info.toString()
                        val Rect = POST_PHOTO?.Rect
                        Log.w(
                            "POST_img64_with_edited_text",
                            "onResponse| response: Result: $RESULT msg: $msg"
                        )
                        if (RESULT == "SUCCESS") {
                            crime.send = true
                            crime.found = true
                            if (crime.title == "") {
                                crime.title = msg
                            } else {
                                crime.title = new_plate
                            }
                            if ((crime.Rect?.size ?: 1) != 4) {
                                crime.Rect = Rect
                            }
                            crime.info = info
                            CrimeRepository.get().updateCrime(crime)
                        } else if (RESULT == "INVALID") {
                            crime.title = "Ошибка лицензирования"
                            crime.send = true
                            crime.found = true
                            crime.info =
                                "Данное программное обеспечение защищено правом пользования. Произошла ошибка в ключе лицензирования при проверк на сервере. Просьба использовать программу в соответсвии договра пользования."
                            authModel.onCheckLicence(false)
                            CrimeRepository.get().updateCrime(crime)
                        }else if (RESULT == "WARNING"){
                            crime.title = "Ошибка на сервере"
                            crime.send = true
                            crime.found = true
                            crime.info = "Произошла ошибка на стороне сервера. Пожалуйста позвоните системному администратору и уведомите его. До звонка прошу не продолжать фиксирование."
                            CrimeRepository.get().updateCrime(crime)
                        } else {
                            if (crime.title == "") {
                                crime.send = true
                                crime.info = ""
                                crime.found = true
                                crime.title = "Не распознан номер"
                            } else {
                                crime.send = true
                                crime.found = true
                                crime.info = ""
                                crime.title = new_plate
                            }
                            CrimeRepository.get().updateCrime(crime)
                        }
                        logApi(POST_PHOTO, crime)

                    } else {
                        crime.send = false
                        if (crime.title == "") {
                            crime.title = "Сервер недоступен"
                        } else {
                            crime.title = new_plate
                        }
                        crime.found = false
                        crime.info = ""
                        CrimeRepository.get().updateCrime(crime)
                        Log.e("POST_img64_with_edited_text", "onResponse | status: $statusCode")

                    }
                } catch (e: Exception) {

                    Log.e("POST_img64_with_edited_text", "onResponse | exception", e)
                    crime.send = false
                    if (crime.title == "") {
                        crime.title = "Сервер недоступен"
                    } else {
                        crime.title = new_plate
                    }
                    crime.info = ""
                    crime.found = false
                    CrimeRepository.get().updateCrime(crime)
                    logApi(crime = crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                authModel.aliveSwitch.postValue(false)
                Log.e("POST_img64_with_edited_text", "onFailure", t)
                crime.send = false
                crime.found = false
                if (crime.title == "") {
                    crime.title = "Сервер недоступен"
                } else {
                    crime.title = new_plate
                }
                crime.info = ""
                CrimeRepository.get().updateCrime(crime)
                logApi(crime = crime, onFailure = true)
            }
        })
    }

        fun postAuthKeys() {
            val post_api: PostInterface = retrofit.create(PostInterface::class.java)
            val call: Call<PostPhoto> = post_api.postPlate("testKey",0, 0.0, 0.0, authModel.uuidKey, authModel.secureKey,SimpleDateFormat("YY-MM-dd HH:mm:ss").format(
                Date()
            ))
            call.enqueue(object : Callback<PostPhoto?> {
                override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                    try {
                        val statusCode = response.code()
                        if (statusCode == 200) {
                            val POST_PHOTO: PostPhoto? = response.body()
//                            val data_get: List<PostPhoto> = PostPhoto.getResponse()
                            val RESULT = POST_PHOTO?.RESULT.toString()
                            Log.w("POST_img64_with_edited_text", "onResponse| response: Result: $RESULT")
                            if (RESULT == "SUCCESS") {
                                authModel.onCheckLicence(true)
                                runOnUiThread {
                                    Toast.makeText(authModel.context, "Лицензия Активирована", Toast.LENGTH_LONG ).show()
                                }
                            }else if (RESULT == "INVALID"){
                                authModel.onCheckLicence(false)
                                runOnUiThread {
                                    Toast.makeText(authModel.context, "Лицензия не прошла", Toast.LENGTH_LONG ).show()
                                }

                        } else{
                                authModel.onCheckLicence(true)
                                runOnUiThread {
                                    Toast.makeText(authModel.context, "Лицензия Активирована", Toast.LENGTH_LONG ).show()
                                }

                            }
                            logApi(POST_PHOTO)

                        } else {
                            authModel.onCheckLicence(false)
                            runOnUiThread {
                                Toast.makeText(authModel.context, "Лицензия не прошла", Toast.LENGTH_LONG ).show()
                            }
                            logApi()
                        }
                    } catch (e: Exception) {
                        Log.e("POST_img64_with_edited_text", "onResponse | exception", e)

                        runOnUiThread {
                            Toast.makeText(authModel.context, "Ошибка приложения", Toast.LENGTH_LONG ).show()
                        }
                        authModel.onCheckLicence(false)
                        logApi()
                    }
                }

                override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                    Log.e("POST_img64_with_edited_text", "onFailure", t)

                    authModel.onCheckLicence(false)
                    runOnUiThread {
                        Toast.makeText(authModel.context, "Ошибка Сервера", Toast.LENGTH_LONG ).show()
                    }
                    logApi(onFailure = true)
                }
            })


    }

    fun checkZone(){
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)

        val call: Call<PostPhoto> =
            post_api.zoneCheck(authModel.uuidKey, authModel.secureKey)
        call.enqueue(object : Callback<PostPhoto?> {
            override fun onResponse(call: Call<PostPhoto?>, response: Response<PostPhoto?>) {
                try {
                    val statusCode = response.code()
                    if (statusCode == 200) {
                        val POST_PHOTO: PostPhoto? = response.body()
                        val RESULT = POST_PHOTO?.RESULT.toString()
                        val info = POST_PHOTO?.info.toString()
                        val msg = POST_PHOTO?.palteNumber.toString()
                        val version = POST_PHOTO?.version
                        Log.e(
                            "POST_CHECK_ZONE",
                            "onResponse| response: Result: $RESULT, info: $info "
                        )
                        if (RESULT == "SUCCESS") {
                            authModel.aliveSwitch.postValue(true)
                            val new_zones = info.split(";").toSet()
                            if(new_zones != authModel.listOfAlias.value){

                                authModel.savePreferences(new_zones)
                            }


                            if(version != Auth.VERSION && version!=null){
                                authModel.version.postValue(version)

                            }

                        } else if (RESULT == "INVALID") {
                            authModel.aliveSwitch.postValue(false)


                        }else if (RESULT == "WARNING"){
                            authModel.aliveSwitch.postValue(false)
                            runOnUiThread {
                                Toast.makeText(authModel.context, "Ошибка сервера зоны не обновлены", Toast.LENGTH_LONG ).show()
                            }

                        }
                        else{ //todo error alias
                            authModel.aliveSwitch.postValue(false)
                            runOnUiThread {
                                Toast.makeText(authModel.context, "Ошибка сервера зоны не обновлены", Toast.LENGTH_LONG ).show()
                            }

                        }
                        logApi(isZoneChecking = true, POST_PHOTO = POST_PHOTO)
                    } else {
                        Log.e("POST_img64_with_edited_text", "onResponse | status: $statusCode")
                        authModel.aliveSwitch.postValue(false)
                        runOnUiThread {
                            Toast.makeText(authModel.context, "Ошибка сервера зоны не обновлены", Toast.LENGTH_LONG ).show()
                        }
                        logApi(isZoneChecking = true)
                    }
                } catch (e: Exception) {
                    Log.e("POST_img64_with_edited_text", "onResponse | exception", e)
                    authModel.aliveSwitch.postValue(false)
                    runOnUiThread {
                        Toast.makeText(authModel.context, "Ошибка зоны не обновлены", Toast.LENGTH_LONG ).show()
                    }
                    logApi(isZoneChecking = true)
                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                authModel.aliveSwitch.postValue(false)
                runOnUiThread {
                    Toast.makeText(authModel.context, "Сервер недоступен", Toast.LENGTH_LONG ).show()
                }

                logApi(isZoneChecking = true, onFailure = true)
            }
        })
    }




    fun logApi(
        POST_PHOTO: PostPhoto? = null,
        crime: Crime? = null,
        onFailure: Boolean = false,
        isZoneChecking: Boolean = false
    ) {
        if (crime != null) {
            if (POST_PHOTO != null) {
                Diagnostics.appendLog(
                    "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                    "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                            "crime:$crime;" +
                            "uuid:${authModel.uuidKey};" +
                            "secureKey:${authModel.secureKey};" +
                            "isAuthSuccess:${authModel.authSuccess.value.toString()};" +
                            "resResult:${POST_PHOTO.RESULT};" +
                            "resMsg:${POST_PHOTO.palteNumber};info:${POST_PHOTO.info};"+
                            "version:${Auth.VERSION}"
                )
            } else {
                if (onFailure) {
                    Diagnostics.appendLog(
                        "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                        "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                                "crime:$crime;" +
                                "uuid:${authModel.uuidKey};" +
                                "secureKey:${authModel.secureKey};" +
                                "isAuthSuccess:${authModel.authSuccess.value.toString()};" +
                                "error:onFailure;"+
                                "version:${Auth.VERSION}"
                    )

                } else {
                    Diagnostics.appendLog(
                        "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                        "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                                "crime:$crime;" +
                                "uuid:${authModel.uuidKey};" +
                                "secureKey:${authModel.secureKey};" +
                                "isAuthSuccess:${authModel.authSuccess.value.toString()};" +
                                "error:Error in Application;"+
                                "version:${Auth.VERSION}"
                    )

                }
            }
        } else {
            if (!isZoneChecking) {
                if (onFailure) {
                    Diagnostics.appendLog(
                        "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                        "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                        "crime:PostAuthKeys;" +
                                "uuid:${authModel.uuidKey};" +
                                "secureKey:${authModel.secureKey};" +
                                "isAuthSuccess:${authModel.authSuccess.value.toString()}" +
                                ";error:onFailure;"+
                                "version:${Auth.VERSION}"
                    )

                } else {
                    Diagnostics.appendLog(
                        "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                        "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                        "crime:PostAuthKeys;" +
                                "uuid:${authModel.uuidKey};" +
                                "secureKey:${authModel.secureKey};" +
                                "isAuthSuccess:${authModel.authSuccess.value.toString()}" +
                                ";resResult:${POST_PHOTO?.RESULT};"+
                                "version:${Auth.VERSION}"
                    )

                }
            } else {
                    if (onFailure) {
                        Diagnostics.appendLog(
                            "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                            "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                            "crime:zoneChecking;" +
                                    "uuid:${authModel.uuidKey};" +
                                    "secureKey:${authModel.secureKey};" +
                                    "isAuthSuccess:${authModel.authSuccess.value.toString()}" +
                                    ";error:onFailure;"+
                                    "version:${Auth.VERSION}"
                        )

                    } else {
                        Diagnostics.appendLog(
                            "${SimpleDateFormat("YYMMdd").format(Date())}_Api",
                            "date:${SimpleDateFormat("hh:mm:ss").format(Date())};" +
                            "crime:zoneChecking;" +
                                    "uuid:${authModel.uuidKey};" +
                                    "secureKey:${authModel.secureKey};" +
                                    "isAuthSuccess:${authModel.authSuccess.value.toString()}" +
                                    ";resResult:${POST_PHOTO?.RESULT};"+
                                    "version:${Auth.VERSION}"
                        )

                    }

            }
        }
    }


}



