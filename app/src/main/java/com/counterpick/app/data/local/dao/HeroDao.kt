package com.counterpick.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.counterpick.app.data.local.entity.HeroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(heroes: List<HeroEntity>)

    @Query("DELETE FROM heroes")
    suspend fun clearAll()

    @Query("SELECT * FROM heroes ORDER BY name ASC")
    fun observeAll(): Flow<List<HeroEntity>>

    @Query("SELECT * FROM heroes ORDER BY name ASC")
    suspend fun getAll(): List<HeroEntity>

    @Query("SELECT * FROM heroes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): HeroEntity?

    @Query("SELECT * FROM heroes WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): HeroEntity?
}
