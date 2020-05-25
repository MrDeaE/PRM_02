package com.example.prm_02.activities

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.example.prm_02.MainViewModel
import com.example.prm_02.R
import com.example.prm_02.helpers.ImageAdapter
import com.example.prm_02.services.LocationService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


private const val REQUEST_CODE_PERMISSIONS_CAMERA = 10
private const val REQUEST_CODE_PERMISSIONS_LOCATION = 11
const val EXTRA_IMAGE_URI = "image_uri_extra"

class MainActivity : AppCompatActivity() {

    companion object{
        var ACTIVITY: Activity? = null
    }

    lateinit var mainViewModel: MainViewModel
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        ACTIVITY = this

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        this.mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        mainViewModel.imageObjects.observe(this, androidx.lifecycle.Observer {
            Log.d("db image count", it.size.toString())
            myAdapter.submitList(it)
            myAdapter.notifyDataSetChanged()
        })

        initRecyclerView()

        fab_add_photo.setOnClickListener { view ->
            if(this.allPermissionsGranted) {
                dispatchTakePictureIntent()
            } else{
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS_CAMERA)
            }
        }

        ActivityCompat.requestPermissions(this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS_LOCATION
        )

        startLocationService()

    }

    override fun onResume() {
        super.onResume()
        mainViewModel.launchSelectFromDb(this)
    }

    private lateinit var myAdapter: ImageAdapter

    private fun initRecyclerView() {
        image_list.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            myAdapter = ImageAdapter(this@MainActivity)
            adapter = myAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager: ActivityManager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.example.prm2.services.LocationService" == service.service.getClassName()) {
                return true
            }
        }
        return false
    }

    var allPermissionsGranted = false

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.allPermissionsGranted = true
            }
            if (this.allPermissionsGranted) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == REQUEST_CODE_PERMISSIONS_LOCATION) {
            startLocationService()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                this.startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

// camera

    val REQUEST_TAKE_PHOTO = 5
    var photoUri : Uri? = null

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).let { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e("main activity", "error creating file $ex")
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.prm_02.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    this.photoUri = photoURI
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            if (this.photoUri != null) {
                Log.d("photouri", this.photoUri.toString())

                val intent = Intent(this, NoteActivity::class.java)
                intent.putExtra(EXTRA_IMAGE_URI, photoUri)
                startActivity(intent)

                this.photoUri = null
            } else {
                Toast.makeText(this, "Can't get photo URI", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}