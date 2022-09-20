package ru.vigtech.android.vigpark.tools

import android.content.Context
import androidx.preference.PreferenceManager

object AliasZone {
    private val LISTOFALIAS = "listOfAlias"
    private var listOfAlias : Set<String> = setOf("1", "2", "3", "4", "5", "6")

    fun getlistOfAlias(context: Context): Set<String>{
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        listOfAlias = preferences.getStringSet(LISTOFALIAS, setOf("1", "2", "3", "4", "5", "6"))!!
        return listOfAlias
    }

    fun setListOfAlias(context: Context, setOfAlias: Set<String>){
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putStringSet(LISTOFALIAS, setOfAlias).apply()
    }






}