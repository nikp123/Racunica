package com.github.nikp123.racunica.data

import androidx.room.Transaction

class ReceiptStoreInterface(
    private val receiptDAO: ReceiptDAO,
    private val storeDAO:   StoreDAO
) {
    constructor(db: AppDatabase) :this(
        db.receiptDAO(),
        db.storeDAO()
    )

    @Transaction
    suspend fun insertOrUpdate(pair: ReceiptStore): Long {
        val (newReceipt, newStore) = pair
        // The logic is as follows:

        // When we insert a new Receipt, it always comes with the corresponding Store
        // However, when we insert a Receipt, the associated Store may not be in the DB yet
        // Furthermore, the Store that's associated with it may be just a placeholder
        // which is there to just provide association but without introducing any details

        // This means that we need to keep the Receipt -> Store association valid because
        // if we don't the app WILL break.

        // This also means that whenever possible we should update "low detail" Stores to
        // their higher detail counterparts

        // The strategy is as follows
        // We should ONLY be using this insert statement to add new Receipts
        // And we should ONLY be using this insert statement to get an "updated/correct" Store
        // The Store-only update methods should be only used for user customization, because
        // every Store update should come from the Receipt (unless a different Tax/Billing
        // system changes this)

        // First check if the Store exists and if it's the "updated" one
        val existingStore = storeDAO.findByCode(newStore.code)

        // This MUST be correctly set in the Receipt otherwise we got a nasty bug
        val storeID = if (existingStore == null) {
            storeDAO.insert(newStore)
        } else {
            // this branch does not return value to storeID, look below
            when(existingStore.status) {
                StoreStatus.OFFLINE, StoreStatus.ONLINE_FAILURE -> storeDAO.update(newStore.copy(
                    id           = existingStore.id,
                    // usersName should NEVER be set by business logic
                    usersName    = existingStore.usersName,
                    note         = existingStore.note,
                    // We should allow the user to keep their own location data if they so choose
                    municipality = existingStore.municipality ?: newStore.municipality,
                    city         = existingStore.city         ?: newStore.city,
                    address      = existingStore.address      ?: newStore.address,
                ))
                StoreStatus.ONLINE -> {}
            }
            existingStore.id
        }

        // This last step ensures we don't nuke a "updated" bill by accident
        val existingReceipt = receiptDAO.findByMatchingReceipt(
            country = newReceipt.country,
            storeID = storeID,
            code = newReceipt.code,
            time = newReceipt.time
        )

        return if(existingReceipt == null) {
            receiptDAO.insert(newReceipt.copy(storeID = storeID))
        } else if(existingReceipt.status == ReceiptStatus.OFFLINE ||
                existingReceipt.status == ReceiptStatus.ONLINE_FAILURE) {
            receiptDAO.update(newReceipt.copy(
                id = existingReceipt.id,
                note = existingReceipt.note,
                storeID = storeID
            ))
            existingReceipt.id
        } else -1L
    }
}