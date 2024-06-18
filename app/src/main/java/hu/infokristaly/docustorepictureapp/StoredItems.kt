package hu.infokristaly.forrasadmin.qrcodescanner.components

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import hu.infokristaly.docustorepictureapp.model.DocInfo
import java.net.URI

class StoredItems {

    var imageFilePath = ""
    lateinit var docInfo: DocInfo

    val KEY_IMAGEPATH = "imagepath"
    val KEY_DOCINFO = "docinfo"

    fun getSerializedDocInfo(): String {
        val gson = Gson()
        var serializedDocInfo = ""
        try {
            serializedDocInfo = gson.toJson(docInfo)
        } catch (e:Exception) {
            Log.e("Error", e.message.toString())
        }
        return serializedDocInfo
    }

    fun getDocInfoFromJSON(docInfoJson: String) {
        val gson = Gson()
        try {
            docInfo = gson.fromJson(docInfoJson, DocInfo::class.java)
        } catch (e:Exception) {
            Log.e("Error", e.message.toString())
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putString(KEY_IMAGEPATH, imageFilePath)
            val serializedDocInfo = getSerializedDocInfo()
            outState.putString(KEY_DOCINFO,serializedDocInfo)
    }

    fun restoreStateFromBundle(savedInstanceState: Bundle) {
        savedInstanceState.let {
            imageFilePath = savedInstanceState.getString(KEY_IMAGEPATH)?:""
            getDocInfoFromJSON(savedInstanceState.getString(KEY_DOCINFO)?:"")
        }
    }

    fun restoreFromSharedPrefs(sharedPrefs: SharedPreferences) {
        imageFilePath = sharedPrefs.getString(KEY_IMAGEPATH, "")?:""
        getDocInfoFromJSON(sharedPrefs.getString(KEY_DOCINFO, "")?:"")
    }

    fun saveState(sharedPrefs: SharedPreferences) {
        sharedPrefs.edit()
            .putString(KEY_IMAGEPATH, imageFilePath)
            .putString(KEY_DOCINFO, getSerializedDocInfo())
            .apply()
    }

}