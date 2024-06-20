package hu.infokristaly.docustorepictureapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import hu.infokristaly.docustorepictureapp.databinding.ActivityOrganizationEditorBinding
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins

class OrganizationEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizationEditorBinding
    private lateinit var appbar: Toolbar
    private var organization: Organization? = null
    private val KEY_ORGANIZATION = "organization"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityOrganizationEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            organization = savedInstanceState.getSerializable(KEY_ORGANIZATION) as Organization
        } else {
            organization = intent.getSerializableExtra("organization") as Organization
        }

        updateViews()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSave.setOnClickListener {
            organization!!.name = binding.edtOrganizationName.text.toString()
            organization!!.hqAddress = binding.edtAddress.text.toString()
            organization!!.hqPhone = binding.edtPhone.text.toString()
            val serverAddress = ApiRoutins.getServerAddress(this, packageName)
            val gson = Gson()
            val organizationJson = gson.toJson(organization)
            ApiRoutins.postPutOrganization(
                "https://$serverAddress/api/organization"
                        + if (organization!!.id != null) "/${organization!!.id}" else "",
                if (organization!!.id == null) "POST" else "PUT",
                organizationJson
            )
            val i = Intent()
            setResult(RESULT_OK, i)
            finish()
        }

        binding.btnCancel.setOnClickListener {
            val i = Intent()
            setResult(RESULT_CANCELED, i)
            finish()
        }
    }

    private fun updateViews() {
        binding.edtOrganizationName.setText(organization?.name)
        binding.edtAddress.setText(organization?.hqAddress)
        binding.edtPhone.setText(organization?.hqPhone)
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
        organization!!.name = binding.edtOrganizationName.text.toString()
        organization!!.hqAddress = binding.edtAddress.text.toString()
        organization!!.hqAddress = binding.edtPhone.text.toString()
        outState.putSerializable(KEY_ORGANIZATION, organization)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        organization = savedInstanceState.getSerializable(KEY_ORGANIZATION) as Organization
    }

}