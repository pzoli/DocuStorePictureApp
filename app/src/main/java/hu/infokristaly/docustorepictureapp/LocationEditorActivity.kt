package hu.infokristaly.docustorepictureapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import hu.infokristaly.docustorepictureapp.databinding.ActivityLocationEditorBinding
import hu.infokristaly.docustorepictureapp.model.DocLocation
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins

class LocationEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationEditorBinding
    private lateinit var appbar: Toolbar
    private lateinit var location: DocLocation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val self = this

        binding = ActivityLocationEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        location = intent.getSerializableExtra("location") as DocLocation
        binding.edtLocationName.setText(location.name)

        binding.btnCancel.setOnClickListener {
            val i = Intent()
            setResult(RESULT_CANCELED, i)
            finish()
        }

        binding.btnSave.setOnClickListener {
            location.name = binding.edtLocationName.text.toString()
            val serverAddress = ApiRoutins.getSharedPrefProp(this,getString(R.string.KEY_SERVERADDRESS))
            val gson = Gson()
            val locationJson = gson.toJson(location)
            try {
                ApiRoutins.postPutLocation(
                    self,
                    "https://$serverAddress/api/doclocation" + if (location.id != null) "/${location.id}" else "",
                    if (location.id == null) "POST" else "PUT",
                    locationJson
                )
                val i = Intent()
                setResult(RESULT_OK, i)
                finish()
            } catch (e:Exception) {
                Toast.makeText(this,e.toString(), Toast.LENGTH_LONG).show()
            }
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
    }}