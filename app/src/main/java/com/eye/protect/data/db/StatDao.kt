package com.eye.protect.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatDao {

    @Insert
    suspend fun insert(record: LockRecord)

    @Query("""
        SELECT COUNT(*) FROM lock_records
        WHERE lockTime >= :dayStart AND lockTime < :dayEnd
    """)
    fun getLockCountToday(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(workDuration), 0) FROM lock_records
        WHERE lockTime >= :dayStart AND lockTime < :dayEnd
    """)
    fun getTotalWorkMinutesToday(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("SELECT * FROM lock_records ORDER BY lockTime DESC LIMIT 20")
    fun getRecentRecords(): Flow<List<LockRecord>>
}
