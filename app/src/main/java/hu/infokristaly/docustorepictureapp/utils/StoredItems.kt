package hu.infokristaly.docustorepictureapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import hu.infokristaly.docustorepictureapp.R
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.model.DocumentSubject

class StoredItems {

    var imageFilePath = ""
    var lastIFileInfoId : Long = -1
    var docInfo: DocInfo? = null
    var selectedOrganization: Organization? = null
    var selectedSubject: DocumentSubject? = null

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
            selectedSubject = gson.fromJson(subjectJson, DocumentSubject::class.java)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
        }
    }

    fun saveInstanceState(context: Context, outState: Bundle) {
        outState.putString(context.getString(R.string.KEY_IMAGEPATH), imageFilePath)
        val serializedDocInfo = getSerializedDocInfo()
        outState.putString(context.getString(R.string.KEY_DOCINFO), serializedDocInfo)
        val serializedOrganization = getSerializedOrganization()
        outState.putString(context.getString(R.string.KEY_SELECTEDORGANIZATION), serializedOrganization)
        val serializeSubject = getSerializedSubject()
        outState.putString(context.getString(R.string.KEY_SELECTEDSUBJECT), serializeSubject)
        outState.putLong(context.getString(R.string.KEY_LAST_VIEWED_FILEINFO_ID), lastIFileInfoId)
    }

    fun restoreStateFromBundle(context: Context, savedInstanceState: Bundle) {
        savedInstanceState.let {
            imageFilePath = savedInstanceState.getString(context.getString(R.string.KEY_IMAGEPATH)) ?: ""
            getDocInfoFromJSON(savedInstanceState.getString(context.getString(R.string.KEY_DOCINFO)) ?: "")
            getSubjectFromJSON(savedInstanceState.getString(context.getString(R.string.KEY_SELECTEDSUBJECT))?: "")
            getOrganizationFromJSON(savedInstanceState.getString(context.getString(R.string.KEY_SELECTEDORGANIZATION))?:"")
            lastIFileInfoId = savedInstanceState.getLong(context.getString(R.string.KEY_LAST_VIEWED_FILEINFO_ID))
        }
    }

    fun restoreFromSharedPrefs(context: Context, sharedPrefs: SharedPreferences) {
        imageFilePath = sharedPrefs.getString(context.getString(R.string.KEY_IMAGEPATH), "") ?: ""
        getDocInfoFromJSON(sharedPrefs.getString(context.getString(R.string.KEY_DOCINFO), "") ?: "")
        getSubjectFromJSON(sharedPrefs.getString(context.getString(R.string.KEY_SELECTEDSUBJECT),"")?: "")
        getOrganizationFromJSON(sharedPrefs.getString(context.getString(R.string.KEY_SELECTEDORGANIZATION),"")?:"")
        lastIFileInfoId = sharedPrefs.getLong(context.getString(R.string.KEY_LAST_VIEWED_FILEINFO_ID),-1)
    }

    fun saveState(context: Context, sharedPrefs: SharedPreferences) {
        sharedPrefs.edit()
            .putString(context.getString(R.string.KEY_IMAGEPATH), imageFilePath)
            .putString(context.getString(R.string.KEY_DOCINFO), getSerializedDocInfo())
            .putString(context.getString(R.string.KEY_SELECTEDSUBJECT), getSerializedSubject())
            .putString(context.getString(R.string.KEY_SELECTEDORGANIZATION), getSerializedOrganization())
            .putLong(context.getString(R.string.KEY_LAST_VIEWED_FILEINFO_ID), lastIFileInfoId)
            .apply()
    }

}