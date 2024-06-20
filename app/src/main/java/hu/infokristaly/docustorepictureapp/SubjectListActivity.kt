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
import hu.infokristaly.docustorepictureapp.databinding.ActivityOrganizationEditorBinding
import hu.infokristaly.docustorepictureapp.databinding.ActivitySubjectEditorBinding
import hu.infokristaly.docustorepictureapp.databinding.ActivitySubjectListBinding
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.OrganizationAdapter
import hu.infokristaly.docustorepictureapp.utils.SubjectAdapter
import hu.infokristaly.forrasimageserver.entity.Subject

class SubjectListActivity : AppCompatActivity() {
    private val KEY_SUBJECT = "subject"

    private lateinit var binding: ActivitySubjectListBinding
    private lateinit var appbar: Toolbar

    private var subject: Subject? = null
    private var subjects = listOf<Subject>()
    private var serverAddress = ""

    val activitySubjectEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            subject = null
            serverAddress = ApiRoutins.getServerAddress(this, packageName)
            updateListView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySubjectListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        serverAddress = ApiRoutins.getServerAddress(this, packageName)

        updateListView()

        binding.lvSubjects.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                subject = subjects.get(position)
            }

        binding.btnNew.setOnClickListener {
            val intent = Intent(this, SubjectEditorActivity::class.java)
            val subjectNew = Subject(null,"")
            val bundle = Bundle();
            bundle.putSerializable(KEY_SUBJECT, subjectNew)
            intent.putExtras(bundle);
            activitySubjectEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (subject != null) {
                val intent = Intent(this, SubjectEditorActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(KEY_SUBJECT, subject)
                intent.putExtras(bundle);
                activitySubjectEditorLauncher.launch(intent)
            }
        }

        binding.btnSelect.setOnClickListener { it ->
            if (subject != null) {
                val i = Intent()
                val bundle = Bundle();
                bundle.putSerializable(KEY_SUBJECT, subject)
                i.putExtras(bundle);
                setResult(RESULT_OK, i)
                finish()
            }
        }

        binding.btnDelete.setOnClickListener {
            if (subject != null) {
                ApiRoutins.deleteSubject(serverAddress, subject?.id!!)
                updateListView()
            }
        }
    }

    private fun updateListView() {
        subjects = ApiRoutins.getSubjects(serverAddress)
        binding.lvSubjects.adapter = SubjectAdapter(this, subjects)
        binding.lvSubjects.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
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
}