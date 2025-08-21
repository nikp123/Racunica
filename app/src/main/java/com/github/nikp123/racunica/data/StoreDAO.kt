package com.github.nikp123.racunica.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(store: Store): Long

    @Update
    suspend fun update(store: Store)

    @Query("SELECT * FROM stores ORDER BY id DESC")
    fun readAllData(): LiveData<List<Store>>

    @Query("SELECT * FROM stores WHERE id = :id")
    fun observe(id: Long): Flow<Store>

    @Query("SELECT * FROM stores WHERE id = :id")
    fun read(id: Long): Store

    @Query("SELECT * FROM stores WHERE code = :code")
    fun findByCode(code: String): Store?

    @Delete
    fun delete(store: Store)

    @Update
    fun update(vararg store: Store)
}
