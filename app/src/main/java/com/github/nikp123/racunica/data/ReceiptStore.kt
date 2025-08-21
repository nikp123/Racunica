package com.github.nikp123.racunica.data

import androidx.room.Embedded
import androidx.room.Relation

data class ReceiptStore (
    @Embedded val receipt: Receipt,
    @Relation(
        parentColumn = "storeID",
        entityColumn = "id"
    )
    val store: Store
)