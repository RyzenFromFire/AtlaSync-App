package com.atlasync.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var pref: SharedPreferences
    private lateinit var ipPref: String
    private lateinit var baseURL: String
    private val client = OkHttpClient()

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

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        ipPref = pref.getString(getString(R.string.ip_pref_key), getString(R.string.default_ip)) ?: getString(R.string.default_ip)
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        baseURL = "http://$ipPref:5000"
//        Toast.makeText(this, ipPref, Toast.LENGTH_SHORT).show()
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

    private fun getRoomInfo(id: String) {
        val request = Request.Builder()
            .url("$baseURL/room?id=$id")
            .build()
        try {
            val response = client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    val respString = response.body()?.string() ?: "Invalid Room"
                    updateLocation(respString)
                }
            })
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun updateLocation(rawLocation: String) {
        println(rawLocation)
    }
}