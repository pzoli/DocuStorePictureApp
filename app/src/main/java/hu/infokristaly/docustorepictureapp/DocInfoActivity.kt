package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocInfoBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.DocumentDirection
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.network.NetworkClient
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import hu.infokristaly.docustorepictureapp.model.DocumentSubject
import java.util.Date
import java.util.Optional


class DocInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocInfoBinding
    lateinit var stored: StoredItems

    private var organizations: Optional<List<Organization>> = Optional.of(listOf())
    private var subjects: Optional<List<DocumentSubject>> = Optional.of(listOf())
    private var toolbar: Toolbar? = null

    val activityOrganizationListLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            stored.selectedOrganization =
                result.data?.getSerializableExtra("organization") as Organization
            updateView()
        }
    }

    val activitySubjectListLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            stored.selectedSubject = result.data?.getSerializableExtra("subject") as DocumentSubject
            updateView()
        }
    }

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

        toolbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(this, savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(this, sharedPrefs)
        }

        if (stored.docInfo != null) {
            if (intent.hasExtra(getString(R.string.KEY_DOCINFO))) {
                stored.docInfo = intent.getSerializableExtra(getString(R.string.KEY_DOCINFO)) as DocInfo
                stored.selectedSubject = stored.docInfo!!.subject
                stored.selectedOrganization = stored.docInfo!!.organization
            }
        }

        updateView()

        val serverAddress = ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_SERVERADDRESS))
        val userName = ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_USERNAME))
        val password = ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_PASSWORD))

        if (serverAddress == "" || userName == "" || password == "") {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else {
            updateAutoComplette()
        }
        binding.btnSend.setOnClickListener {
            if (stored.selectedSubject != null && stored.selectedOrganization != null) {
                if (stored.docInfo != null) {
                    stored.docInfo!!.subject = stored.selectedSubject
                    stored.docInfo!!.organization = stored.selectedOrganization
                    if (stored.docInfo!!.createdAt == null) {
                        stored.docInfo!!.createdAt = Date()
                    }
                    if (stored.docInfo!!.direction == null) {
                        stored.docInfo!!.direction = DocumentDirection.IN
                    }
                } else {
                    stored.docInfo = DocInfo(
                        null,
                        stored.selectedSubject!!,
                        DocumentDirection.IN,
                        stored.selectedOrganization!!,
                        null,
                        Date()
                    )
                }
                NetworkClient()
                    .sendDocInfo(this, stored.docInfo)
            }
        }
        binding.btnSubject.setOnClickListener {
            val intent = Intent(this, SubjectListActivity::class.java)
            activitySubjectListLauncher.launch(intent)
        }
        binding.btnOrganization.setOnClickListener {
            val intent = Intent(this, OrganizationListActivity::class.java)
            activityOrganizationListLauncher.launch(intent)

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateView() {
        binding.actSubject.setText(stored.selectedSubject?.value)
        binding.actOrganization.setText(stored.selectedOrganization?.name)
    }

    private fun updateAutoComplette() {
        try {
            organizations = ApiRoutins.getOrganizations(this)
        } catch (e:Exception) {
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()
            organizations = Optional.of(listOf())
        }

        try {
            subjects = ApiRoutins.getSubjects(this)
        } catch (e:Exception) {
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()
            subjects = Optional.of(listOf())
        }

        binding.actOrganization.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                organizations.get()
            )
        )
        binding.actOrganization.dropDownWidth = android.view.ViewGroup.LayoutParams.MATCH_PARENT

        binding.actOrganization.onItemClickListener =
            OnItemClickListener { _, _, pos, id ->
                stored.selectedOrganization =
                    binding.actOrganization.adapter.getItem(pos) as Organization
                Toast.makeText(
                    this,
                    "selected[$pos, id:${stored.selectedOrganization?.id}}",
                    Toast.LENGTH_LONG
                ).show()
            }

        binding.actSubject.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                subjects.get()
            )
        )
        binding.actSubject.dropDownWidth = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        binding.actSubject.onItemClickListener =
            OnItemClickListener { _, _, pos, id ->
                stored.selectedSubject = binding.actSubject.adapter.getItem(pos) as DocumentSubject
                Toast.makeText(
                    this,
                    " selected[$pos, id:${stored.selectedSubject?.id}]",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stored.saveInstanceState(this, outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(this, savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(this, sharedPrefs)
    }

    override fun onStop() {
        super.onStop()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(this, sharedPrefs)
    }

}