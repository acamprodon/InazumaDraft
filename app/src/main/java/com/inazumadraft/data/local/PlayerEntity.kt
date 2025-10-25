package com.inazumadraft.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val nickname: String,
    val position: String,
    val elementRef: String,
    val kick: Int,
    val speed: Int,
    val control: Int,
    val defense: Int,
    val imageRef: String,
    val seasons: List<String>,
    val secondaryPositions: List<String>
)
