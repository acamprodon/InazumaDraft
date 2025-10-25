package com.inazumadraft.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PlayerEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InazumaDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
}