package com.serubii.zonereader.ui.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.serubii.zonereader.R
import com.serubii.zonereader.helpers.*
import com.serubii.zonereader.helpers.analyzers.LegacyFirebaseAnalyzer
import com.serubii.zonereader.room.entities.Document
import com.serubii.zonereader.room.viewmodels.DocumentViewModel
import com.serubii.zonereader.ui.adapters.HomeAdapter
import com.serubii.zonereader.ui.interfaces.SimpleInterface
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName

    private lateinit var toolbar: Toolbar

    private lateinit var fab: FloatingActionButton

    private lateinit var recyclerView: RecyclerView

    private lateinit var testImage: ImageView
    private lateinit var adapter: HomeAdapter
    private lateinit var allItems: ArrayList<Document>
    private lateinit var viewModel: DocumentViewModel
    private lateinit var photoURI: Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setup ui elements
        setupElements()
        setSupportActionBar(toolbar)

        // check for permissions
        checkPermission()

        // Recycler lifecycle
        viewModel = ViewModelProviders.of(this).get(DocumentViewModel::class.java)
        setupRecycler()
        setupAdapter()
    }

    private val anInterface: SimpleInterface = object : SimpleInterface {
        override fun didTapAt(index: Int) {
            val item = allItems[index]
            val intent = Intent(applicationContext, ResultsActivity::class.java)

            intent.putExtra("id", item.id)
            intent.putExtra("editMode", true)

            val bundle = setupBundle(item)

            for (key in bundle.keySet()) {
                Log.i("Bundle Debug to send", key + " = \"" + bundle[key] + "\"")
            }
            intent.putExtras(bundle)
            startActivity(intent)
        }

        private fun setupBundle(item: Document): Bundle {
            // send data through a bundle
            val bundle = Bundle()
            bundle.putString(preference_code_id_type, item.id_type)
            bundle.putString(preference_code_issue_country, item.issuing_country)
            bundle.putString(preference_code_document_number, item.document_number)
            bundle.putString(preference_code_optional_info, item.optional_info)
            bundle.putString(preference_code_dob, item.date_of_birth)
            bundle.putString(preference_code_gender, item.gender)
            bundle.putString(preference_code_expiration, item.expiration)
            bundle.putString(preference_code_nationality, item.nationality)
            bundle.putString(preference_code_surname, item.surname)
            bundle.putString(preference_code_given_name, item.given_name)
            bundle.putString(preference_code_photo_string, item.initial_uri)
            bundle.putString(preference_code_photo_front_string, item.front_uri)
            bundle.putString(preference_code_photo_back_string, item.back_uri)

            return bundle
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun checkPermission() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) { /* ... */
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) { /* ... */
                    }
                }).check()
    }

    private fun setupElements() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.homeRecyclerView)
        testImage = findViewById(R.id.TestImage)

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            Snackbar.make(it, "Time to Scan a Document", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            doScanNow()
        }


    }

    private fun checkCameraType(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean("CameraType", false)
    }


    private fun doScanNow() {
        // check permissions
        checkPermission()
        val cameraType = checkCameraType()

        if (!cameraType) {
            // use default camera
            startContinuousCamera()
        } else {
            // use legacy camera
            startReliabilityCamera()
        }
    }


    private fun setupRecycler() {
        val layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = DefaultItemAnimator()
        allItems = ArrayList()
        adapter = HomeAdapter(applicationContext, allItems, anInterface)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager
    }

    private fun setupAdapter() {
        viewModel.allItemsByScanTimeDesc.observe(this, Observer { documents: List<Document>? ->
            if (documents != null) {
                Log.d(TAG, "setup_adapter: $documents")
                allItems.clear()
                allItems.addAll(documents)
                adapter.notifyDataSetChanged()
            }
        })
    }

    private fun startContinuousCamera() {
        startActivity(Intent(this, ContinuousCameraActivity::class.java))
    }

    private fun startReliabilityCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, R.string.errorFileCreate, Toast.LENGTH_LONG).show()
                run { e.printStackTrace() }
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(
                        this@MainActivity,
                        "com.serubii.mrz",  //(use your app signature + ".provider" )
                        photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // create a image file name
        val timeStamp = SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                imageFileName,  /* prefix*/
                ".jpg",  /* suffix*/
                storageDir /* directory*/
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    var bm: Bitmap? = null
                    try {
                        val `is` = this.contentResolver.openInputStream(photoURI)
                        val options = BitmapFactory.Options()
                        bm = BitmapFactory.decodeStream(`is`, null, options)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                    if (bm == null) {
                        Log.w(TAG, "BM WAS NULL")
                    } else {
                        Log.i(TAG, "BM IS $bm")
                    }
                    Log.i(TAG, "BM IS $photoURI")
                    Log.i(TAG, "BM IS " + photoURI.path)



                    // execute decode from bitmap
                    try {
                        val toast = Toast.makeText(applicationContext, "Processing Image", Toast.LENGTH_LONG)
                        toast.show()
                        sendToLegacyHelper(applicationContext, photoURI)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun sendToLegacyHelper(context: Context, uri: Uri?) {
        Log.i(TAG, "Sent to Helper $uri")
        val FA = LegacyFirebaseAnalyzer()
        FA.initOCR(context, uri)
    }

    companion object {
        // TODO Cleanup old implementation of OCR handling
        private const val REQUEST_IMAGE_CAPTURE = 420
    }
}