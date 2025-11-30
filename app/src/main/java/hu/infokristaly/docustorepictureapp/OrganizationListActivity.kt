package hu.infokristaly.docustorepictureapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import java.util.Optional

class OrganizationListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizationListBinding
    private lateinit var appbar: Toolbar

    private var organization: Optional<Organization> = Optional.empty()
    private var organizations: Optional<List<Organization>> = Optional.of(listOf())

    val activityOrganizationEditorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            val outOrganization = result.data?.extras?.getSerializable(getString(R.string.KEY_ORGANIZATION));
            if (outOrganization != null) {
                organization = Optional.of(outOrganization as Organization)
            }
            updateListView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val self = this

        binding = ActivityOrganizationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState?.getSerializable(getString(R.string.KEY_ORGANIZATION)) != null) {
            organization = Optional.of(savedInstanceState.getSerializable(getString(R.string.KEY_ORGANIZATION)) as Organization)
        }
        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        updateListView()

        binding.lvOrganizations.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                organization = Optional.of(organizations.get().get(position))
            }

        binding.btnNew.setOnClickListener {
            val intent = Intent(this, OrganizationEditorActivity::class.java)
            val organizationNew = Organization(null,"","", "", 0)
            val bundle = Bundle();
            bundle.putSerializable(getString(R.string.KEY_ORGANIZATION), organizationNew)
            intent.putExtras(bundle);
            activityOrganizationEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (organization.isPresent) {
                val intent = Intent(this, OrganizationEditorActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_ORGANIZATION), organization.get())
                intent.putExtras(bundle);
                activityOrganizationEditorLauncher.launch(intent)
            }
        }

        binding.btnDelete.setOnClickListener {
            if (organization.isPresent) {
                try {
                    ApiRoutins.deleteOrganization(this, organization.get().id!!)
                    organization = Optional.empty()
                    updateListView()
                } catch (e:Exception) {
                    Toast.makeText(self,e.toString(),Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnSelect.setOnClickListener { it ->
            if (organization.isPresent) {
                val i = Intent()
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_ORGANIZATION), organization.get())
                i.putExtras(bundle);
                setResult(RESULT_OK, i)
                finish()
            }
        }
    }

    private fun updateListView() {
        try {
            organizations = ApiRoutins.getOrganizations(this)
        } catch (e:Exception) {
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()
            organizations = Optional.of(listOf())
        }
        val adapter = OrganizationAdapter(this, organizations.get())
        binding.lvOrganizations.adapter = adapter
        binding.lvOrganizations.setChoiceMode(ListView.CHOICE_MODE_SINGLE)
        if (organization.isPresent) {
            val targetPosition = organizations.get().indexOf(organization.get());
            binding.lvOrganizations.setItemChecked(targetPosition, true)
            binding.lvOrganizations.setSelection(targetPosition)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getSerializable(getString(R.string.KEY_ORGANIZATION)) != null) {
            organization = Optional.of(savedInstanceState.getSerializable(getString(R.string.KEY_ORGANIZATION)) as Organization)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (organization.isPresent) {
            outState.putSerializable(getString(R.string.KEY_ORGANIZATION), organization.get())
        }
    }

}