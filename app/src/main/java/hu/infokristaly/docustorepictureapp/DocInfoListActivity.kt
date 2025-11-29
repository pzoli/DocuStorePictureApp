package hu.infokristaly.docustorepictureapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocinfoListBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins.Companion.getSharedPrefProp
import hu.infokristaly.docustorepictureapp.utils.ItemAdapter
import hu.infokristaly.docustorepictureapp.utils.ItemViewModel
import hu.infokristaly.docustorepictureapp.utils.StoredItems


class DocInfoListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocinfoListBinding
    private lateinit var appbar: Toolbar
    private lateinit var stored: StoredItems
    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    private val selectedDocInfos: MutableList<DocInfo> = mutableListOf()

    private var docInfo: DocInfo? = null
    var selectedPositions: MutableList<Int> = mutableListOf()

    val activitySettingsLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        updateSettings()
        updateRecyclerView()
        clearSelections()
    }

    private fun clearSelections() {
        stored.selectedLocation = null
        stored.selectedSubject = null
        stored.selectedOrganization = null
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(this, sharedPrefs)
    }

    val activityDocInfoEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        docInfo = null
        updateRecyclerView()
    }

    val activityMainLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            updateRecyclerView()
        }
    }

    private fun updateRecyclerView() {
        selectedPositions.clear()
        selectedDocInfos.clear()

        viewModel.currentPage = 1
        viewModel.items.value = listOf()
        viewModel.loadNextPage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val self = this

        viewModel = ViewModelProvider(this).get(ItemViewModel::class.java)
        updateSettings()

        binding = ActivityDocinfoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        stored = StoredItems()
        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(this, savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(this, sharedPrefs)
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val layoutManager = binding.lvDocInfos.layoutManager as LinearLayoutManager
        adapter = ItemAdapter(emptyList(), ::isPositionSelected)
        binding.lvDocInfos.adapter = adapter
        viewModel.items.observe(this) { newItems ->
            adapter.updateItems(newItems)
        }
        adapter.onItemClickListener = ::onSelectItem
        adapter.onItemLongClickListener = ::onTouchItem
        binding.lvDocInfos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Ha már nincs több adat, ne töltsön be
                // if (isLastPage) return

                // A betöltést akkor indítjuk el, ha görgettünk, és a lista végéhez közelítünk
                if (dy > 0) { // Csak lefelé görgetéskor
                    // A küszöbérték (threshold): Akkor kezdje el a betöltést, ha már csak 5 elem maradt
                    val threshold = 5
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - threshold) {
                        viewModel.loadNextPage()
                    }
                }
            }
        })

        // Első betöltés elindítása
        if (viewModel.items.value.isNullOrEmpty()) {
            viewModel.loadNextPage()
        }

        binding.btnNew.setOnClickListener {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(this, sharedPrefs)
            stored.docInfo = null
            stored.selectedOrganization = null
            stored.selectedSubject = null
            stored.imageFilePath = ""
            stored.saveState(this, sharedPrefs)

            val intent = Intent(this, DocInfoActivity::class.java)
            val docInfoNew =
                DocInfo(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    stored.selectedLocation
                )
            val bundle = Bundle();
            bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfoNew)
            intent.putExtras(bundle);
            activityDocInfoEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (docInfo != null) {
                stored.docInfo = docInfo
                stored.selectedLocation = docInfo!!.docLocation
                stored.selectedOrganization = docInfo!!.organization
                stored.selectedSubject = docInfo!!.subject
                val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
                stored.saveState(this, sharedPrefs)

                val intent = Intent(this, DocInfoActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfo)
                intent.putExtras(bundle);
                activityDocInfoEditorLauncher.launch(intent)
            }
        }

        binding.btnDelete.setOnClickListener {
            if (selectedDocInfos.isNotEmpty()) {
                val context = this
                val alert: AlertDialog.Builder = AlertDialog.Builder(this)
                alert.setTitle("Delete entry")
                alert.setMessage("Are you sure you want to delete?")
                val self = this
                alert.setPositiveButton(
                    android.R.string.ok,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            for (item in selectedDocInfos) {
                                try {
                                    ApiRoutins.deleteDocInfo(context, item.id!!)
                                } catch (e: Exception) {
                                    Toast.makeText(self, e.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                            docInfo = null
                            selectedDocInfos.clear()
                            selectedPositions.clear()
                            stored.lastIFileInfoId = -1
                            val sharedPrefs =
                                getSharedPreferences(
                                    "my_activity_prefs",
                                    Context.MODE_PRIVATE
                                )
                            stored.saveState(context, sharedPrefs)
                            updateRecyclerView()
                        }
                    }
                )
                alert.setNegativeButton(android.R.string.cancel,
                    DialogInterface.OnClickListener { dialog, which -> // close dialog
                        dialog.cancel()
                    })
                alert.show()

            }
        }

        binding.btnSelect.setOnClickListener { it ->
            if (docInfo != null) {
                val intent = Intent(this, MainActivity::class.java)
                val bundle = Bundle();
                stored.lastIFileInfoId = -1
                stored.imageFilePath = ""
                stored.docInfo = docInfo
                val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
                stored.saveState(this, sharedPrefs)

                bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfo)
                bundle.putSerializable(getString(R.string.KEY_IMAGEPATH), "")
                intent.putExtras(bundle);
                activityMainLauncher.launch(intent)
            }
        }
        binding.swipeRefreshLayout.setOnRefreshListener(OnRefreshListener {
            updateRecyclerView()
            binding.swipeRefreshLayout.setRefreshing(false)
        })
    }

    private fun updateSettings() {
        viewModel.userName = getSharedPrefProp(this, this.getString(R.string.KEY_USERNAME))
        viewModel.password = getSharedPrefProp(this, this.getString(R.string.KEY_PASSWORD))
        viewModel.serverAddress =
            getSharedPrefProp(this, this.getString(R.string.KEY_SERVERADDRESS))
    }

    fun onSelectItem(docInfo: DocInfo, position: Int): Boolean {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        var isSelected = false
        var selectedItem = selectedDocInfos.firstOrNull({ item -> docInfo.equals(item) })
        if (selectedItem != null) {
            selectedDocInfos.remove(selectedItem)
        } else {
            selectedDocInfos.add(docInfo)
            isSelected = true
        }
        this.docInfo = if (isSelected) docInfo else null
        return isSelected
    }

    fun onTouchItem(docInfo: DocInfo, position: Int): Boolean {
        selectedDocInfos.clear()
        selectedDocInfos.add(docInfo)
        this.docInfo = docInfo
        selectedPositions.forEach { pos ->
            if (pos != position) {
                adapter.notifyItemChanged(pos)
            }
        }
        selectedPositions.clear()
        selectedPositions.add(position)

        return true
    }

    private fun isPositionSelected(position: Int): Boolean {
        return selectedPositions.contains(position)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(this, savedInstanceState)
        docInfo = stored.docInfo
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.custom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.m_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                activitySettingsLauncher.launch(intent)
            }

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stored.saveInstanceState(this, outState)
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