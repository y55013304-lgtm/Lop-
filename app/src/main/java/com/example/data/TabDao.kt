package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TabDao {
    @Query("SELECT * FROM saved_tabs ORDER BY positionIndex ASC")
    suspend fun getAllTabs(): List<TabEntity>

    @Query("SELECT COUNT(*) FROM saved_tabs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<TabEntity>)

    @Query("DELETE FROM saved_tabs WHERE id = :id")
    suspend fun deleteTab(id: String)

    @Query("DELETE FROM saved_tabs")
    suspend fun clearAllTabs()

    @Transaction
    suspend fun replaceAllTabs(tabs: List<TabEntity>) {
        clearAllTabs()
        if (tabs.isNotEmpty()) {
            insertTabs(tabs)
        }
    }
}
