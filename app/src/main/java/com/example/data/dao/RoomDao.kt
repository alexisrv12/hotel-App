package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.RoomEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Interface for CRUD operations on hotel rooms in Room persistence layer.
 */
@Dao
interface RoomDao {

    @Query("SELECT * FROM rooms ORDER BY sortOrder ASC, id ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoomById(id: Long): RoomEntity?

    @Query("SELECT * FROM rooms WHERE roomNumber = :roomNumber LIMIT 1")
    suspend fun getRoomByNumber(roomNumber: String): RoomEntity?

    @Query("SELECT * FROM rooms WHERE status = :status")
    fun getRoomsByStatus(status: String): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("UPDATE rooms SET status = :newStatus WHERE id = :roomId")
    suspend fun updateRoomStatus(roomId: Long, newStatus: String)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteRoomById(id: Long)

    @Query("DELETE FROM rooms")
    suspend fun deleteAllRooms()
}
