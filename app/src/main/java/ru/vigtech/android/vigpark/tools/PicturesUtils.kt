package ru.vigtech.android.vigpark.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import ru.vigtech.android.vigpark.database.Crime
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


object PicturesUtils {

    var canCam = true

    fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
    }


    fun getImg_64(crime: Crime):String{
        //todo удалить строку, на которую нажал и коорая не отправилась
        if(File(crime.img_path).exists() && crime.img_path != "") {
            val bOut2 = ByteArrayOutputStream()
            var bm = BitmapFactory.decodeFile(crime.img_path)
            if (bm.height != 768){
                bm = Bitmap.createScaledBitmap(bm, 1024, 768, false)
                val out = FileOutputStream(crime.img_path)
                bm.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
            var img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
            return img64_full
        }
        return ""
    }

    fun img64FromFile(img_path:String):String{
        val bOut2 = ByteArrayOutputStream()
        var bm = BitmapFactory.decodeFile(img_path)
        if (bm.height != 768){
            bm = Bitmap.createScaledBitmap(bm, 1024, 768, false)
            val out = FileOutputStream(img_path)
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
        var img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
        return img64_full.toString()
    }
}