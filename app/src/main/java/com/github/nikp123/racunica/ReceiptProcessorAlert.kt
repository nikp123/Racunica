package com.github.nikp123.racunica

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReceiptProcessorAlert {
    private var activity: Activity
    private var dialog: AlertDialog
    private var layout: LinearLayout
    private var progress: ProgressBar
    private var progressLabel: TextView
    private var closeButton: Button


    private var failedItems: ArrayList<Pair<String, String>> = ArrayList()
    private var failedItemLabel: TextView
    private var failedItemView: RecyclerView

    private var invalidItems: ArrayList<Pair<String, String>> = ArrayList()
    private var invalidItemLabel: TextView
    private var invalidItemView: RecyclerView


    private var length: Int = 0

    private var view: View


    class ProcessedItemAdapter(
        private val entries: List<Pair<String, String>>
    ) : RecyclerView.Adapter<ProcessedItemAdapter.EntryViewHolder>() {
        private lateinit var view: View

        class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemName: TextView = itemView.findViewById(R.id.itemName)
            val itemReason: TextView = itemView.findViewById(R.id.itemReason)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            view = LayoutInflater.from(parent.context).inflate(
                R.layout.element_receipt_processor_item, parent, false
            )
            return EntryViewHolder(view)
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val clipboard = view.context.getSystemService( Context.CLIPBOARD_SERVICE) as ClipboardManager

            holder.itemName.text = entries[position].first
            holder.itemName.setOnLongClickListener {
                val clip = ClipData.newPlainText("label", entries[position].first)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(view.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                true // return true to indicate that the long press was handled
            }
            holder.itemReason.text = entries[position].second
            holder.itemReason.setOnLongClickListener {
                val clip = ClipData.newPlainText("label", entries[position].second)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(view.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                true // return true to indicate that the long press was handled
            }
            holder.itemReason.visibility = VISIBLE
        }

        override fun getItemCount(): Int = entries.size
    }

    @SuppressLint("InflateParams")
    constructor(activity: Activity) {
        this.activity = activity

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val inflater: LayoutInflater = activity.layoutInflater

        view = inflater.inflate(R.layout.alert_receipt_processor, null, false)
        layout = view.findViewById<LinearLayout>(R.id.receipt_extractor_layout)

        builder.setView(view)
        builder.setCancelable(false)

        dialog = builder.create()
        dialog.show()

        progress = view.findViewById<ProgressBar>(R.id.receipt_processor_progress)
        progressLabel = view.findViewById<TextView>(R.id.receipt_processor_label)
        failedItemLabel = view.findViewById<TextView>(R.id.failed_items_label)
        failedItemView = view.findViewById<RecyclerView>(R.id.failed_items_view)
        failedItemView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        failedItemView.isVerticalScrollBarEnabled = true
        invalidItemLabel = view.findViewById<TextView>(R.id.invalid_items_label)
        invalidItemView = view.findViewById<RecyclerView>(R.id.invalid_items_view)
        invalidItemView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        invalidItemView.isVerticalScrollBarEnabled = true

        closeButton = view.findViewById<Button>(R.id.close)
        closeButton.setOnClickListener {
            dialog.hide()
        }
    }

    fun expandLayout() {
        val params = view.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
    }

    fun addFailedItem(item: Pair<String, String>) {
        expandLayout()
        failedItemLabel.visibility = VISIBLE

        failedItems.add(item)
        failedItemLabel.setOnClickListener {
            val (visibility, drawable) = when(failedItemView.visibility) {
                GONE -> Pair(VISIBLE, R.drawable.ic_expand_less)
                VISIBLE -> Pair(GONE, R.drawable.ic_expand_more)
                else -> Pair(VISIBLE, R.drawable.ic_expand_less)
            }
            failedItemView.visibility = visibility
            failedItemLabel.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, drawable, 0
            )
        }

        failedItemLabel.text = view.resources.getString(
            R.string.failed_items, failedItems.size
        )

        failedItemView.layoutManager = LinearLayoutManager(view.context)
        failedItemView.adapter = ProcessedItemAdapter(failedItems)
    }

    fun addInvalidItem(item: Pair<String, String>) {
        expandLayout()
        invalidItemLabel.visibility = VISIBLE

        invalidItems.add(item)
        invalidItemLabel.setOnClickListener {
            val (visibility, drawable) = when(invalidItemView.visibility) {
                GONE -> Pair(VISIBLE, R.drawable.ic_expand_less)
                VISIBLE -> Pair(GONE, R.drawable.ic_expand_more)
                else -> Pair(VISIBLE, R.drawable.ic_expand_less)
            }
            invalidItemView.visibility = visibility
            invalidItemLabel.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, drawable, 0
            )
        }

        invalidItemLabel.text = view.resources.getString(
            R.string.invalid_items, invalidItems.size
        )

        invalidItemView.layoutManager = LinearLayoutManager(view.context)
        invalidItemView.adapter = ProcessedItemAdapter(invalidItems)
    }


    fun updateProgress(value: Int, length: Int = 0) {
        progress.isIndeterminate = false
        progress.progress = value
        if(length != 0) {
            this.length = length
            progress.max = length
        }

        // The operation has finished and the user can close the dialog now
        if(value == this.length) {
            closeButton.visibility = VISIBLE
            progress.visibility = GONE
            progressLabel.text = view.resources.getString(R.string.finished)
        }
    }

    fun close() {
        dialog.hide()
    }
}