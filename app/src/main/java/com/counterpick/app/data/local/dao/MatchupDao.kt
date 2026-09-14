package com.counterpick.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.counterpick.app.data.local.entity.MatchupEntity

@Dao
interface MatchupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<MatchupEntity>)

    @Query("DELETE FROM matchups")
    suspend fun clearAll()

    @Query("SELECT * FROM matchups")
    suspend fun getAll(): List<MatchupEntity>

    @Query("SELECT * FROM matchups WHERE mainId = :mainId")
    suspend fun getForMain(mainId: Int): List<MatchupEntity>
}
