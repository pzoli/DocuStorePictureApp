package hu.infokristaly.docustorepictureapp.utils

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.forrasimageserver.entity.Subject

class StoredItems {

    var imageFilePath = ""
    lateinit var docInfo: DocInfo
    var selectedOrganization: Organization? = null
    var selectedSubject: Subject? = null
    val KEY_IMAGEPATH = "imagepath"
    val KEY_DOCINFO = "docinfo"
    val KEY_SELECTEDORGANIZATION = "selectedOrganization"
    val KEY_SELECTEDSUBJECT = "selectedSubject"

    fun getSerializedDocInfo(): String {
        val gson = Gson()
        var serializedDocInfo = ""
        try {
            serializedDocInfo = gson.toJson(docInfo)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
        return serializedDocInfo
    }

    fun getDocInfoFromJSON(docInfoJson: String) {
        val gson = Gson()
        try {
            docInfo = gson.fromJson(docInfoJson, DocInfo::class.java)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
    }

    fun getSerializedOrganization(): String {
        val gson = Gson()
        var serializedOrganization = ""
        try {
            serializedOrganization = gson.toJson(selectedOrganization)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
        return serializedOrganization
    }

    fun getOrganizationFromJSON(organizationJson: String) {
        val gson = Gson()
        try {
            selectedOrganization = gson.fromJson(organizationJson, Organization::class.java)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
    }

    fun getSerializedSubject(): String {
        val gson = Gson()
        var serializedSubject = ""
        try {
            serializedSubject = gson.toJson(selectedSubject)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
        return serializedSubject
    }

    fun getSubjectFromJSON(subjectJson: String) {
        val gson = Gson()
        try {
            selectedSubject = gson.fromJson(subjectJson, Subject::class.java)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putString(KEY_IMAGEPATH, imageFilePath)
        val serializedDocInfo = getSerializedDocInfo()
        outState.putString(KEY_DOCINFO, serializedDocInfo)
        val serializedOrganization = getSerializedOrganization()
        outState.putString(KEY_SELECTEDORGANIZATION, serializedOrganization)
        val serializeSubject = getSerializedSubject()
        outState.putString(KEY_SELECTEDSUBJECT, serializeSubject)
    }

    fun restoreStateFromBundle(savedInstanceState: Bundle) {
        savedInstanceState.let {
            imageFilePath = savedInstanceState.getString(KEY_IMAGEPATH) ?: ""
            getDocInfoFromJSON(savedInstanceState.getString(KEY_DOCINFO) ?: "")
            getSubjectFromJSON(savedInstanceState.getString(KEY_SELECTEDSUBJECT)?: "")
            getOrganizationFromJSON(savedInstanceState.getString(KEY_SELECTEDORGANIZATION)?:"")
        }
    }

    fun restoreFromSharedPrefs(sharedPrefs: SharedPreferences) {
        imageFilePath = sharedPrefs.getString(KEY_IMAGEPATH, "") ?: ""
        getDocInfoFromJSON(sharedPrefs.getString(KEY_DOCINFO, "") ?: "")
        getSubjectFromJSON(sharedPrefs.getString(KEY_SELECTEDSUBJECT,"")?: "")
        getOrganizationFromJSON(sharedPrefs.getString(KEY_SELECTEDORGANIZATION,"")?:"")
    }

    fun saveState(sharedPrefs: SharedPreferences) {
        sharedPrefs.edit()
            .putString(KEY_IMAGEPATH, imageFilePath)
            .putString(KEY_DOCINFO, getSerializedDocInfo())
            .putString(KEY_SELECTEDSUBJECT, getSerializedSubject())
            .putString(KEY_SELECTEDORGANIZATION, getSerializedOrganization())
            .apply()
    }

}