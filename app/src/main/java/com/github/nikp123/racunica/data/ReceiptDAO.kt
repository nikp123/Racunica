package com.github.nikp123.racunica.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.github.nikp123.racunica.util.TaxCore.ReceiptExtractor
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(receipt: Receipt): Long

    @Update
    suspend fun update(receipt: Receipt)

    @Query("SELECT * FROM receipts ORDER BY time DESC")
    fun readAllData(): LiveData<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observe(id: Long): Flow<Receipt>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun read(id: Long): Receipt

    @Query("SELECT * FROM receipts WHERE country = :country AND storeID = :storeID AND code = :code AND time = :time")
    fun findByMatchingReceipt(country: ReceiptExtractor.Country, storeID: Long, code: String, time: Long): Receipt?

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeWithStore(id: Long): Flow<ReceiptStore>

    @Transaction
    @Query("SELECT * FROM receipts ORDER BY time DESC")
    fun readAllDataWithStores(): LiveData<List<ReceiptStore>>

    @Query("SELECT COUNT(*) FROM receipts WHERE storeID = :storeID;")
    fun getStoreReferenceCount(storeID: Long): Long

    @Delete
    fun delete(receipt: Receipt)

    @Update
    fun update(vararg receipt: Receipt)
}
