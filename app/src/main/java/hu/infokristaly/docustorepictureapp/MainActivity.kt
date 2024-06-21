package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hu.infokristaly.docustorepictureapp.databinding.ActivityMainBinding
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.docustorepictureapp.network.NetworkClient
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import hu.infokristaly.forrasimageserver.entity.Subject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val MY_CAMERA_REQUEST_CODE = 100

    private lateinit var mScaleGestureDetector: ScaleGestureDetector
    private var mScaleFactor = 1.0f
    private lateinit var stored: StoredItems
    private var serverAddress = ""

    private var toolbar: Toolbar? = null

    val activityCropLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            resetImagePosAndScale()
            viewImage()
        }
    }

    val activityTakePictreLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            resetImagePosAndScale()
            viewImage()
        } else {
            deleteImage()
        }
    }

    fun deleteImage() {
        if (stored.imageFilePath != "") {
            val tempFile = File(stored.imageFilePath)
            tempFile.delete()
            binding.imageView.setImageBitmap(null)
            stored.imageFilePath = ""
        }
    }

    val activitySettingsLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        serverAddress = ApiRoutins.getSharedPrefProp(this, ApiRoutins.KEY_SERVERADDRESS)

    }

    private fun viewImage() {
        if (stored.imageFilePath != "") {
            val myBitmap = BitmapFactory.decodeFile(stored.imageFilePath)
            var bmRotated = myBitmap
            try {
                val exif = ExifInterface(File(stored.imageFilePath))
                val orientation = exif!!.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
                bmRotated = rotateBitmap(myBitmap, orientation)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            binding.imageView.setImageBitmap(bmRotated)
        }
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix: Matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1F, 1F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180F)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180F)
                matrix.postScale(-1F, 1F)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90F)
                matrix.postScale(-1F, 1F)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90F)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90F)
                matrix.postScale(-1F, 1F)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90F)
            else -> return bitmap
        }
        try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            return bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        stored = StoredItems()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState != null) {
            stored.restoreStateFromBundle(savedInstanceState)
        } else {
            val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
            stored.restoreFromSharedPrefs(sharedPrefs)
        }

        toolbar = findViewById(R.id.mytoolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        serverAddress = ApiRoutins.getSharedPrefProp(this, ApiRoutins.KEY_SERVERADDRESS)

        mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        if (stored.imageFilePath != "") {
            viewImage()
        }

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), MY_CAMERA_REQUEST_CODE
            );
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stored.saveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(savedInstanceState)
        serverAddress = ApiRoutins.getSharedPrefProp(this, ApiRoutins.KEY_SERVERADDRESS)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.image_captire_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.m_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                activitySettingsLauncher.launch(intent)
            }

            R.id.m_crop -> {
                if (stored.imageFilePath != "") {
                    val intent = Intent("com.android.camera.action.CROP")
                    val uri = FileProvider.getUriForFile(
                        this,
                        applicationContext.packageName + ".provider",
                        File(stored.imageFilePath)
                    )
                    intent.setDataAndType(uri, "image/*")
                    intent.putExtra("crop", "true")
                    //intent.putExtra("aspectX", 1)
                    //intent.putExtra("aspectY", 1)
                    intent.putExtra("output", uri)
                    intent.putExtra("return-data", false);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        activityCropLauncher.launch(intent)
                    } catch (e: Exception) {
                        Log.e("MainActivity", e.message.toString())
                    }
                }
            }

            R.id.m_takeapicture -> {
                deleteImage()
                takeAPicture()
            }

            R.id.m_upload -> {
                if (stored.imageFilePath != "") {
                    val subject = Subject(1, "test")
                    val organization = Organization(1, "Organ1", "", "")

                    NetworkClient().uploadToServer(
                        this,
                        serverAddress,
                        stored.docInfo,
                        stored.imageFilePath
                    )
                }
            }

            R.id.m_delete -> {
                deleteImage()
            }

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun takeAPicture() {
        var photoFile: File? = null
        try {
            photoFile = createImageFile();
        } catch (ex: IOException) {
            // Error occurred while creating the File
        }
        if (photoFile != null) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val author = "${packageName}.provider"
            try {
                val photoURI: Uri = FileProvider.getUriForFile(this, author, photoFile);
                cameraIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoURI
                )
                activityTakePictreLauncher.launch(cameraIntent)
            } catch (ex: Exception) {
                Log.e("MainActivity", ex.message.toString())
            }
        }
    }

    fun resetImagePosAndScale() {
        x = 0F
        y = 0F
        binding.imageView.x = 0F
        binding.imageView.y = 0F
        mScaleFactor = 1.0f
        binding.imageView.scaleX = mScaleFactor
        binding.imageView.scaleY = mScaleFactor
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    var x: Float = 0F
    var y: Float = 0F
    var dx: Float = 0F
    var dy: Float = 0F

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                x = motionEvent.x
                y = motionEvent.y
            }

            MotionEvent.ACTION_MOVE -> {
                dx = motionEvent.x - x
                dy = motionEvent.y - y

                binding.imageView.x = binding.imageView.x + dx
                binding.imageView.y = binding.imageView.y + dy

                x = motionEvent.x
                y = motionEvent.y
            }
        }
        mScaleGestureDetector.onTouchEvent(motionEvent)
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = max(0.1f, min(mScaleFactor, 10.0f))
            binding.imageView.scaleX = mScaleFactor
            binding.imageView.scaleY = mScaleFactor
            return true
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"
        val storageDir =
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        stored.imageFilePath = image.absolutePath
        return image
    }

    override fun onPause() {
        super.onPause()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(sharedPrefs)
    }

    override fun onStop() {
        super.onStop()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(sharedPrefs)
    }

}