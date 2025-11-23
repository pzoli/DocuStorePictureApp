package hu.infokristaly.docustorepictureapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hu.infokristaly.docustorepictureapp.databinding.ActivitySubjectListBinding
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.SubjectAdapter
import hu.infokristaly.docustorepictureapp.model.DocumentSubject
import java.util.Optional

class SubjectListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectListBinding
    private lateinit var appbar: Toolbar

    private var subject: Optional<DocumentSubject> = Optional.empty()
    private var subjects: Optional<List<DocumentSubject>> = Optional.of(listOf())

    val activitySubjectEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            subject = Optional.empty()
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

        val self = this
        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        updateListView()

        binding.lvSubjects.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                subject = Optional.of(subjects.get().get(position))
            }

        binding.btnNew.setOnClickListener {
            val intent = Intent(this, SubjectEditorActivity::class.java)
            val subjectNew = DocumentSubject(null,"")
            val bundle = Bundle();
            bundle.putSerializable(getString(R.string.KEY_SUBJECT), subjectNew)
            intent.putExtras(bundle);
            activitySubjectEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (subject.isPresent) {
                val intent = Intent(this, SubjectEditorActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_SUBJECT), subject.get())
                intent.putExtras(bundle);
                activitySubjectEditorLauncher.launch(intent)
            }
        }

        binding.btnSelect.setOnClickListener { it ->
            if (subject.isPresent) {
                val i = Intent()
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_SUBJECT), subject.get())
                i.putExtras(bundle);
                setResult(RESULT_OK, i)
                finish()
            }
        }

        binding.btnDelete.setOnClickListener {
            if (subject.isPresent) {
                try {
                    ApiRoutins.deleteSubject(this, subject.get().id!!)
                    subject = Optional.empty()
                    updateListView()
                } catch (e:Exception) {
                    Toast.makeText(self,e.toString(),Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateListView() {
        try {
            subjects = ApiRoutins.getSubjects(this)
        } catch (e:Exception) {
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show()
            subjects = Optional.of(listOf())
        }
        binding.lvSubjects.adapter = SubjectAdapter(this, subjects.get())
        binding.lvSubjects.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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