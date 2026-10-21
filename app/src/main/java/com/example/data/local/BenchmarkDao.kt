package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(benchmark: BenchmarkEntity)

    @Update
    suspend fun update(benchmark: BenchmarkEntity)

    @Delete
    suspend fun delete(benchmark: BenchmarkEntity)

    @Query("DELETE FROM benchmarks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM benchmarks ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BenchmarkEntity>>

    @Query("SELECT * FROM benchmarks WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<BenchmarkEntity>>

    @Query("SELECT * FROM benchmarks WHERE id = :id")
    suspend fun getById(id: String): BenchmarkEntity?

    @Query("SELECT * FROM benchmarks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<BenchmarkEntity>

    @Query("UPDATE benchmarks SET syncStatus = :status, cloudDocId = :cloudId WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, cloudId: String?)

    @Query("DELETE FROM benchmarks")
    suspend fun clearAll()
}
