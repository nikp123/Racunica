package com.github.nikp123.racunica

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptStore
import com.github.nikp123.racunica.databinding.ActivityReceiptFullBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiptFullActivity : AppCompatActivity() {

    class InvalidReceiptID : Exception()

    private lateinit var binding: ActivityReceiptFullBinding

    private var receiptID: Long = -1

    private fun renderUI(pair: ReceiptStore) {
        val view = binding.root
        val (receipt, store) = pair

        // Navbar
        val receiptName = String.format("%s-%s", store.code, receipt.code)
        val title = resources.getString(R.string.receipt_overview, receiptName)
        supportActionBar?.title = title

        // Receipt details
        val webView = view.findViewById<WebView>(R.id.receipt_full_content)
        val siteUnencoded = receipt.text ?: "Text content is missing, this is a bug!"

        // We need to do this conversion bullshit because
        // for some reason if you parse a raw UTF-8 '#' (Pound sign)
        // the WebView will treat it as some sort of stop character
        // and prevent further content from being loaded in
        val siteEncoded = Base64.encode(siteUnencoded.toByteArray(), Base64.NO_PADDING)
        webView.loadData(String(siteEncoded), "text/html", "base64")
    }

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReceiptFullBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the RECEIPT_ID from the Intent
        receiptID = intent.getLongExtra("RECEIPT_ID", -1L) // Default value is -1 if not found
        if(receiptID == -1L)
            throw InvalidReceiptID()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch {
            // Access the database globally
            val db = AppDatabase.getDatabase(this@ReceiptFullActivity)
            withContext(Dispatchers.IO) {
                val dao = db.receiptDAO()
                dao.observeWithStore(receiptID).collectLatest { receiptStore ->
                    runOnUiThread {
                        renderUI(receiptStore)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}