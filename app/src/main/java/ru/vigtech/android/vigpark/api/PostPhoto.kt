package ru.vigtech.android.vigpark.api

import com.google.gson.annotations.SerializedName
import ru.vigtech.android.vigpark.viewmodel.Auth

data class PostPhoto(
   var RESULT: String,
   @SerializedName("PlateText")
   var palteNumber: String,
   var info: String = "",
   var Rect: ArrayList<String?> = ArrayList<String?>(),
   var version: String = Auth.VERSION
)