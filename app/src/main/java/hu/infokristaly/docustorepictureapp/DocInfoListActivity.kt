package hu.infokristaly.docustorepictureapp

import android.content.Context
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
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocinfoListBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.DocInfoAdapter
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import java.text.SimpleDateFormat

class DocInfoListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocinfoListBinding
    private lateinit var appbar: Toolbar
    private lateinit var stored: StoredItems

    private var docInfo: DocInfo? = null
    private var docinfos = listOf<DocInfo>()

    val activityOrganizationEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            docInfo = null
        }
        updateListView()
    }

    val activityMainLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            updateListView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDocinfoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        stored = StoredItems()
        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(this,savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(this,sharedPrefs)
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        updateListView()

        binding.lvDocInfos.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                docInfo = docinfos.get(position)
            }

        binding.btnNew.setOnClickListener {
            val intent = Intent(this, DocInfoActivity::class.java)
            val docInfoNew = DocInfo(null, null,null, null, null, null)
            val bundle = Bundle();
            bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfoNew)
            intent.putExtras(bundle);
            activityOrganizationEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (docInfo != null) {
                val intent = Intent(this, DocInfoActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfo)
                intent.putExtras(bundle);
                activityOrganizationEditorLauncher.launch(intent)
            }
        }

        binding.btnDelete.setOnClickListener {
            if (docInfo != null) {
                ApiRoutins.deleteDocInfo(this, docInfo!!.id!!)
                docInfo = null
                stored.lastIFileInfoId = -1
                val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
                stored.saveState(this, sharedPrefs)
                updateListView()
            }
        }

        binding.btnSelect.setOnClickListener { it ->
            if (docInfo != null) {
                val intent = Intent(this, MainActivity::class.java)
                val bundle = Bundle();
                stored.lastIFileInfoId > -1
                stored.imageFilePath = ""
                val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
                stored.saveState(this, sharedPrefs)
                bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfo)
                bundle.putSerializable(getString(R.string.KEY_IMAGEPATH), "")
                intent.putExtras(bundle);
                activityMainLauncher.launch(intent)
            }
        }
    }

    private fun updateListView() {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        docinfos = ApiRoutins.getDocInfos(this)
        binding.lvDocInfos.adapter = DocInfoAdapter(this, docinfos)
        binding.lvDocInfos.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(this,savedInstanceState)
        docInfo = stored.docInfo
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
        stored.saveInstanceState(this,outState)
    }

    override fun onPause() {
        super.onPause()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(this,sharedPrefs)
    }

    override fun onStop() {
        super.onStop()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(this,sharedPrefs)
    }

}