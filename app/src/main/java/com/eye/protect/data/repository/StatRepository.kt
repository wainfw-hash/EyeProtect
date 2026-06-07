package com.eye.protect.data.repository

import android.content.Context
import com.eye.protect.data.db.AppDatabase
import com.eye.protect.data.db.LockRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class StatRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).statDao()

    private fun getDayStartEnd(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000
        return dayStart to dayEnd
    }

    fun getLockCountToday(): Flow<Int> {
        val (start, end) = getDayStartEnd()
        return dao.getLockCountToday(start, end)
    }

    fun getTotalWorkMinutesToday(): Flow<Int> {
        val (start, end) = getDayStartEnd()
        return dao.getTotalWorkMinutesToday(start, end)
    }

    suspend fun recordLock(workDuration: Int, mode: String) {
        val now = System.currentTimeMillis()
        dao.insert(
            LockRecord(
                startTime = now,
                lockTime = now,
                workDuration = workDuration,
                mode = mode
            )
        )
    }
}
