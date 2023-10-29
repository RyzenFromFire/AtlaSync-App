package com.atlasync.app

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.atlasync.app.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var pref: SharedPreferences
    private lateinit var ipPref: String
    private lateinit var baseURL: String
    private val client = OkHttpClient()
    private lateinit var roomInfoString: String
    private lateinit var backgroundImage: ImageView
    private lateinit var backgroundImageString: String
    private var displayMetrics = DisplayMetrics()
    private lateinit var ROOM_INFO_URL: String
    private lateinit var FLOOR_MAP_URL: String
    var lastRoomID: String = ""
    val LAST_ROOM_ID_KEY = "ROOM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        scanner = GmsBarcodeScanning.getClient(this, options)

        binding.syncFab.setOnClickListener { view ->
            Snackbar.make(view, "Sync FAB triggered", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        binding.scanFab.setOnClickListener { view -> doScan(view) }


        val windowHeight = this.windowManager.currentWindowMetrics.bounds.height()
        backgroundImage = binding.root.findViewById(R.id.backgroundImage)
        backgroundImage.maxHeight = windowHeight

        ROOM_INFO_URL = getString(R.string.room_info_url)
        FLOOR_MAP_URL = getString(R.string.floor_map_url)

//        println("ONCREATE CALLED")
//        println("lastRoomID = `$lastRoomID`")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (lastRoomID != "") {
            outState.putString(LAST_ROOM_ID_KEY, lastRoomID)
        }
//        println("SAVING STATE")
//        println("lastRoomID = `$lastRoomID`")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastRoomID = savedInstanceState.getString(LAST_ROOM_ID_KEY) ?: ""
        if (lastRoomID != "") {
            getRoomInfo(lastRoomID)
        }
//        println("RESTORING STATE")
    }

    override fun onResume() {
        super.onResume()
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        ipPref = pref.getString(getString(R.string.ip_pref_key), getString(R.string.default_ip))
            ?: getString(R.string.default_ip)
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        baseURL = "http://$ipPref:5000"
//        Toast.makeText(this, ipPref, Toast.LENGTH_SHORT).show()

//        println("ONRESUME CALLED")
//        getRoomInfo(lastRoomID)
//        println("lastRoomID: $lastRoomID")
//        if (this::roomInfoString.isInitialized) {
//            println("ris: $roomInfoString")
//        }
//        if (backgroundImageString != "") {
////            decodeImage(backgroundImageString)
//            println("no")
//        }
//        println("bis: $backgroundImageString")
    }

    private fun doScan(view: View) {
        // Perform Scan
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                Snackbar.make(view, barcode.rawValue ?: "0", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                val result = getRoomInfo(barcode.rawValue ?: "0")
                println(result)
            }
            .addOnCanceledListener {
                Snackbar.make(view, "Scan Canceled", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(view, "Scan error: $e", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
    }

    private fun initiateRequest(url: String, callback: (response: Response) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            val response = client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    callback(response)
                }
            })
        } catch (e: Exception) {
            println(e)
        }
    }

    fun getRoomInfo(id: String) {
        if (id != "" && id != "0") {
            lastRoomID = id
            initiateRequest("$baseURL/$ROOM_INFO_URL?id=$id", this::setRoomInfo)
            initiateRequest("$baseURL/$FLOOR_MAP_URL?id=$id", this::setFloorMap)
        } else {
            println("Invalid Room ID: $id")
        }
    }

    private fun setRoomInfo(response: Response) {
        roomInfoString = response.body()?.string() ?: "Invalid Room"
        print(roomInfoString)
    }

    private fun setFloorMap(response: Response) {
        val imgStr = response.body()?.string()
        if (imgStr == null) {
            return
        } else {
            decodeImage(imgStr)
        }
    }

    private fun decodeImage(encodedStr: String) {
        if (encodedStr != "") {

            backgroundImageString = encodedStr

            // https://stackoverflow.com/a/49628231
            // decode base64 string to image
            val imageBytes: ByteArray = Base64.decode(encodedStr, Base64.DEFAULT)
            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // required otherwise android will throw the error:
            // "Only the original thread that created a view hierarchy can touch its views."
            runOnUiThread {
                backgroundImage.setImageBitmap(decodedImage)
            }
        }
    }
}