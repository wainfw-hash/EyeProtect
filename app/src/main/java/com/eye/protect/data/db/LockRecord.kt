package com.eye.protect.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lock_records")
data class LockRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val lockTime: Long,
    val workDuration: Int,
    val mode: String
)
