package hu.infokristaly.docustorepictureapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import hu.infokristaly.docustorepictureapp.model.DocLocation
import hu.infokristaly.docustorepictureapp.model.DocumentDirection
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.network.NetworkClient
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import hu.infokristaly.docustorepictureapp.model.DocumentSubject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
        val self = this
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
                stored.docInfo =
                    intent.getSerializableExtra(getString(R.string.KEY_DOCINFO)) as DocInfo
                stored.selectedSubject = stored.docInfo!!.subject
                stored.selectedOrganization = stored.docInfo!!.organization
            }
        }

        updateView()

        val serverAddress =
            ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_SERVERADDRESS))
        val userName = ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_USERNAME))
        val password = ApiRoutins.getSharedPrefProp(this, getString(R.string.KEY_PASSWORD))

        if (serverAddress == "" || userName == "" || password == "") {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else {
            updateAutoComplette()
        }

        val myCalendar = Calendar.getInstance()

        val datePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel(myCalendar) // Updates TextView with the selected date
        }

        binding.btnDatePicker.setOnClickListener {
            if (stored.docInfo!!.createdAt !== null) {
                myCalendar.setTime(stored.docInfo!!.createdAt!!)
            }
            val year = myCalendar.get(Calendar.YEAR)
            val month = myCalendar.get(Calendar.MONTH)
            val day = myCalendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(
                self,
                datePicker,
                year,
                month,
                day
            ).show()
        }

        binding.btnSend.setOnClickListener {
            if (stored.selectedSubject != null && stored.selectedOrganization != null) {
                try {
                    if (stored.docInfo != null) {
                        stored.docInfo!!.subject = stored.selectedSubject
                        stored.docInfo!!.organization = stored.selectedOrganization
                        if (binding.etDatePicker.text.isEmpty()) {
                            stored.docInfo!!.createdAt = Date()
                        } else {
                            stored.docInfo!!.createdAt =
                                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                                    .parse(binding.etDatePicker.text.toString())
                        }
                        if (stored.docInfo!!.direction == null) {
                            stored.docInfo!!.direction = DocumentDirection.IN
                        }
                        stored.docInfo!!.comment = binding.etComment.text.toString()
                    } else {
                        stored.docInfo = DocInfo(
                            null,
                            stored.selectedSubject!!,
                            DocumentDirection.IN,
                            stored.selectedOrganization!!,
                            null,
                            stored.docInfo!!.createdAt,
                            stored.docInfo!!.comment,
                            null
                        )
                    }
                    NetworkClient()
                        .sendDocInfo(this, stored.docInfo)
                } catch (e: Exception) {
                    Toast.makeText(self, e.toString(), Toast.LENGTH_LONG).show()
                }
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

    private fun updateLabel(calendar: Calendar) {
        val myFormat = "yyyy-MM-dd" // Define the date format
        val sdf = SimpleDateFormat(
            myFormat,
            Locale.ENGLISH
        )
        binding.etDatePicker.setText(sdf.format(calendar.time))
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
        try {
            binding.etDatePicker.setText(
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(
                    stored.docInfo!!.createdAt
                )
            )
        } catch (e: Exception) {
            Log.e("Exception", e.toString())
        }
        binding.etComment.setText(stored.docInfo!!.comment)
    }

    private fun updateAutoComplette() {
        try {
            organizations = ApiRoutins.getOrganizations(this)
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            organizations = Optional.of(listOf())
        }

        try {
            subjects = ApiRoutins.getSubjects(this)
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
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