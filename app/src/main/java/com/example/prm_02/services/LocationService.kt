package com.example.prm_02.services

import com.example.prm_02.R
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.example.prm_02.models.SettingsModel
import com.example.prm_02.db.AppDatabase
import com.example.prm_02.activities.ImageActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class LocationService: Service() {

    val CHANNEL_ID = "my_channel_01"
    val TAG = "LocationService"
    val UPDATE_INTERVAL = 5000L // 5 sec
    val FASTEST_INTERVAL = 2000L // 2 sec

    lateinit var locationClient: FusedLocationProviderClient

    var notificatedImages = arrayListOf<Int>(-1)

    companion object {
        var currentLocation: Location? = null
        var currentAddress: String? = null
        var currentCountry: String? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        // create notification channel
        val channel = NotificationChannel(CHANNEL_ID, "My channel", NotificationManager.IMPORTANCE_DEFAULT)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("").build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        getLocation()
        return START_NOT_STICKY
    }

    fun getLocation() {
        // create location request
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL

        // check permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "stop location service")
            stopSelf()
            return
        }
        Log.d(TAG, "get location information")

        // handle location
        var locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    Log.d(TAG, "Got location $location")
                    currentLocation = location

                    // map to city
                    val geocoder = Geocoder(this@LocationService, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    currentAddress = addresses[0].getAddressLine(0)
                    currentCountry = addresses[0].countryName

                    GlobalScope.launch {
                        // location range radius
                        val settings = SettingsModel()
                        val radius = settings.range

                        // search all images
                        val db = Room.databaseBuilder(
                            this@LocationService,
                            AppDatabase::class.java,
                            AppDatabase.DATABASE_NAME
                        ).build()
                        val dao = db.imageDao()
                        val allImages = dao.getAll()
                        for (image in allImages) {
                            if (image.lat != "" && image.lon != "") {
                                var imageLocation = Location("image location")
                                imageLocation.latitude = image.lat!!.toDouble()
                                imageLocation.longitude = image.lon!!.toDouble()
                                val distance = imageLocation.distanceTo(currentLocation)

                                if (distance/1000 <= radius) {
                                    val intent = Intent(this@LocationService, ImageActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    intent.putExtra(ImageActivity.EXTRA_IMAGE_PATH, image.imageLocation)
                                    intent.putExtra(ImageActivity.EXTRA_NOTE_TEXT, image.note)
                                    intent.putExtra(ImageActivity.EXTRA_DATE_TEXT, image.date)
                                    intent.putExtra(ImageActivity.EXTRA_LOC_TEXT, image.loc)

                                    val pendingIntent = PendingIntent.getActivity(this@LocationService, 0, intent, 0)

                                    val addresses: List<Address> = geocoder.getFromLocation(image.lat.toDouble(), image.lon.toDouble(), 1)

                                    val notifBuilder = NotificationCompat.Builder(this@LocationService, CHANNEL_ID)
                                        .setContentTitle("Byłeś tutaj!")
                                        .setContentText("Witamy ponownie w "+addresses[0].locality+", "+addresses[0].countryName)
                                        .setSmallIcon(R.drawable.camera_icon)
                                        .setContentIntent(pendingIntent)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                                    // check if we don't spam notifs
                                    if (!notificatedImages.contains(image.id)) {
                                        Log.d(TAG, "Post notification")
                                        with(NotificationManagerCompat.from(this@LocationService)) {
                                            notify(100+image.id, notifBuilder.build())
                                        }
                                        notificatedImages.add(image.id)
                                    } else {
                                        Log.d(TAG, "Skip notification")
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }
}

