package hu.infokristaly.docustorepictureapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
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
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocinfoListBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins.Companion.getSharedPrefProp
import hu.infokristaly.docustorepictureapp.utils.ItemAdapter
import hu.infokristaly.docustorepictureapp.utils.ItemViewModel
import hu.infokristaly.docustorepictureapp.utils.StoredItems


class DocInfoListActivity : AppCompatActivity(), FilterDialogListener {
    private lateinit var binding: ActivityDocinfoListBinding
    private lateinit var appbar: Toolbar
    private lateinit var stored: StoredItems
    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    private val selectedDocInfos: MutableList<DocInfo> = mutableListOf()
    var selectedPositions: MutableList<Int> = mutableListOf()
    private var multiSelectMode = false

    private var docInfo: DocInfo? = null

    private val activitySettingsLauncher = registerForActivityResult<Intent, ActivityResult>(
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

    private val activityDocInfoEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        docInfo = null
        updateRecyclerView()
    }

    private val activityMainLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            updateRecyclerView()
        }
    }

    private fun updateRecyclerView() {
        selectedPositions.clear()
        selectedDocInfos.clear()
        multiSelectMode = false
        viewModel.currentPage = 1
        viewModel.items.value = listOf()
        viewModel.loadNextPage(stored.currentSearchTerm)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        adapter = ItemAdapter(emptyList(), ::isPositionSelected, ::modifyItem)
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

                if (dy > 0) {
                    val threshold = 5
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - threshold) {
                        viewModel.loadNextPage(stored.currentSearchTerm)
                    }
                }
            }
        })

        // Első betöltés elindítása
        if (viewModel.items.value.isNullOrEmpty()) {
            viewModel.loadNextPage(stored.currentSearchTerm)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            updateRecyclerView()
            binding.swipeRefreshLayout.setRefreshing(false)
        }
    }

    private fun modifyItem(item : DocInfo, view: View) {
        docInfo = item
        showPopupMenu(view)
    }

    private fun selectDocInfo() {
        if (docInfo != null && !multiSelectMode) {
            val intent = Intent(this, MainActivity::class.java)
            val bundle = Bundle()
            stored.lastIFileInfoId = -1
            stored.imageFilePath = ""
            stored.docInfo = docInfo
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.saveState(this, sharedPrefs)

            bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfo)
            bundle.putSerializable(getString(R.string.KEY_IMAGEPATH), "")
            intent.putExtras(bundle)
            activityMainLauncher.launch(intent)
        }

    }

    private fun deleteSelectedDocInfos() {
        if (selectedDocInfos.isNotEmpty()) {
            val context = this
            val alert: AlertDialog.Builder = AlertDialog.Builder(this)
            alert.setTitle("Delete entry")
            alert.setMessage("Are you sure you want to delete ${selectedDocInfos.size} item?")
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
            alert.setNegativeButton(android.R.string.cancel
            ) { dialog, _ -> // close dialog
                dialog.cancel()
            }
            alert.show()

        }

    }

    private fun modifyDocInfo() {
        if (docInfo != null && !multiSelectMode) {
            stored.docInfo = docInfo
            stored.selectedLocation = docInfo!!.docLocation
            stored.selectedOrganization = docInfo!!.organization
            stored.selectedSubject = docInfo!!.subject
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.saveState(this, sharedPrefs)

            val intent = Intent(this, DocInfoActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfo)
            intent.putExtras(bundle)
            activityDocInfoEditorLauncher.launch(intent)
        }
    }

    private fun createNewDocInfo() {
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
        val bundle = Bundle()
        bundle.putSerializable(getString(R.string.KEY_DOCINFO), docInfoNew)
        intent.putExtras(bundle)
        activityDocInfoEditorLauncher.launch(intent)

    }

    private fun updateSettings() {
        viewModel.userName = getSharedPrefProp(this, this.getString(R.string.KEY_USERNAME))
        viewModel.password = getSharedPrefProp(this, this.getString(R.string.KEY_PASSWORD))
        viewModel.serverAddress =
            getSharedPrefProp(this, this.getString(R.string.KEY_SERVERADDRESS))
    }

    private fun onSelectItem(docInfo: DocInfo, position: Int): Boolean {
        if (multiSelectMode) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
            } else {
                selectedPositions.add(position)
            }
            var isSelected = false
            val selectedItem = selectedDocInfos.contains(docInfo)
            if (selectedItem) {
                selectedDocInfos.remove(docInfo)
            } else {
                selectedDocInfos.add(docInfo)
                isSelected = true
            }
            return isSelected
        } else {
            val copy = mutableListOf<Int>()
            copy.addAll(selectedPositions)
            selectedPositions.clear()
            selectedPositions.add(position)
            selectedDocInfos.clear()
            selectedDocInfos.add(docInfo)
            this.docInfo = docInfo
            copy.forEach { pos ->
                adapter.notifyItemChanged(pos)
            }
            return true
        }
    }

    fun onTouchItem(docInfo: DocInfo, position: Int): Boolean {
        selectedDocInfos.clear()
        selectedDocInfos.add(docInfo)
        val copy = mutableListOf<Int>()
        copy.addAll(selectedPositions)
        selectedPositions.clear()
        selectedPositions.add(position)
        copy.forEach { pos ->
            adapter.notifyItemChanged(pos)
        }
        multiSelectMode = !multiSelectMode
        if (!multiSelectMode) {
            this.docInfo = docInfo
            Toast.makeText(this, "Select single document!",Toast.LENGTH_LONG).show()
        } else {
            this.docInfo = null
            Toast.makeText(this, "Select multiple documents!",Toast.LENGTH_LONG).show()
        }

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
            R.id.m_search -> {
                showFilterDialog()
            }
            R.id.m_delete -> {
                deleteSelectedDocInfos()
            }
            R.id.m_new -> {
                createNewDocInfo()
            }

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFilterDialog() {
        val dialog = SearchFilterDialogFragment.newInstance(stored.currentSearchTerm)

        // Beállítjuk a Listener-t magára az Activity-re
        dialog.listener = this

        // Megjelenítjük a dialógust
        dialog.show(supportFragmentManager, "SearchFilterDialog")
    }

    // A FilterDialogListener interfész megvalósítása
    override fun onFilterApplied(searchText: String) {
        // Frissítjük a feltételt
        stored.currentSearchTerm = searchText
        //TODO store search things
        // Indítjuk a szűrést az új feltétellel
        updateRecyclerView()
    }

    private fun showPopupMenu(view: View?) {
        if (!multiSelectMode) {
            val popup = PopupMenu(applicationContext, view)
            popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_item_edit -> {
                        modifyDocInfo()
                        true
                    }
                    R.id.menu_item_select -> {
                        selectDocInfo()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
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