package com.github.nikp123.racunica

import ReceiptStoreViewModel
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.github.nikp123.racunica.data.ReceiptAdapter
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptStoreInterface

import com.github.nikp123.racunica.util.TaxCore
import com.github.nikp123.racunica.util.TaxCore.SimpleReceipt

import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig
import io.github.g00fy2.quickie.QRResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

class ReceiptsFragment : Fragment() {
    private lateinit var receiptStoreAdapter: ReceiptAdapter
    private lateinit var receiptStoreViewModel: ReceiptStoreViewModel

    class EntryAlreadyExists() : Exception()

    private val scanQR = registerForActivityResult(ScanCustomCode()) {
        result -> processScan(requireActivity(), result)
    }

    private fun processScan(activity: Activity, result: QRResult) {
        val text = when (result) {
            is QRResult.QRSuccess -> {
                result.content.rawValue
                // decoding with default UTF-8 charset when rawValue is null will not result in meaningful output, demo purpose
                    ?: result.content.rawBytes?.let { String(it) }.orEmpty()
            }
            QRResult.QRUserCanceled -> return
            QRResult.QRMissingPermission -> return
            is QRResult.QRError -> return
            // "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
        }

        val extractor = try {
            TaxCore.ReceiptExtractor(activity, URI.create(text))
        } catch(e: Exception) {
            Toast.makeText(activity, "QR code is not a valid receipt", Toast.LENGTH_SHORT).show()
            Log.d("ReceiptsFragment", String.format("URI was: %s", text))
            Log.d("ReceiptsFragment", e.message.orEmpty())
            return
        }

        val simple = extractor.getSimple()

        lifecycleScope.launch {
            val res = activity.resources

            // Access the database globally
            val db = AppDatabase.getDatabase(activity)

            var error = withContext(Dispatchers.IO) {
                val receiptStoreInterface = ReceiptStoreInterface(db.receiptDAO(), db.storeDAO())
                var (pair, error) = simple.fetchReceiptAndStore(activity, null)

                val insertResult = receiptStoreInterface.insertOrUpdate(pair)
                if(insertResult == -1L) {
                    error = EntryAlreadyExists()
                }

                error
            }

            if (error == null) {
                Toast.makeText(
                    activity,
                    resources.getString(
                        R.string.scanner_taxcore_receipt_added,
                        simple.requestedBy,
                        simple.signedBy,
                        simple.totalTransactions
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } else if(error is EntryAlreadyExists) {
                Toast.makeText(activity,
                    R.string.receipt_exists,
                    Toast.LENGTH_SHORT).show()
            } else {
                // Replace with error dictionary once you get to it
                val errorMessage = if(error is SimpleReceipt.FetchInvalidCertificate) {
                    res.getString(R.string.ssl_exception, error.message)
                } else {
                    res.getString(R.string.unknown_failure, error.message)
                }

                Toast.makeText(activity,
                    res.getString(
                        R.string.receipt_fetch_failure, errorMessage
                    ),
                    Toast.LENGTH_LONG).show()
            }

            extractor.close()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_receipts, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addButton = view.findViewById<FloatingActionButton>(R.id.new_scan)
        addButton.setOnClickListener { scanQR.launch (ScannerConfig.build {
            setBarcodeFormats(listOf(BarcodeFormat.FORMAT_QR_CODE)) // set interested barcode formats
            setHapticSuccessFeedback(true) // enable (default) or disable haptic feedback when a barcode was detected
            setShowTorchToggle(true) // show or hide (default) torch/flashlight toggle button
            setShowCloseButton(true) // show or hide (default) close button
            setHorizontalFrameRatio(1f) // set the horizontal overlay ratio (default is 1 / square frame)
            setUseFrontCamera(false) // use the front camera
        })}

        // Access the database globally
        receiptStoreAdapter = ReceiptAdapter()

        val recyclerView: RecyclerView = view.findViewById(R.id.recycleView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = receiptStoreAdapter

        receiptStoreViewModel = ViewModelProvider(this)[ReceiptStoreViewModel::class.java]

        receiptStoreViewModel.receiptStores.observe(viewLifecycleOwner, Observer { receiptStore ->
            receiptStoreAdapter.updateReceiptStore(receiptStore)
        })
    }
}