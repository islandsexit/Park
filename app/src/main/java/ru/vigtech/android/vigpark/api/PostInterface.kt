package ru.vigtech.android.vigpark.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface PostInterface {
    @FormUrlEncoded
    @POST("/onplate")
    fun  postPlate(@Field("image") img64_full: String, @Field("zone")zone:Int, @Field("lon")long: Double, @Field("lat")lat:Double, @Field("uuid")uuidKey: String, @Field("sec")secKey: String, @Field("date")date:String): Call<PostPhoto>


    @FormUrlEncoded
    @POST("/onplateedited")
    fun  postPlateEdited( @Field("image") img64_full: String, @Field("plate") edited_plate_number: String, @Field("zone")zone:Int, @Field("lon")long: Double, @Field("lat")lat:Double, @Field("uuid")uuidKey: String, @Field("sec")secKey: String
                          , @Field("date")date:String): Call<PostPhoto>

    @FormUrlEncoded
    @POST("/zonecheking")
    fun zoneCheck(@Field("uuid") uuidKey: String, @Field("sec") secKey: String):Call<PostPhoto>
}