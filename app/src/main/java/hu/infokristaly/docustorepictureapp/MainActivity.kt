package hu.infokristaly.docustorepictureapp

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Insets
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.exifinterface.media.ExifInterface
import hu.infokristaly.docustorepictureapp.databinding.ActivityMainBinding
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.FileInfo
import hu.infokristaly.docustorepictureapp.network.NetworkClient
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins
import hu.infokristaly.docustorepictureapp.utils.StoredItems
import java.io.File
import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val MY_CAMERA_REQUEST_CODE = 100

    private lateinit var mScaleGestureDetector: ScaleGestureDetector
    private var mScaleFactor = 1.0f
    private lateinit var stored: StoredItems

    val IMAGENAME_FROM_SERVER = "IMG_FROM_SERVER"
    val IMAGE_SWITCH_MARGIN = 50

    private var toolbar: Toolbar? = null
    var fileList: List<FileInfo>? = null

    var  photoFile: File? = null
    var moveToTarget = true

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

    val activityChooseAPictreLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = result.data!!.data
            try {
                /*
                val selectedImageBitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    selectedImageUri
                )
                binding.imageView.setImageBitmap(
                    selectedImageBitmap
                );
                 */
                val photoPath = getRealPathFromURI(selectedImageUri!!)!!
                val target = createImageFile()
                Files.copy(File(photoPath).toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                resetImagePosAndScale()
                viewImage()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            deleteImage()
        }
    }

    private fun getRealPathFromURI(contentURI: Uri): String? {
        val result: String?
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    fun deleteImage() {
        if (stored.imageFilePath != "") {
            val tempFile = File(stored.imageFilePath)
            //deleteFromDatabase(File(tempFile.name))
            //queryImages(tempFile.name)
            tempFile.delete()
            binding.imageView.setImageBitmap(null)
            stored.imageFilePath = ""
        }
    }

    fun setFileInfo(fileInfo: FileInfo) {
        stored.lastIFileInfoId = fileInfo.id!!
    }

    fun setFileName(fileName: String) {
        stored.imageFilePath = fileName
    }

    fun getFileUriFromFileName(fileName: String): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }

        return null
    }
    fun deleteFromDatabase(fileInfoId: Long) {
        try {
            ApiRoutins.deleteFileInfo(this, fileInfoId)
        } catch (e:Exception) {
            Log.e("MainActivity", e.message.toString())
        }
    }
    val activitySettingsLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->

    }

    fun updateFileList() {
        fileList = ApiRoutins.getFileInfosForDocInfo(this, stored.docInfo!!.id)
    }
    fun queryImages(fileNameParam:String) {
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
        )

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val size = it.getString(sizeColumn)
                val date = it.getString(dateColumn)
                if (name.trim().startsWith(fileNameParam.trim()) || name.trim().endsWith(fileNameParam.trim())) {
                    Log.i("MainActivity", name)
                }
                Log.d("IMAGE", "$id $name $size $date")
            }
        }
    }

    private fun correctOrientationByExif(myBitmap:  Bitmap): Bitmap? {
        try {
            val exif = ExifInterface(File(stored.imageFilePath))
            val orientation = exif!!.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            return rotateBitmap(myBitmap, orientation)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }
    private fun viewImage() {
        if (stored.imageFilePath != "") {
            try {
                val myBitmap = BitmapFactory.decodeFile(stored.imageFilePath)
                binding.imageView.setImageBitmap(correctOrientationByExif(myBitmap))
            } catch (e:Exception) {
                Log.e("MainActivity",e.message.toString())
            }
        }
        else
            binding.imageView.setImageBitmap(null)
    }

    fun loadImageById(fineInfo:FileInfo) {
        val byteArray = ApiRoutins.getImage(this, fineInfo.id!!)
        try {
            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            Files.write(Paths.get(stored.imageFilePath), byteArray)
            binding.imageView.setImageBitmap(correctOrientationByExif(bmp))
        } catch (e:Exception) {
            Log.e("MainActivity",e.message.toString())
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

        binding = ActivityMainBinding.inflate(layoutInflater)
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

        toolbar = findViewById(R.id.mytoolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        if (intent.hasExtra(getString(R.string.KEY_DOCINFO)) && (stored.imageFilePath == "" || File(stored.imageFilePath).name.startsWith(IMAGENAME_FROM_SERVER))) {
            stored.docInfo =
                intent.getSerializableExtra(getString(R.string.KEY_DOCINFO)) as DocInfo
            if (stored.docInfo != null && stored.docInfo!!.id != null) {
                fileList = ApiRoutins.getFileInfosForDocInfo(this, stored.docInfo!!.id)
                if (fileList!!.isNotEmpty()) {
                    val fileInfo = fileList!!.firstOrNull {
                        it.id!!.equals(
                            stored.lastIFileInfoId
                        )
                    }
                    val currentFileInfo = if (stored.lastIFileInfoId > -1 && (fileInfo != null)) fileInfo else fileList!![0]
                    stored.lastIFileInfoId = currentFileInfo.id!!
                    val storageDir =
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path
                    val fileName = IMAGENAME_FROM_SERVER + ".${currentFileInfo.uniqueFileName.substringAfter(".")}"
                    stored.imageFilePath = Paths.get(storageDir, fileName ).toString()
                    loadImageById(currentFileInfo)
                }
            }
        } else if (stored.imageFilePath != "") {
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
        stored.saveInstanceState(this,outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        stored.restoreStateFromBundle(this,savedInstanceState)
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
                stored.lastIFileInfoId = -1
                stored.imageFilePath = ""
                takeAPicture()
            }

            R.id.m_choosepicture -> {
                chooseAPicture()
            }

            R.id.m_upload -> {
                if (stored.imageFilePath != "") {
                    if (File(stored.imageFilePath).name.startsWith(IMAGENAME_FROM_SERVER)) {
                        NetworkClient().updateOnServer(this, stored.lastIFileInfoId, stored.imageFilePath)
                    } else {
                        NetworkClient().uploadToServer(
                            this,
                            stored.docInfo,
                            stored.imageFilePath
                        )
                    }
                }
                viewImage()
            }

            R.id.m_delete -> {
                val alert: AlertDialog.Builder = AlertDialog.Builder(this)
                alert.setTitle("Delete entry")
                alert.setMessage("Are you sure you want to delete?")
                alert.setPositiveButton(
                    android.R.string.yes,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            deleteImage()
                            deleteFromDatabase(stored.lastIFileInfoId)
                            if (fileList != null && fileList!!.isNotEmpty()) {
                                updateFileList()
                                val firstFileInfo = fileList!!.get(0)
                                val fileName = "${IMAGENAME_FROM_SERVER}.${firstFileInfo.uniqueFileName.substringAfter(".")}"
                                val storageDir =
                                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path
                                stored.imageFilePath = Paths.get(storageDir, fileName ).toString()
                                stored.lastIFileInfoId = firstFileInfo.id!!
                                loadImageById(firstFileInfo)
                            }
                        }
                    })
                alert.setNegativeButton(android.R.string.no,
                    DialogInterface.OnClickListener { dialog, which -> // close dialog
                        dialog.cancel()
                    })
                alert.show()
            }

            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun takeAPicture() {
        photoFile = null
        moveToTarget = false
        try {
            photoFile = createImageFile();
        } catch (ex: IOException) {
            // Error occurred while creating the File
        }
        if (photoFile != null) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val author = "${packageName}.provider"
            try {
                val photoURI: Uri = FileProvider.getUriForFile(this, author, photoFile!!);
                cameraIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoURI
                )
                cameraIntent.putExtra("saveToGallery", false)
                activityTakePictreLauncher.launch(cameraIntent)
            } catch (ex: Exception) {
                Log.e("MainActivity", ex.message.toString())
            }
        }
    }

    private fun chooseAPicture() {
        try {
            val i = Intent()
            i.setType("image/*")
            i.setAction(Intent.ACTION_GET_CONTENT)
            activityChooseAPictreLauncher.launch(i)
        } catch (ex: Exception) {
            Log.e("MainActivity", ex.message.toString())
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

    fun getScreenWidth(activity: Activity): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            return windowMetrics.bounds.width() //- insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }
    }
    var x: Float = 0F
    var y: Float = 0F
    var dx: Float = 0F
    var dy: Float = 0F
    var downX: Float = 0F

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                x = motionEvent.x
                y = motionEvent.y
                downX = binding.imageView.x
            }

            MotionEvent.ACTION_MOVE -> {
                dx = motionEvent.x - x
                dy = motionEvent.y - y

                binding.imageView.x = binding.imageView.x + dx
                binding.imageView.y = binding.imageView.y + dy

                x = motionEvent.x
                y = motionEvent.y
            }

            MotionEvent.ACTION_UP -> {
                val downDiff = downX - binding.imageView.x
                if (mScaleFactor <= 1 && downDiff.absoluteValue > IMAGE_SWITCH_MARGIN && fileList != null && fileList!!.isNotEmpty()) {
                    val fileInfo = fileList!!.firstOrNull {
                        it.id!!.equals(
                            stored.lastIFileInfoId
                        )
                    }
                    var idx = fileList!!.indexOf(fileInfo)
                    var isFirst = idx == 0
                    var isLast = idx == fileList!!.size - 1
                    if (downDiff  < 0) {
                        idx = max(0, idx - 1)
                    } else {
                        idx = min(fileList!!.size - 1,idx + 1)
                    }
                    val width = getScreenWidth(this)
                    var destinationX = if (downDiff < 0)  width * 1f else width * -1f
                    if ((isFirst && destinationX > 0) || (isLast && destinationX < 0)) {
                        destinationX = 0f
                    }
                    ObjectAnimator.ofFloat(binding.imageView,"x", destinationX).apply {
                        duration = 200
                        start()
                    }.doOnEnd {
                        val currentFileInfo = fileList!!.get(idx)
                        stored.lastIFileInfoId = currentFileInfo.id!!
                        loadImageById(currentFileInfo)
                        downX = binding.imageView.x
                        resetImagePosAndScale()
                    }


                }
            }
        }
        mScaleGestureDetector.onTouchEvent(motionEvent)
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = max(0.5f, min(mScaleFactor, 10.0f))
            binding.imageView.scaleX = mScaleFactor
            binding.imageView.scaleY = mScaleFactor
            downX = binding.imageView.x
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
        stored.saveState(this,sharedPrefs)
    }

    override fun onStop() {
        super.onStop()
        val sharedPrefs = getSharedPreferences("my_activity_prefs", Context.MODE_PRIVATE)
        stored.saveState(this,sharedPrefs)
    }

}