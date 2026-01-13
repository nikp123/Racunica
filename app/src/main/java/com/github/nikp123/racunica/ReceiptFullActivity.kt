package com.github.nikp123.racunica

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.WindowInsetsController
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptStore
import com.github.nikp123.racunica.databinding.ActivityReceiptFullBinding
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiptFullActivity : AppCompatActivity() {

    class InvalidReceiptID : Exception()

    private lateinit var binding: ActivityReceiptFullBinding

    private var receiptID: Long = -1

    private var statusBarIsLight: Boolean = false

    private fun Context.isUsingLightTheme(): Boolean {
        // The UI mode bits are stored in the configuration’s uiMode field.
        // Mask out the night‑mode bits and compare with UI_MODE_NIGHT_NO.
        return (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO
    }

    private fun setStatusBarLightIcons(activity: Activity, lightIcons: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appearance = if (lightIcons) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            window.insetsController?.setSystemBarsAppearance(
                appearance,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,

            )
        } else {
            val decor = activity.window.decorView
            var flags = decor.systemUiVisibility
            flags = if (lightIcons) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()   // dark background → light icons
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR          // light background → dark icons
            }
            decor.systemUiVisibility = flags
        }
    }

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

        val appBar = view.findViewById<AppBarLayout>(R.id.appbar)
        appBar.addOnOffsetChangedListener { _, offset ->
            // total scroll range is negative; offset goes from 0 (expanded) to -total (collapsed)
            val total = appBar.totalScrollRange
            val collapseFraction = -offset / total.toFloat()   // 0..1

            // Switch when > 0.5 (or when fully collapsed)
            val shouldUseLightIcons = collapseFraction > 0.5f

            if (shouldUseLightIcons != statusBarIsLight && !view.context.isUsingLightTheme()) {
                setStatusBarLightIcons(
                    this,
                    !shouldUseLightIcons
                ) // invert because flag = dark icons
                statusBarIsLight = shouldUseLightIcons
            }
        }

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