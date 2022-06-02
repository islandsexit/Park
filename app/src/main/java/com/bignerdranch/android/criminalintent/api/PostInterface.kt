package com.bignerdranch.android.criminalintent.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface PostInterface {
    @FormUrlEncoded
    @POST("/onplate")
    fun  postPlate( @Field("image") img64_full: String): Call<PostPhoto>


    @FormUrlEncoded
    @POST("/onplate")
    fun  postPlateEdited( @Field("image") img64_full: String, @Field("plate") edited_plate_number: String): Call<PostPhoto>
}