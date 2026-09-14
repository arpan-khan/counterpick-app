package com.counterpick.app.data.repository

import com.counterpick.app.data.local.dao.HeroDao
import com.counterpick.app.data.local.dao.MatchupDao
import com.counterpick.app.data.local.dao.MetaDao
import com.counterpick.app.data.local.entity.CacheMetaEntity
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.data.local.entity.MatchupEntity
import com.counterpick.app.data.local.entity.MatchupSource
import com.counterpick.app.data.local.entity.MetaCreditEntity
import com.counterpick.app.data.local.entity.MetaTierEntity
import com.counterpick.app.data.model.DataJsonDto
import com.counterpick.app.data.model.MetaJsonParser
import com.counterpick.app.data.remote.FetchResult
import com.counterpick.app.data.remote.GitHubDataSource
import com.counterpick.app.scoring.MainRecord
import com.counterpick.app.scoring.buildByMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json

sealed class UpdateOutcome {
    data class Updated(val newGenerated: String?) : UpdateOutcome()
    object AlreadyUpToDate : UpdateOutcome()
    data class Failed(val reason: String) : UpdateOutcome()
}

class CounterPickRepository(
    private val heroDao: HeroDao,
    private val matchupDao: MatchupDao,
    private val metaDao: MetaDao,
    private val remote: GitHubDataSource = GitHubDataSource()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _byMain = MutableStateFlow<Map<Int, MainRecord>>(emptyMap())
    val byMain: StateFlow<Map<Int, MainRecord>> = _byMain.asStateFlow()

    init {
        heroDao.observeAll()
            .onEach { _byMain.value = buildByMain(matchupDao.getAll()) }
            .launchIn(repoScope)
    }

    fun observeHeroes() = heroDao.observeAll()
    fun observeMetaTiers() = metaDao.observeTiers()
    fun observeCredit() = metaDao.observeCredit()
    fun observeCacheMeta() = metaDao.observeCacheMeta()
    suspend fun hasCachedData(): Boolean = metaDao.getCacheMeta() != null
    suspend fun getAllHeroes(): List<HeroEntity> = heroDao.getAll()
    suspend fun getAllMatchups(): List<MatchupEntity> = matchupDao.getAll()

    suspend fun updateFromRemote(dataJsonUrl: String, metaJsonUrl: String): UpdateOutcome {
        val dataResult = remote.fetchDataJson(dataJsonUrl)
        val dataBody = when (dataResult) {
            is FetchResult.Success -> dataResult.body
            is FetchResult.Failure -> return UpdateOutcome.Failed(dataResult.message)
        }

        val dto: DataJsonDto = try {
            json.decodeFromString(DataJsonDto.serializer(), dataBody)
        } catch (e: Exception) {
            return UpdateOutcome.Failed("Malformed data.json: ${e.message}")
        }

        val cached = metaDao.getCacheMeta()
        if (cached?.generated != null && dto.generated != null && cached.generated >= dto.generated) {
            return UpdateOutcome.AlreadyUpToDate
        }

        val metaBody = (remote.fetchMetaJson(metaJsonUrl) as? FetchResult.Success)?.body

        persist(dto, metaBody)
        return UpdateOutcome.Updated(dto.generated)
    }

    private suspend fun persist(dto: DataJsonDto, metaBody: String?) {
        val heroEntities = dto.heroes.map { (idStr, hero) ->
            HeroEntity(
                id = idStr.toInt(),
                name = hero.name,
                image = hero.image,
                roleCsv = HeroEntity.packList(hero.role),
                laneCsv = HeroEntity.packList(hero.lane)
            )
        }

        val matchupEntities = mutableListOf<MatchupEntity>()
        dto.best.forEach { row ->

            matchupEntities.add(
                MatchupEntity(
                    mainId = row[0].toInt(),
                    otherId = row[1].toInt(),
                    score = row[2],
                    rank = row[3].toInt(),
                    source = MatchupSource.BEST
                )
            )
        }
        dto.worst.forEach { row ->

            matchupEntities.add(
                MatchupEntity(
                    mainId = row[0].toInt(),
                    otherId = row[1].toInt(),
                    score = row[2],
                    rank = row[3].toInt(),
                    source = MatchupSource.WORST
                )
            )
        }

        heroDao.clearAll()
        heroDao.insertAll(heroEntities)
        matchupDao.clearAll()
        matchupDao.insertAll(matchupEntities)

        if (metaBody != null) {
            try {
                val metaJson = MetaJsonParser.parse(metaBody)
                val nameToId = heroEntities.associate { it.name.lowercase() to it.id }
                val tierEntities = metaJson.tiers.flatMap { (tier, names) ->
                    names.mapNotNull { name ->
                        nameToId[name.lowercase()]?.let { id -> MetaTierEntity(id, tier) }
                    }
                }
                metaDao.clearTiers()
                metaDao.insertTiers(tierEntities)
                metaDao.upsertCredit(MetaCreditEntity(text = metaJson.credit.text, url = metaJson.credit.url))
            } catch (_: Exception) {

            }
        }

        metaDao.upsertCacheMeta(
            CacheMetaEntity(generated = dto.generated, lastFetchedAtEpochMs = System.currentTimeMillis())
        )
    }
}
