package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocInfoBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.DocumentDirection
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.network.NetworkClient
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import hu.infokristaly.forrasimageserver.entity.Subject
import java.util.Date


class DocInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocInfoBinding
    lateinit var stored: StoredItems
    private var serverAddress = ""
    private var organizations = listOf<Organization>()
    private var subjects = listOf<Subject>()

    val activityOrganizationListLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            stored.selectedOrganization = result.data?.getSerializableExtra("organization") as Organization
            updateView()
        }
    }

    val activitySubjectListLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            stored.selectedSubject = result.data?.getSerializableExtra("subject") as Subject
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

        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(sharedPrefs)
        }

        updateView()

        serverAddress = ApiRoutins.getServerAddress(this, packageName)

        if (serverAddress != "") {
            updateAutoComplette()
        } else {
            val intent = Intent(this, SettingsActivity::class.java)
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

                NetworkClient()
                    .sendDocInfo(this, serverAddress, stored.docInfo)
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

    private fun updateView() {
        binding.actSubject.setText(stored.selectedSubject?.value)
        binding.actOrganization.setText(stored.selectedOrganization?.name)
    }

    private fun updateAutoComplette() {
        organizations = ApiRoutins.getOrganizations(serverAddress)

        binding.actOrganization.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                organizations
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

        subjects = ApiRoutins.getSubjects(serverAddress)
        binding.actSubject.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                subjects
            )
        )
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stored.saveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(savedInstanceState)
        serverAddress = ApiRoutins.getServerAddress(this, packageName)
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