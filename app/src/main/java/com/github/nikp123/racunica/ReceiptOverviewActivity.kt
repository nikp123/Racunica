package com.github.nikp123.racunica

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptStatus
import com.github.nikp123.racunica.data.ReceiptStore
import com.github.nikp123.racunica.data.ReceiptStoreInterface
import com.github.nikp123.racunica.databinding.ActivityReceiptOverviewBinding
import com.github.nikp123.racunica.util.CountryToIDString
import com.github.nikp123.racunica.util.TaxCore
import com.github.nikp123.racunica.util.TaxCore.SimpleReceipt
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ReceiptOverviewActivity : AppCompatActivity() {

    class InvalidReceiptID : Exception()

    private lateinit var binding: ActivityReceiptOverviewBinding
    private lateinit var storeCustomName: EditText
    private lateinit var receiptNote: TextInputEditText
    private lateinit var storeNote: TextInputEditText
    private lateinit var uiUpdaterThread: Job

    private var receiptID: Long = -1

    // We need to signal to our onDestroy method NOT
    // to update the record if we just have deleted it
    private var isBeingActivelyDestroyed = false

    @SuppressLint("DefaultLocale")
    private fun renderUI(receiptStore: ReceiptStore) {
        val (receipt, store) = receiptStore
        val view = binding.root

        // Navbar
        val receiptName = String.format("%s-%s", store.code, receipt.code)
        val title = resources.getString(R.string.receipt_overview, receiptName)
        supportActionBar?.title = title

        // Receipt details
        receiptNote = view.findViewById<TextInputEditText>(R.id.receipt_details_note)
        receiptNote.setText(receipt.note)
        view.findViewById<TextView>(R.id.receipt_details_amount).text = String.format(
            "%.02f %s",
            SimpleReceipt.getHumanAmount(receipt.amount, receipt.unit),
            receipt.unit.toString()
        )
        val date = Date(receipt.time)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        view.findViewById<TextView>(R.id.receipt_details_time).text = dateFormat.format(date)
        view.findViewById<TextView>(R.id.receipt_details_type).setText(when(receipt.type) {
            SimpleReceipt.TransactionType.SALE ->    R.string.receipt_details_type_sale
            SimpleReceipt.TransactionType.REFUND ->  R.string.receipt_details_type_refund
        })
        view.findViewById<TextView>(R.id.receipt_details_details).setText(when(receipt.status) {
            ReceiptStatus.OFFLINE -> R.string.receipt_details_details_low
            ReceiptStatus.ONLINE_FAILURE -> R.string.receipt_details_details_low
            ReceiptStatus.ONLINE -> R.string.receipt_details_details_high
        })
        view.findViewById<TextView>(R.id.receipt_details_id).text = receipt.code
        view.findViewById<TextView>(R.id.receipt_details_country).text =
            resources.getString(CountryToIDString(receipt.country))
        if(receipt.purchaserCode != null)
            view.findViewById<TextView>(R.id.receipt_details_purchaser_id).text = receipt.purchaserCode

        // Store
        storeCustomName = view.findViewById<EditText>(R.id.receipt_store_custom_name)
        if (store.usersName != null) storeCustomName.setText(store.usersName)
        if(store.municipality != null)
            view.findViewById<TextView>(R.id.receipt_details_store_municipality).text = store.municipality
        if(store.city != null)
            view.findViewById<TextView>(R.id.receipt_details_store_city).text = store.city
        if(store.address != null)
            view.findViewById<TextView>(R.id.receipt_details_store_address).text = store.address
        if(store.name != null)
            view.findViewById<TextView>(R.id.receipt_details_store_registered_name).text = store.name
        storeNote = view.findViewById<TextInputEditText>(R.id.receipt_details_store_note)
        storeNote.setText(store.note)
        view.findViewById<TextView>(R.id.receipt_details_store_id).text = store.code

        // Options
        view.findViewById<Button>(R.id.receipt_options_open_link).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, receipt.url.toUri())
            startActivity(browserIntent)
        }
        val fetchOrOpenReceipt = view.findViewById<Button>(R.id.receipt_options_obtain_or_see_full_receipt)
        when (receipt.status) {
            ReceiptStatus.OFFLINE, ReceiptStatus.ONLINE_FAILURE -> {
                fetchOrOpenReceipt.setOnClickListener {
                    val extractor = TaxCore.ReceiptExtractor(this, uri = URI.create(receipt.url))
                    val simple = extractor.getSimple()
                    val activity = this

                    // Do networking on a separate thread to prevent UI lockups
                    lifecycleScope.launch {
                        val res = activity.resources

                        // Access the database globally
                        val error = withContext(Dispatchers.IO) {
                            val (pair, error) = simple.fetchReceiptAndStore(activity, receipt.id)

                            val db = AppDatabase.getDatabase(applicationContext)
                            val receiptStoreInterface = ReceiptStoreInterface(db)
                            receiptStoreInterface.insertOrUpdate(pair)

                            error
                        }

                        val errorMessage = if(error is SimpleReceipt.FetchInvalidCertificate) {
                            res.getString(R.string.ssl_exception, error.message)
                        } else if(error != null) {
                            res.getString(R.string.unknown_failure, error.message)
                        } else null

                        val message = when(error == null) {
                            true  -> res.getString(R.string.receipt_options_try_fetching_success)
                            false -> res.getString(R.string.receipt_fetch_failure,
                                errorMessage)
                        }

                        Toast.makeText(
                            applicationContext,
                            message,
                            Toast.LENGTH_LONG
                        ).show()

                        extractor.close()
                    }
                }
            }
            ReceiptStatus.ONLINE -> {
                fetchOrOpenReceipt.setText(R.string.receipt_options_full_receipt)
                fetchOrOpenReceipt.setOnClickListener {
                    // Open receipt activity
                    runOnUiThread {
                        val context = this@ReceiptOverviewActivity
                        val intent = Intent(context, ReceiptFullActivity::class.java)
                        intent.putExtra("RECEIPT_ID", receipt.id)
                        context.startActivity(intent)
                    }
                }
            }
        }

        val navigateToBusiness = view.findViewById<Button>(R.id.receipt_options_navigate_to_business)
        if(store.address != null) {
            navigateToBusiness.isEnabled = true
            navigateToBusiness.setOnClickListener {
                runOnUiThread {
                    var address = store.address
                    if(store.city != null)
                        address += ", " + store.city
                    if(store.municipality != null)
                        address += ", " + store.municipality

                    address += ", " + resources.getString(CountryToIDString(store.country))

                    val locationString = "geo:0,0?q=" + URLEncoder.encode(address, "UTF-8")

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = locationString.toUri()
                    }

                    // Do NOT set a package — system will show all apps that can handle navigation
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No navigation app found", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val deleteButton = view.findViewById<Button>(R.id.receipt_options_delete)
        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                // Stop the UI from being updated because clearing the record
                // while the thread is listening for it WILL cause a crash
                uiUpdaterThread.cancel()
                isBeingActivelyDestroyed = true

                val db = AppDatabase.getDatabase(applicationContext)
                val receiptDAO = db.receiptDAO()
                val storeDAO = db.storeDAO()

                // Do updates on a separate thread to prevent UI lockups
                withContext(Dispatchers.IO) {
                    receiptDAO.delete(receipt)
                    if(receiptDAO.getStoreReferenceCount(store.id) == 0L)
                        storeDAO.delete(store)
                }

                // The bill was invalidated, therefore we must exit
                finish()
            }
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReceiptOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the RECEIPT_ID from the Intent
        receiptID = intent.getLongExtra("RECEIPT_ID", -1L) // Default value is -1 if not found
        if(receiptID == -1L)
            throw InvalidReceiptID()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        uiUpdaterThread = lifecycleScope.launch {
            // Access the database globally
            val db = AppDatabase.getDatabase(this@ReceiptOverviewActivity)
            withContext(Dispatchers.IO) {
                db.receiptDAO().observeWithStore(receiptID).collectLatest { receiptStore ->
                    runOnUiThread {
                        renderUI(receiptStore)
                    }
                }
            }
        }
    }

    private fun saveTextFields() {
        // Save the text after it has changed
        val receiptText = receiptNote.text.toString()
        val storeText = storeNote.text.toString()
        val storeNameText = storeCustomName.text.toString()

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val receiptDAO = db.receiptDAO()
            val storeDAO = db.storeDAO()

            // Do updates on a separate thread to prevent UI lockups
            withContext(Dispatchers.IO) {
                val receipt = receiptDAO.read(receiptID)
                val store = storeDAO.read(receipt.storeID)
                receiptDAO.update(receipt.copy(
                    note = if (receiptText.isEmpty()) null else receiptText
                ))
                storeDAO.update(store.copy(
                    usersName = if(storeNameText.isEmpty()) null else storeNameText,
                    note = if (storeText.isEmpty()) null else storeText
                ))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!isBeingActivelyDestroyed)
            saveTextFields()
    }

    override fun onPause() {
        super.onPause()

        // yes the fucking pause method gets called upon finish()
        // i do not understand android at all
        if(!isBeingActivelyDestroyed)
            saveTextFields()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}