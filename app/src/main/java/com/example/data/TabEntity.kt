package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val isDesktopMode: Boolean = false,
    val isReaderMode: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis(),
    val positionIndex: Int = 0,
    val isActive: Boolean = false
)
