package hu.infokristaly.docustorepictureapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.infokristaly.docustorepictureapp.databinding.ActivityOrganizationEditorBinding
import hu.infokristaly.docustorepictureapp.databinding.ActivityOrganizationListBinding
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.OrganizationAdapter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL

class OrganizationListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizationListBinding
    private lateinit var appbar: Toolbar

    private val KEY_ORGANIZATION = "organization"
    private var organization: Organization? = null
    private var organizations = listOf<Organization>()
    private var serverAddress = ""

    val activityOrganizationEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            serverAddress = ApiRoutins.getServerAddress(this, packageName)
            organization = null
            updateListView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityOrganizationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState?.getSerializable(KEY_ORGANIZATION) != null) {
            organization = savedInstanceState.getSerializable(KEY_ORGANIZATION) as Organization
        }
        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        serverAddress = ApiRoutins.getServerAddress(this, packageName)
        updateListView()

        binding.lvOrganizations.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                organization = organizations.get(position)
            }

        binding.btnNew.setOnClickListener {
            val intent = Intent(this, OrganizationEditorActivity::class.java)
            val organizationNew = Organization(null,"","", "")
            val bundle = Bundle();
            bundle.putSerializable(KEY_ORGANIZATION, organizationNew)
            intent.putExtras(bundle);
            activityOrganizationEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (organization != null) {
                val intent = Intent(this, OrganizationEditorActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(KEY_ORGANIZATION, organization)
                intent.putExtras(bundle);
                activityOrganizationEditorLauncher.launch(intent)
            }
        }

        binding.btnDelete.setOnClickListener {
            if (organization != null) {
                ApiRoutins.deleteOrganization(serverAddress,organization!!.id!!)
                organization = null
                updateListView()
            }
        }

        binding.btnSelect.setOnClickListener { it ->
            if (organization != null) {
                val i = Intent()
                val bundle = Bundle();
                bundle.putSerializable(KEY_ORGANIZATION, organization)
                i.putExtras(bundle);
                setResult(RESULT_OK, i)
                finish()
            }
        }
    }

    private fun updateListView() {
        organizations = ApiRoutins.getOrganizations(serverAddress)
        binding.lvOrganizations.adapter = OrganizationAdapter(this, organizations)
        binding.lvOrganizations.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getSerializable(KEY_ORGANIZATION) != null) {
            organization = savedInstanceState.getSerializable(KEY_ORGANIZATION) as Organization
        }
        serverAddress = ApiRoutins.getServerAddress(this, packageName)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_ORGANIZATION, organization)
    }

}