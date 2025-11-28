package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.amrdeveloper.treeview.TreeNode
import com.amrdeveloper.treeview.TreeViewAdapter
import com.amrdeveloper.treeview.TreeViewHolderFactory
import hu.infokristaly.docustorepictureapp.databinding.ActivityDocLocationBinding
import hu.infokristaly.docustorepictureapp.model.DocLocation
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import java.util.Optional


class DocLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocLocationBinding
    private lateinit var appbar: Toolbar
    private lateinit var treeViewAdapter: TreeViewAdapter
    private lateinit var stored: StoredItems
    private var location: Optional<DocLocation> = Optional.empty()

    val activityLocationEditorLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) updateTreeView()
    }

    private fun updateTreeView() {
        val roots: MutableList<TreeNode> = ArrayList()
        try {
            val rootList = ApiRoutins.getLocations(this, -1L)
            if (rootList.isPresent) {
                for (location in rootList.get()) {
                    val childsList = ApiRoutins.getLocations(this, location.id!!)
                    val root = TreeNode(
                        location,
                        if (childsList.isPresent && childsList.get()
                                .isEmpty()
                        ) R.layout.list_item_child else R.layout.list_item_root
                    )
                    roots.add(root)
                    buildTree(root, childsList)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }

        treeViewAdapter.updateTreeNodes(roots)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val self = this

        binding = ActivityDocLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appbar = findViewById(R.id.custom_appbar)
        setSupportActionBar(appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        stored = StoredItems()

        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(this, savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(this, sharedPrefs)
        }

        binding.btnNew.setOnClickListener {
            val intent = Intent(this, LocationEditorActivity::class.java)
            val location = DocLocation(null, "", if (location.isPresent) location.get() else null)
            val bundle = Bundle();
            bundle.putSerializable(getString(R.string.KEY_LOCATION), location)
            intent.putExtras(bundle);
            activityLocationEditorLauncher.launch(intent)
        }

        binding.btnModify.setOnClickListener {
            if (location.isPresent) {
                val intent = Intent(this, LocationEditorActivity::class.java)
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_LOCATION), location.get())
                intent.putExtras(bundle);
                activityLocationEditorLauncher.launch(intent)
            }
        }

        binding.btnDelete.setOnClickListener {
            if (location.isPresent) {
                try {
                    ApiRoutins.deleteLocation(this, location.get().id!!)
                    location = Optional.empty()
                    updateTreeView()
                } catch (e: Exception) {
                    Toast.makeText(self, e.toString(), Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnSelect.setOnClickListener {
            if (location.isPresent) {
                val i = Intent()
                val bundle = Bundle();
                bundle.putSerializable(getString(R.string.KEY_LOCATION), location.get())
                i.putExtras(bundle);
                setResult(RESULT_OK, i)
                finish()
            }
        }

        val factory =
            TreeViewHolderFactory { v: View, layout: Int ->
                CustomViewHolder(
                    v
                )
            }
        treeViewAdapter = TreeViewAdapter(factory)

        treeViewAdapter.setTreeNodeClickListener { treeNode: TreeNode, nodeView: View ->
            location =
                if (treeNode.isSelected) Optional.of(treeNode.value as DocLocation) else Optional.empty()

        }

        binding.recyclerView.setAdapter(treeViewAdapter)

        updateTreeView()

        binding.recyclerView.setLayoutManager(LinearLayoutManager(this));
    }

    private fun buildTree(parentNode: TreeNode, nodeList: Optional<List<DocLocation>>) {
        if (nodeList.isPresent) {
            for (node in nodeList.get()) {
                val childsList = ApiRoutins.getLocations(this, node.id!!)
                val child = TreeNode(
                    node,
                    if (childsList.isPresent && childsList.get()
                            .isEmpty()
                    ) R.layout.list_item_child else R.layout.list_item_root
                )
                parentNode.addChild(child)
                buildTree(child, childsList)
            }
        } else {

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