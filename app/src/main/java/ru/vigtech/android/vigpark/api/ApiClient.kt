package ru.vigtech.android.vigpark.api


import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext


//@SuppressLint("StaticFieldLeak")
object ApiClient {
    var baseUrl = "http://95.182.74.37:1234/"
    var retrofit: Retrofit = getRetroInstance(baseUrl)

    lateinit var authModel: Auth





    private fun getRetroInstance(baseUrl: String): Retrofit {
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
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
    }



    fun POST_img64( img64_full: String, img_path: String, img_plate_path:String, zone:Int,long: Double, lat:Double, ) {
        val post_api: PostInterface = retrofit.create(PostInterface::class.java)
        val crime = Crime(title = "Отправка на сервер", img_path = img_path, img_path_full = img_plate_path, send = true, found = true, Zone=zone, lon = long, lat = lat, Rect = ArrayList<String?>())
        Log.w("Create", "create $crime")
        CrimeRepository.get().addCrime(crime)
        val call: Call<PostPhoto> = post_api.postPlate(img64_full,zone, long, lat, authModel.uuidKey, authModel.secureKey)
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
                            crime.title = "Ошибка лицензирований"
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
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        crime.title = "Сервер недоступен"
                        crime.send = false
                        crime.found = false
                        CrimeRepository.get().updateCrime(crime)
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    crime.title = "Сервер недоступен"
                    crime.send = false
                    crime.found = false
                    CrimeRepository.get().updateCrime(crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                crime.title = "Сервер недоступен"
                crime.send = false
                crime.found = false
                CrimeRepository.get().updateCrime(crime)

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
        val call: Call<PostPhoto> = post_api.postPlate(img64,crime.Zone, crime.lon, crime.lat, authModel.uuidKey, authModel.secureKey)
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
                    } else {
                        Log.e("POST", "onResponse | status: $statusCode")
                        crime.send = false
                        crime.title  = "Сервер недоступен"
                        crime.info = ""
                        CrimeRepository.get().updateCrime(crime)
                    }
                } catch (e: Exception) {
                    Log.e("POST", "onResponse | exception", e)
                    crime.send = false
                    crime.info=""
                    crime.title  = "Сервер недоступен"
                    CrimeRepository.get().updateCrime(crime)

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                Log.e("POST", "onFailure", t)
                crime.send = false
                crime.title = "Сервер недоступен"
                crime.info = ""
                CrimeRepository.get().updateCrime(crime)

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
            post_api.postPlateEdited(img64, new_plate, crime.Zone, crime.lon, crime.lat, authModel.uuidKey, authModel.secureKey)
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

                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
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

            }
        })
    }

        fun postAuthKeys() {
            val post_api: PostInterface = retrofit.create(PostInterface::class.java)
            val call: Call<PostPhoto> = post_api.postPlate("testKey",0, 0.0, 0.0, authModel.uuidKey, authModel.secureKey)
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
                            }else if (RESULT == "INVALID"){
                                authModel.onCheckLicence(false)


                        } else{
                                authModel.onCheckLicence(true)

//                            if(authModel.authSuccess.value == 2){
//                                authModel.authSuccess.postValue(1)
//                            }
                            }
                        } else {
                            authModel.onCheckLicence(false)

                        }
                    } catch (e: Exception) {
                        Log.e("POST_img64_with_edited_text", "onResponse | exception", e)
                        authModel.onCheckLicence(false)




                    }
                }

                override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                    Log.e("POST_img64_with_edited_text", "onFailure", t)
                    authModel.onCheckLicence(false)

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
                        Log.e(
                            "POST_CHECK_ZONE",
                            "onResponse| response: Result: $RESULT, info: $info "
                        )
                        if (RESULT == "SUCCESS") {
                            val new_zones = info.split(";").toSet()
                            if(new_zones != authModel.listOfAlias.value){

                                authModel.savePreferences(new_zones)
                            }
                        } else if (RESULT == "INVALID") {
                           authModel.onCheckLicence(false)


                        }else if (RESULT == "WARNING"){
                            runOnUiThread {
                                Toast.makeText(authModel.context, "Ошибка сервера зоны не обновлены", Toast.LENGTH_LONG ).show()
                            }
                        }
                        else{ //todo error alias
                            runOnUiThread {
                                Toast.makeText(authModel.context, "Ошибка сервера зоны не обновлены", Toast.LENGTH_LONG ).show()
                            }
                        }
                    } else {
                        Log.e("POST_img64_with_edited_text", "onResponse | status: $statusCode")
                        runOnUiThread {
                            Toast.makeText(authModel.context, "Ошибка сервера зоны не обновлены", Toast.LENGTH_LONG ).show()
                        }

                    }
                } catch (e: Exception) {
                    Log.e("POST_img64_with_edited_text", "onResponse | exception", e)
                    runOnUiThread {
                        Toast.makeText(authModel.context, "Ошибка зоны не обновлены", Toast.LENGTH_LONG ).show()
                    }


                }
            }

            override fun onFailure(call: Call<PostPhoto?>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(authModel.context, "Сервер недоступен", Toast.LENGTH_LONG ).show()
                }
            }
        })
    }




}



