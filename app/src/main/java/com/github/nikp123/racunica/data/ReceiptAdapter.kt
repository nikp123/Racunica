package com.github.nikp123.racunica.data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.github.nikp123.racunica.R
import com.github.nikp123.racunica.util.TaxCore.SimpleReceipt
import com.github.nikp123.racunica.util.unixTimeToRelativeTime
import com.github.nikp123.racunica.ReceiptOverviewActivity

class ReceiptAdapter(private var receiptStores: List<ReceiptStore> = emptyList()) :
    RecyclerView.Adapter<ReceiptAdapter.ListViewHolder>() {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        context = parent.context

        val itemView = LayoutInflater.from(context).inflate(R.layout.element_receipt, parent, false)

        return ListViewHolder(itemView)
    }

    // This method returns the total
    // number of items in the data set
    override fun getItemCount(): Int {
        return receiptStores.size
    }

    fun updateReceiptStore(newReceiptStores: List<ReceiptStore>) {
        receiptStores = newReceiptStores
        notifyDataSetChanged()
    }

    // This method binds the data to the ViewHolder object
    // for each item in the RecyclerView
    @SuppressLint("DefaultLocale") // We want the OS to localize the numbers
    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val (receipt, store) = receiptStores[position]

        // TODO: Offer other stores of value
        holder.unit.text = receipt.unit.toString() // Assuming we are only dealing with Serbia
        holder.time.text =
            unixTimeToRelativeTime(receipt.time, context)
        holder.amount.text = String.format("%.2f", SimpleReceipt.getHumanAmount(receipt.amount,
            receipt.unit))
        holder.name.text        = store.usersName ?: (store.name ?: store.code)
        holder.receiptCode.text = holder.receiptCode.resources.getString(R.string.receipt_code_short) + receipt.code
        holder.type.setBackgroundResource(when(receipt.type) {
            SimpleReceipt.TransactionType.SALE   -> R.drawable.ic_receipt_foreground
            SimpleReceipt.TransactionType.REFUND -> R.drawable.ic_money_foreground
        })
        holder.receiptNote.visibility = when(receipt.note != null) {
            true -> View.VISIBLE; false -> View.GONE
        }
        if(receipt.note != null) holder.receiptNote.text = receipt.note.split("\n")[0]
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time        : TextView  = itemView.findViewById(R.id.tvTime)
        val amount      : TextView  = itemView.findViewById(R.id.tvAmount)
        val receiptCode : TextView  = itemView.findViewById(R.id.tvReceiptCode)
        val receiptNote : TextView  = itemView.findViewById(R.id.tvReceiptNote)
        val unit        : TextView  = itemView.findViewById(R.id.tvUnit)
        val name        : TextView  = itemView.findViewById(R.id.tvStoreName)
        val type        : ImageView = itemView.findViewById(R.id.transactionType)

        init {
            itemView.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    var (receipt, store) = receiptStores[position]
                    val context: Context = itemView.context
                    val intent = Intent(context, ReceiptOverviewActivity::class.java)
                    intent.putExtra("RECEIPT_ID", receipt.id)
                    context.startActivity(intent)
                }
            }
        }
    }
}