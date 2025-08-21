package com.github.nikp123.racunica.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.github.nikp123.racunica.util.TaxCore

enum class ReceiptStatus {
    OFFLINE, ONLINE_FAILURE, ONLINE
}

@Entity(
    tableName = "receipts",
    indices = [Index(value = ["country", "storeID", "code", "time"], unique = true)]
)
data class Receipt (
    // Sequential ID of the receipt for easier tracking
    @PrimaryKey(autoGenerate = true)
    val id:         Long = 0,

    // Raw scanned value
    val amount:     Long,

    // Type of currency
    val unit:       TaxCore.SimpleReceipt.MonetaryUnit,

    // Country of origin
    val country:    TaxCore.ReceiptExtractor.Country,

    // UNIX timestamp
    val time:       Long,

    // Code legally identifying this store
    val storeID:    Long,

    // Code identifying the receipt
    val code:       String,

    // Purchaser identity
    var purchaserCode: String?,

    // Track the status of the receipt
    val status:     ReceiptStatus,

    // Track the type of the receipt
    val type:       TaxCore.SimpleReceipt.TransactionType,

    // Text of the entire receipt
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val text:       String?,

    // Government issued URL of this receipt
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val url:        String,

    // User specified note
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val note:       String?,
)
