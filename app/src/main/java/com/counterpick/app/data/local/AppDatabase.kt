package com.counterpick.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.counterpick.app.data.local.dao.CustomListDao
import com.counterpick.app.data.local.dao.HeroDao
import com.counterpick.app.data.local.dao.MatchupDao
import com.counterpick.app.data.local.dao.MetaDao
import com.counterpick.app.data.local.entity.CacheMetaEntity
import com.counterpick.app.data.local.entity.CustomCounterEntity
import com.counterpick.app.data.local.entity.CustomEntryEntity
import com.counterpick.app.data.local.entity.CustomListEntity
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.data.local.entity.MatchupEntity
import com.counterpick.app.data.local.entity.MatchupSource
import com.counterpick.app.data.local.entity.MetaCreditEntity
import com.counterpick.app.data.local.entity.MetaTierEntity

class Converters {
    @TypeConverter
    fun fromMatchupSource(value: MatchupSource): String = value.name

    @TypeConverter
    fun toMatchupSource(value: String): MatchupSource = MatchupSource.valueOf(value)
}

@Database(
    entities = [
        HeroEntity::class,
        MatchupEntity::class,
        MetaTierEntity::class,
        MetaCreditEntity::class,
        CacheMetaEntity::class,
        CustomListEntity::class,
        CustomEntryEntity::class,
        CustomCounterEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heroDao(): HeroDao
    abstract fun matchupDao(): MatchupDao
    abstract fun metaDao(): MetaDao
    abstract fun customListDao(): CustomListDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "counterpick.db"
                ).build().also { INSTANCE = it }
            }
    }
}
