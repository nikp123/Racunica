package com.github.nikp123.racunica.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.nikp123.racunica.util.TaxCore

enum class StoreStatus {
    OFFLINE, ONLINE_FAILURE, ONLINE
}

@Entity(
    tableName = "stores",
    indices = [Index(value = ["code", "country"], unique = true)]
)
data class Store (
    // Sequential ID of the store for easier tracking
    @PrimaryKey(autoGenerate = true)
    val id:         Long = 0,

    // Internal tracking status of the store
    val status:     StoreStatus,

    // Raw scanned value
    val code:       String,

    // API-backed name
    val name:       String?,

    // User-defined name
    val usersName: String?,

    // Location
    val country:        TaxCore.ReceiptExtractor.Country,
    val municipality:   String?,
    val city:           String?,
    val address:        String?,

    // User specified note
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val note:           String?
)