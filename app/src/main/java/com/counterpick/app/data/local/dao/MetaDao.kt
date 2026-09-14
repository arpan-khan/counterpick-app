package com.counterpick.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.counterpick.app.data.local.entity.CacheMetaEntity
import com.counterpick.app.data.local.entity.MetaCreditEntity
import com.counterpick.app.data.local.entity.MetaTierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiers(tiers: List<MetaTierEntity>)

    @Query("DELETE FROM meta_tiers")
    suspend fun clearTiers()

    @Query("SELECT * FROM meta_tiers")
    fun observeTiers(): Flow<List<MetaTierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredit(credit: MetaCreditEntity)

    @Query("SELECT * FROM meta_credit WHERE id = 0 LIMIT 1")
    fun observeCredit(): Flow<MetaCreditEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCacheMeta(meta: CacheMetaEntity)

    @Query("SELECT * FROM cache_meta WHERE id = 0 LIMIT 1")
    fun observeCacheMeta(): Flow<CacheMetaEntity?>

    @Query("SELECT * FROM cache_meta WHERE id = 0 LIMIT 1")
    suspend fun getCacheMeta(): CacheMetaEntity?
}
