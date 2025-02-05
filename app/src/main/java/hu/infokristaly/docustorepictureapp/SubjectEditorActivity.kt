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
import hu.infokristaly.docustorepictureapp.databinding.ActivitySubjectEditorBinding
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.model.DocumentSubject

class SubjectEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubjectEditorBinding
    private lateinit var appbar: Toolbar
    private lateinit var subject: DocumentSubject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySubjectEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        subject = intent.getSerializableExtra("subject") as DocumentSubject
        binding.edtSubjectValue.setText(subject.value)

        binding.btnCancel.setOnClickListener {
            val i = Intent()
            setResult(RESULT_CANCELED, i)
            finish()
        }

        binding.btnSave.setOnClickListener {
            subject.value = binding.edtSubjectValue.text.toString()
            val serverAddress = ApiRoutins.getSharedPrefProp(this,getString(R.string.KEY_SERVERADDRESS))
            val gson = Gson()
            val subjectJson = gson.toJson(subject)
            ApiRoutins.postPutSubject(this,"https://$serverAddress/api/subject" + if (subject.id!=null) "/${subject.id}"  else "",if (subject.id == null) "POST" else "PUT", subjectJson)
            val i = Intent()
            setResult(RESULT_OK, i)
            finish()
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
}