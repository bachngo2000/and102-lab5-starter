package com.codepath.articlesearch

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// defines an AppDatabase class to hold the database. AppDatabase defines the database configuration and serves as the app's main access point to the persisted data
// The class must be annotated with a @Database annotation that includes an entities array that lists all of the data entities associated with the database
@Database(entities = [ArticleEntity::class], version = 1)
// The class must be an abstract class that extends RoomDatabase.
abstract class AppDatabase : RoomDatabase() {

    // For each DAO class that is associated with the database, the database class must define an abstract
    // method that has zero arguments and returns an instance of the DAO class.
    abstract fun articleDao(): ArticleDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "Articles-db"
            ).build()
    }

}