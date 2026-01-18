package com.github.nikp123.racunica

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptStoreInterface
import com.github.nikp123.racunica.util.TaxCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

class SettingsFragment : Fragment() {
    private var filePickerLauncher: ActivityResultLauncher<Intent?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filePickerLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult()
        ) { result: ActivityResult? ->
            if (result!!.resultCode == RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                if (uri != null) processRestore(uri)
            }
        }
    }

    private fun readPlainTextLineByLine(uri: Uri): List<String> {
        val list: ArrayList<String> = ArrayList()

        try {
            requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader.forEachLine { line ->
                    list.add(line)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error reading file", Toast.LENGTH_SHORT).show()
        }

        return list
    }

    private fun processRestore(uri: Uri) {
        val alert = ReceiptProcessorAlert(requireActivity())

        val mimetype = requireContext().contentResolver.getType(uri)

        // Unused, but can be indicated to the user that something is wrong with their backup
        val invalidEntries: ArrayList<ByteArray> = ArrayList()

        val list: List<Pair<TaxCore.ReceiptExtractor, ByteArray>> = when(mimetype) {
            "text/plain" -> readPlainTextLineByLine(uri)
                .mapNotNull {
                    try {
                        Pair(
                            TaxCore.ReceiptExtractor(it.toByteArray(Charsets.UTF_8)),
                            it.toByteArray(Charsets.UTF_8)
                        )
                    } catch(e: Exception) {
                        invalidEntries.add(it.toByteArray(Charsets.UTF_8))
                        alert.addInvalidItem(Pair(it, e.toString()))
                        null
                    }
                }
            else -> return
        }

        val validEntries = list.size
        val progress = AtomicInteger(0)
        alert.updateProgress(progress.toInt(), validEntries)

        CoroutineScope(Dispatchers.Main + Job()).launch {
            val receiptStoreInterface = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                ReceiptStoreInterface(db)
            }

            // Receipt identifier (be it URL or code) and the actual error
            // Optionally we want to display this to the user and let them decide
            // what to do further
            val errors: ArrayList<Pair<String, String>> = ArrayList()

            list.map { (it, origin) ->
                val (receiptCode, error) = it.processAndInsertReceipt(
                    requireActivity(),
                    receiptStoreInterface
                )

                val humanError = TaxCore.ReceiptExtractor.receiptStatusToMessage(
                    requireActivity(),
                    receiptCode,
                    error,
                    false
                    )

                if(error != null) {
                    val item = Pair(
                        String(origin, StandardCharsets.UTF_8),
                        humanError
                    )
                    alert.addFailedItem(item)
                    errors.add(item)
                }

                withContext(Dispatchers.Main) {
                    alert.updateProgress(progress.incrementAndGet())
                }
            }

            Toast.makeText(context,
                requireContext().resources.getString(R.string.import_report,
                    validEntries - errors.size,
                    errors.size,
                    invalidEntries.size),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val processExport = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.also { uri ->
                // Write your data to the Uri
                view?.context?.contentResolver?.openOutputStream(uri)?.use { outputStream ->
                    // Do it the dumb way
                    runBlocking {
                        val dao = AppDatabase.getDatabase(requireContext()).receiptDAO()

                        withContext(Dispatchers.IO) {
                            val list = dao.readAllDataBlocking()

                            val output = list.joinToString("\n") { it.url }.toByteArray()
                            outputStream.write(output)
                        }
                    }
                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val importButton = view.findViewById<Button>(R.id.settings_import)
        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "text/csv"
            ))
            filePickerLauncher?.launch(intent)
        }

        val exportButton = view.findViewById<Button>(R.id.settings_export)
        exportButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"                     // MIME type of the file you want to save
                putExtra(Intent.EXTRA_TITLE, "backup.txt") // suggested filename
            }
            processExport.launch(intent)
        }
    }
}