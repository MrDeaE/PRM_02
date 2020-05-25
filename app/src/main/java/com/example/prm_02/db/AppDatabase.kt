package com.example.prm_02.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Image::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val DATABASE_NAME = "appdb"
    }

    abstract fun imageDao(): ImageDao
}