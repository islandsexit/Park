package ru.vigtech.android.vigpark.tools

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import ru.vigtech.android.vigpark.BuildConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

@SuppressLint("StaticFieldLeak")
object Diagnostics {
    lateinit var context: Context
    const val TAG = "Diagnostics"

    var DEBUG = BuildConfig.DEBUG
    fun i(msg: String?): DiagnosticLog {
        return i(TAG, msg)
    }

    fun i(tag: String?, msg: String?): DiagnosticLog {
        var msg = msg
        msg = if (TextUtils.isEmpty(msg)) "" else msg
        Log.i(tag, msg!!)
        return DiagnosticLog(msg)
    }

    fun e(tag: String?, msg: String?): DiagnosticLog {
        var msg = msg
        msg = if (TextUtils.isEmpty(msg)) "" else msg
        Log.e(tag, msg!!)
        return DiagnosticLog(msg)
    }

    fun i(caller: Any, msg: String): DiagnosticLog {
        return i(caller, TAG, msg)
    }

    fun i(caller: Any, tag: String?, msg: String): DiagnosticLog {
        return i(tag, caller.javaClass.simpleName + "." + msg)
    }

    fun e(caller: Any, msg: String): DiagnosticLog {
        return e(caller, TAG, msg)
    }

    fun e(caller: Any, tag: String?, msg: String): DiagnosticLog {
        return e(tag, caller.javaClass.simpleName + "." + msg)
    }

    fun e(msg: String?): DiagnosticLog {
        return e(TAG, msg)
    }

    fun getLogFile(filename: String): File {
        Log.e("LOG", context.externalCacheDir!!.absolutePath)
        return File( context.externalCacheDir!!.absolutePath,"$filename.log")
    }

    fun createLog(filename: String) {
        val file = getLogFile(filename)
        if (file.exists()) file.delete()
        try {
            file.createNewFile()
            appendLog(filename, "Created at " + Date().toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun appendLog(filename: String, line: String?) {
        val file = getLogFile(filename)
        if (!file.exists()) createLog(filename)
        try {
            val bufferedWriter = BufferedWriter(FileWriter(file, true))
            bufferedWriter.write(line)
            bufferedWriter.newLine()
            bufferedWriter.flush()
            bufferedWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class DiagnosticLog(private val msg: String?) {
        fun append(fileName: String) {
            appendLog(fileName, msg)
        }
    }
}