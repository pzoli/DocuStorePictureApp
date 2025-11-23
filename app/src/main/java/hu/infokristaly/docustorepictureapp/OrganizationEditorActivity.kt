package hu.infokristaly.docustorepictureapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import hu.infokristaly.docustorepictureapp.databinding.ActivityOrganizationEditorBinding
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import java.util.Optional

class OrganizationEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizationEditorBinding
    private lateinit var appbar: Toolbar
    private var organization: Optional<Organization> = Optional.of(Organization(null,"","","", 0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val self = this

        binding = ActivityOrganizationEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            organization = Optional.of(savedInstanceState.getSerializable(getString(R.string.KEY_ORGANIZATION)) as Organization)
        } else {
            organization = Optional.of(intent.getSerializableExtra("organization") as Organization)
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
            organization.get().name = binding.edtOrganizationName.text.toString()
            organization.get().hqAddress = binding.edtAddress.text.toString()
            organization.get().hqPhone = binding.edtPhone.text.toString()
            val serverAddress = ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_SERVERADDRESS))
            val gson = Gson()
            val organizationJson = gson.toJson(organization.get())
            try {
                ApiRoutins.postPutOrganization(
                    self,
                    "https://$serverAddress/api/organization"
                            + if (organization.get().id != null) "/${organization.get().id}" else "",
                    if (organization.get().id == null) "POST" else "PUT",
                    organizationJson
                )
                val i = Intent()
                setResult(RESULT_OK, i)
                finish()
            } catch (e:Exception) {
                Toast.makeText(self, e.toString(), Toast.LENGTH_LONG).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            val i = Intent()
            setResult(RESULT_CANCELED, i)
            finish()
        }
    }

    private fun updateViews() {
        if (organization.isPresent) {
            binding.edtOrganizationName.setText(organization.get().name)
            binding.edtAddress.setText(organization.get().hqAddress)
            binding.edtPhone.setText(organization.get().hqPhone)
        } else {
            binding.edtOrganizationName.setText("")
            binding.edtAddress.setText("")
            binding.edtPhone.setText("")
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
        if (!organization.isPresent) {
            organization = Optional.of(Organization(null,"","","", 0))
        }
        organization.get().name = binding.edtOrganizationName.text.toString()
        organization.get().hqAddress = binding.edtAddress.text.toString()
        organization.get().hqAddress = binding.edtPhone.text.toString()
        outState.putSerializable(getString(R.string.KEY_ORGANIZATION), organization.get())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        organization = Optional.of(savedInstanceState.getSerializable(getString(R.string.KEY_ORGANIZATION)) as Organization)
    }

}