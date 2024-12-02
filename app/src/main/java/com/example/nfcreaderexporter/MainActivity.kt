package com.example.nfcreaderexporter;

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class MainActivity : Activity(), NfcAdapter.ReaderCallback {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var exportButton: Button
    private lateinit var builder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_SHORT).show()
            finish()
        }

        sharedPreferences = getSharedPreferences("nfc_data", Context.MODE_PRIVATE)

        exportButton = findViewById(R.id.export_button)
        exportButton.setOnClickListener {
            exportData()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val id = ByteArrayToString(tag.id)

        runOnUiThread {
            try {
                builder = AlertDialog.Builder(this);
                builder.setTitle("NFC Tag Detected");
                builder.setMessage("Do you want to save the tag ID: " + id + "?");
                builder.setPositiveButton("Save") { _, _ ->
                    saveData(id)
                }
                builder.setNegativeButton("Ignore", null);
                builder.show();
            } catch (e: Exception) {
                Log.e("Dialog", "Error creating or showing AlertDialog: ${e.message}")
            }
        }
    }

    private fun saveData(id: String) {
        val currentDate = SimpleDateFormat("yyyyMMdd").format(Date())
        val editor = sharedPreferences.edit()
        val previos = sharedPreferences.getString("nfc_data_$currentDate", "")
        editor.putString("nfc_data_$currentDate", previos + "\\n" +  id)
        editor.apply()
    }

    private fun exportData() {
        val currentDate = SimpleDateFormat("yyyyMMdd").format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Subtract one day
        val previousDate = SimpleDateFormat("yyyyMMdd").format(calendar.time)
        val currentData = sharedPreferences.getString("nfc_data_$currentDate", "")
        val previousData = sharedPreferences.getString("nfc_data_$previousDate", "")

        val dataToExport = "Current Data: $currentData\nPrevious Data: $previousData"

        val filename = "nfc_data_${SimpleDateFormat("yyyyMMdd").format(Date())}.txt"
        val filesDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(filesDir, filename)
        try {
            val fos = FileOutputStream(file)
            fos.write(dataToExport.toByteArray())
            fos.close()
            Toast.makeText(this, "Data exported successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ByteArrayToString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (byte in bytes) {
            sb.append(String.format("%02X ", byte))
        }
        return sb.toString().trim()
    }
}