package com.github.nikp123.racunica

import com.github.nikp123.racunica.data.ReceiptStoreViewModel
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptAdapter
import com.github.nikp123.racunica.data.ReceiptStoreInterface
import com.github.nikp123.racunica.util.TaxCore
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiptsFragment : Fragment() {
    private lateinit var receiptStoreAdapter: ReceiptAdapter
    private lateinit var receiptStoreViewModel: ReceiptStoreViewModel

    private val scanQR = registerForActivityResult(ScanCustomCode()) {
        result -> processScan(requireActivity(), result)
    }

    private fun processScan(activity: Activity, result: QRResult) {
        val data = when (result) {
            is QRResult.QRSuccess -> {
                result.content.rawBytes ?: return
            }
            QRResult.QRUserCanceled -> return
            QRResult.QRMissingPermission -> return
            is QRResult.QRError -> return
            // "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
        }

        val alert = ReceiptProcessorAlert(activity)

        val extractor = try {
            TaxCore.ReceiptExtractor(data)
        } catch(e: Exception) {
            Toast.makeText(activity, "QR code is not a valid receipt", Toast.LENGTH_SHORT).show()
            Log.d("ReceiptsFragment", "QR decode failed due to: " + e.message.orEmpty())
            alert.close()
            return
        }

        // Do networking on a separate thread to prevent UI lockups
        lifecycleScope.launch {
            val receiptStoreInterface = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                ReceiptStoreInterface(db)
            }

            val (receiptCode, error) = extractor.processAndInsertReceipt(
                activity,
                receiptStoreInterface,
            )

            val humanError = TaxCore.ReceiptExtractor.receiptStatusToMessage(
                activity,
                receiptCode,
                error,
                false
            )

            Toast.makeText(
                requireContext(),
                humanError,
                if (error != null) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()

            alert.close()
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