package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entities.SharedAppStateBackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedAppStateDao {

    @Query("SELECT * FROM shared_app_state_backup WHERE sessionId = :sessionId LIMIT 1")
    fun observeBackup(sessionId: String): Flow<SharedAppStateBackupEntity?>

    @Query("SELECT * FROM shared_app_state_backup WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBackup(sessionId: String): SharedAppStateBackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBackup(entity: SharedAppStateBackupEntity)

    @Query("DELETE FROM shared_app_state_backup WHERE sessionId = :sessionId")
    suspend fun deleteBackup(sessionId: String)

    @Query("UPDATE shared_app_state_backup SET lastActivityTimestamp = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateActivityTimestamp(sessionId: String, timestamp: Long)
}
