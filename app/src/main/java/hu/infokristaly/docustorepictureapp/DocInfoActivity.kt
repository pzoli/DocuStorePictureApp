package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocInfoBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.DocumentDirection
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.forrasadmin.qrcodescanner.components.StoredItems
import hu.infokristaly.forrasimageserver.entity.Subject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

class DocInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocInfoBinding
    lateinit var stored: StoredItems
    private var serverAddress = ""
    private var organizations = listOf<Pair<Int,Organization>>()
    private var subjects = listOf<Pair<Int,Subject>>()

    private var selectedOrganization:Organization? = null
    private var selectedSubject: Subject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        stored = StoredItems()
        binding = ActivityDocInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(sharedPrefs)
        }

        serverAddress = getServerAddress()

        if (serverAddress != "") {
            updateSpinners()
        } else {
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
        }
        binding.btnSend.setOnClickListener {
            if (selectedSubject != null && selectedOrganization != null) {
                stored.docInfo = DocInfo(
                    null,
                    selectedSubject!!,
                    DocumentDirection.IN,
                    selectedOrganization!!,
                    null,
                    null
                )
                stored.docInfo.createdAt = Date()
                NetworkClient().sendDocInfo(this, serverAddress, stored.docInfo)
            }
        }
    }

    private fun updateSpinners() {
        getOrganizations()
        binding.spOrganization.adapter = OrganizationAdapter(this,organizations)

        binding.spOrganization.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedOrganization = null
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedOrganization = organizations[position].second
            }

        }

        getSubjects()
        binding.spSubject.adapter = SubjectAdapter(this,subjects)
        binding.spSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSubject = null
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSubject = subjects[position].second
            }

        }
    }

    private fun getOrganizations() {
        runBlocking {
            var result: Deferred<String> = async() {
                withContext(Dispatchers.IO) {
                    val result =
                        getApiRequest(URL("http://$serverAddress/api/organization"))
                    return@withContext result
                }
            }
            val organizationsResult = result.await()
            val gson = Gson()
            val itemType = object : TypeToken<List<Organization>>() {}.type
            val organizationList = gson.fromJson<List<Organization>>(organizationsResult,itemType)
            organizations = organizationList.map { item -> Pair(item.id!!,item) }
        }

    }

    private fun getSubjects() {
        runBlocking {
            var result: Deferred<String> = async() {
                withContext(Dispatchers.IO) {
                    val result =
                        getApiRequest(URL("http://$serverAddress/api/subject"))
                    return@withContext result
                }
            }
            val subjectsResult = result.await()
            val gson = Gson()
            val itemType = object : TypeToken<List<Subject>>() {}.type
            val subjectList = gson.fromJson<List<Subject>>(subjectsResult,itemType)
            subjects = subjectList.map { item -> Pair(item.id!!,item) }
        }

    }

    private fun getApiRequest(url: URL): String {
        var result = ""
        try {
            val conn = url.openConnection() as HttpURLConnection
            with(conn) {
                requestMethod = "GET"
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream.bufferedReader().use {
                        it.lines().forEach { line ->
                            result += line
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Exception", e.toString())
        }

        return result;
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stored.saveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(savedInstanceState)
        serverAddress = getServerAddress()
    }

    private fun getServerAddress(): String {
        val prefFile = "${packageName}_preferences"
        val sharedPreferences = getSharedPreferences(prefFile, Context.MODE_PRIVATE)
        val result = sharedPreferences.getString("serveraddress", "") ?: ""
        return result
    }

    override fun onPause() {
        super.onPause()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(sharedPrefs)
    }

    override fun onStop() {
        super.onStop()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(sharedPrefs)
    }

}