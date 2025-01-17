package com.gbros.tabslite.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gbros.tabslite.utilities.DATABASE_NAME

/**
 * The Room database for this app
 */
@Database(entities = [Tab::class, ChordVariation::class, Playlist::class, PlaylistEntry::class], version = 9, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chordVariationDao(): ChordVariationDao
    abstract fun tabFullDao(): TabDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistEntryDao(): PlaylistEntryDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tabs ADD COLUMN transposed INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE garden_plantings")
                db.execSQL("DROP TABLE plants")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE chord_variation")
                db.execSQL("CREATE TABLE IF NOT EXISTS chord_variation (id TEXT NOT NULL, chord_id TEXT NOT NULL, chord_markers TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE chord_variation")
                db.execSQL("CREATE TABLE IF NOT EXISTS chord_variation (id TEXT NOT NULL, chord_id TEXT NOT NULL, note_chord_markers TEXT NOT NULL, open_chord_markers TEXT NOT NULL, muted_chord_markers TEXT NOT NULL, bar_chord_markers TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tabs ADD COLUMN favorite_time INTEGER DEFAULT NULL")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            // add the playlist functionality / data
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist (id INTEGER NOT NULL, user_created INTEGER NOT NULL, title TEXT NOT NULL, date_created INTEGER NOT NULL, date_modified INTEGER NOT NULL, description TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist_entry (id INTEGER NOT NULL, playlist_id INTEGER NOT NULL, tab_id INTEGER NOT NULL, next_entry_id INTEGER, prev_entry_id INTEGER, date_added INTEGER NOT NULL, transpose INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            // migrate favorites over to the playlist with special ID -1
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO playlist_entry (playlist_id, tab_id, next_entry_id, prev_entry_id, date_added, transpose) SELECT -1, id, NULL, NULL, favorite_time, transposed FROM tabs WHERE favorite IS 1")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            // rename playlist_entry.id to playlist_entry.entry_id
            // remove unused columns from tabs table
            override fun migrate(db: SupportSQLiteDatabase) {
                // create new temp table
                db.execSQL("CREATE TABLE IF NOT EXISTS playlist_entry_new (entry_id INTEGER NOT NULL, playlist_id INTEGER NOT NULL, tab_id INTEGER NOT NULL, next_entry_id INTEGER, prev_entry_id INTEGER, date_added INTEGER NOT NULL, transpose INTEGER NOT NULL, PRIMARY KEY(entry_id))")

                // copy data from old table to new
                db.execSQL("INSERT INTO playlist_entry_new (entry_id, playlist_id, tab_id, next_entry_id, prev_entry_id, date_added, transpose) SELECT id, playlist_id, tab_id, next_entry_id, prev_entry_id, date_added, transpose FROM playlist_entry")

                // delete old playlist_entry table
                db.execSQL("DROP TABLE playlist_entry")

                // rename new table to playlist_entry
                db.execSQL("ALTER TABLE playlist_entry_new RENAME TO playlist_entry")
            }
        }


        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
        }
    }
}
