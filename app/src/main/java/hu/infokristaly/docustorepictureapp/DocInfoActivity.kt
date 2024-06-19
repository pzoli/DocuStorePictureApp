package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private var organizations = listOf<Organization>()
    private var subjects = listOf<Subject>()

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

        updateView()

        serverAddress = getServerAddress()

        if (serverAddress != "") {
            updateSpinners()
        } else {
            val intent = Intent(this,SettingsActivity::class.java)
            startActivity(intent)
        }
        binding.btnSend.setOnClickListener {
            if (stored.selectedSubject != null && stored.selectedOrganization != null) {
                stored.docInfo = DocInfo(
                    null,
                    stored.selectedSubject!!,
                    DocumentDirection.IN,
                    stored.selectedOrganization!!,
                    null,
                    Date()
                )

                NetworkClient().sendDocInfo(this, serverAddress, stored.docInfo)
            }
        }
    }

    private fun updateView() {
        binding.actSubject.setText(stored.selectedSubject?.value)
        binding.actOrganization.setText(stored.selectedOrganization?.name)
    }

    private fun updateSpinners() {
        getOrganizations()

        binding.actOrganization.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,organizations))
        binding.actOrganization.dropDownWidth = android.view.ViewGroup.LayoutParams.MATCH_PARENT

        binding.actOrganization.onItemClickListener =
            OnItemClickListener { _, _, pos, id ->
                stored.selectedOrganization = binding.actOrganization.adapter.getItem(pos) as Organization
                Toast.makeText(
                    this,
                    "selected[$pos, id:${stored.selectedOrganization?.id}}",
                    Toast.LENGTH_LONG
                ).show()
            }

        getSubjects()
        binding.actSubject.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,subjects))
        binding.actSubject.dropDownWidth = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        binding.actSubject.onItemClickListener =
            OnItemClickListener { _, _, pos, id ->
                stored.selectedSubject = binding.actSubject.adapter.getItem(pos) as Subject
                Toast.makeText(
                    this,
                    " selected[$pos, id:${stored.selectedSubject?.id}]",
                    Toast.LENGTH_LONG
                ).show()
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
            organizations = gson.fromJson<List<Organization>>(organizationsResult,itemType)
            //organizationList.map { item -> Pair(item.id!!,item) }
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
            subjects = gson.fromJson<List<Subject>>(subjectsResult,itemType)
            //subjectList.map { item -> Pair(item.id!!,item) }
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