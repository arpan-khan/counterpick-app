package com.counterpick.app.ui.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counterpick.app.CounterPickApp
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.scoring.CounterResult
import com.counterpick.app.scoring.FilterEngine
import com.counterpick.app.scoring.MatchType
import com.counterpick.app.scoring.RoleLaneUniverse
import com.counterpick.app.scoring.toFilterable
import com.counterpick.app.ui.components.AvatarCircle
import com.counterpick.app.ui.components.AvatarSize
import com.counterpick.app.ui.components.HeroPickerSheet
import com.counterpick.app.ui.components.MultiSelectFilterGroup
import com.counterpick.app.ui.components.RoleLanePillRow
import com.counterpick.app.ui.theme.Green
import com.counterpick.app.ui.theme.Red

@Composable
fun DraftScreen() {
    val app = LocalContext.current.applicationContext as CounterPickApp
    val viewModel: DraftViewModel = viewModel(
        factory = DraftViewModel.Factory(app.repository, app.customListsRepository)
    )
    val state by viewModel.uiState.collectAsState()

    var pickerForSlot by rememberSaveable { mutableStateOf<Int?>(null) }

    val filteredResults = remember(state.top15, state.filterState, viewModel.heroesById) {
        FilterEngine.filter(
            state.top15.map { it.toFilterable(viewModel.heroesById) },
            state.filterState
        ).map { it.result }
    }

    if (pickerForSlot != null) {
        HeroPickerSheet(
            heroes = state.heroes,
            onPick = { hero ->
                viewModel.setEnemySlot(pickerForSlot!!, hero.id)
                pickerForSlot = null
            },
            onDismiss = { pickerForSlot = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Find your counter", style = MaterialTheme.typography.titleLarge)
            Text(
                "Enter the enemy lineup to find the best counters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            Text("Enemy team", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            EnemySlotRow(
                enemySlots = state.enemySlots,
                heroesById = viewModel.heroesById,
                onSlotClick = { index -> pickerForSlot = index },
                onSlotClear = { index -> viewModel.clearEnemySlot(index) }
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.findBestPicks() }, modifier = Modifier.fillMaxWidth()) {
                Text("Find best picks")
            }
            Spacer(Modifier.height(20.dp))
        }

        if (state.hasSearched) {
            if (state.top15.isEmpty()) {
                item {
                    EmptyMessage(
                        if (state.enemySlots.all { it == null })
                            "Add at least one enemy hero to get suggestions."
                        else
                            "No recorded matchup data for these specific heroes yet. Try different or fewer enemies."
                    )
                }
            } else {
                item {
                    LegendRow()
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Filter by role & lane", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { viewModel.resetFilters() }) { Text("Reset") }
                    }
                    Spacer(Modifier.height(4.dp))
                    MultiSelectFilterGroup(
                        label = "Role",
                        options = RoleLaneUniverse.ROLES,
                        selected = state.filterState.roles,
                        onToggle = { viewModel.toggleRole(it) },
                        onSelectAll = { viewModel.setAllRoles(true) },
                        onSelectNone = { viewModel.setAllRoles(false) }
                    )
                    Spacer(Modifier.height(10.dp))
                    MultiSelectFilterGroup(
                        label = "Lane",
                        options = RoleLaneUniverse.LANES,
                        selected = state.filterState.lanes,
                        onToggle = { viewModel.toggleLane(it) },
                        onSelectAll = { viewModel.setAllLanes(true) },
                        onSelectNone = { viewModel.setAllLanes(false) }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                itemsIndexed(filteredResults) { idx, result ->
                    ResultCard(
                        rank = idx + 1,
                        result = result,
                        hero = viewModel.heroesById[result.heroId],
                        heroName = { id -> viewModel.heroesById[id]?.name ?: "Unknown" },
                        maxAbsTotal = state.maxAbsTotal,
                        expanded = state.expandedHeroId == result.heroId,
                        onToggleExpand = { viewModel.toggleExpanded(result.heroId) },
                        enemyCount = state.enemySlots.count { it != null }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                FromYourListsSection(
                    hasAnyCustomList = state.hasAnyCustomList,
                    activeListName = state.activeListName,
                    useAllLists = state.useAllListsForSuggestions,
                    results = state.fromYourLists,
                    heroesById = viewModel.heroesById,
                    onToggleUseAllLists = { viewModel.setUseAllListsForSuggestions(it) }
                )
            }
        }
    }
}

@Composable
private fun EnemySlotRow(
    enemySlots: List<Int?>,
    heroesById: Map<Int, HeroEntity>,
    onSlotClick: (Int) -> Unit,
    onSlotClear: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        enemySlots.forEachIndexed { index, heroId ->
            val hero = heroId?.let { heroesById[it] }
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { onSlotClick(index) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (hero != null) {
                            AvatarCircle(imageUrl = hero.image, heroName = hero.name, size = AvatarSize.MEDIUM)
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = "Add enemy ${index + 1}")
                        }
                    }
                }
                if (hero != null) {
                    IconButton(onClick = { onSlotClear(index) }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row {
        Text("Green", color = Green, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(" = Good Counter   ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Red", color = Red, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(" = Bad Pick", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultCard(
    rank: Int,
    result: CounterResult,
    hero: HeroEntity?,
    heroName: (Int) -> String,
    maxAbsTotal: Double,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    enemyCount: Int
) {
    val isGood = result.total >= 0
    val color = if (isGood) Green else Red
    val barWidth = (kotlin.math.abs(result.total) / maxAbsTotal).coerceIn(0.0, 1.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RankChip(rank)
                if (hero != null) AvatarCircle(hero.image, hero.name, AvatarSize.MEDIUM)
                Text(
                    text = hero?.name ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${if (isGood) "+" else ""}${"%.2f".format(result.total * 100)}%",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(barWidth.toFloat())
                        .height(4.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
            Spacer(Modifier.height(6.dp))
            if (hero != null) {
                RoleLanePillRow(roles = hero.roles, lanes = hero.lanes)
            }
            Text(
                "Data for ${result.matched.size} of $enemyCount selected ${if (enemyCount > 1) "enemies" else "enemy"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onToggleExpand) {
                Text(if (expanded) "Hide reasoning" else "Show reasoning")
            }
            if (expanded) {
                Column {
                    result.matched.forEach { m ->
                        val line = if (m.type == MatchType.GOOD) {
                            "Beats ${heroName(m.enemyId)} · #${m.rank} · +${"%.2f".format(m.score * 100)}%"
                        } else {
                            "${heroName(m.enemyId)} beats this hero · #${m.rank} · ${"%.2f".format(m.score * 100)}%"
                        }
                        Text(line, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankChip(rank: Int) {
    val bg = when (rank) {
        1 -> Color(0xFFFFB238).copy(alpha = 0.15f)
        2 -> Color(0xFFCFD6E4).copy(alpha = 0.3f)
        3 -> Color(0xFFC9853B).copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(9.dp)) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Text(rank.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FromYourListsSection(
    hasAnyCustomList: Boolean,
    activeListName: String?,
    useAllLists: Boolean,
    results: List<com.counterpick.app.scoring.FromYourListsResult>,
    heroesById: Map<Int, HeroEntity>,
    onToggleUseAllLists: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("From your lists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (hasAnyCustomList) {
            TextButton(onClick = { onToggleUseAllLists(!useAllLists) }) {
                Text(if (useAllLists) "Searching all lists" else "Searching active list")
            }
        }
    }
    Spacer(Modifier.height(8.dp))

    when {
        !hasAnyCustomList -> EmptyMessage(
            "You haven't created any custom counter lists yet. Build one in the Lists tab and your own picks will show up here, ranked separately from the official data above."
        )
        !useAllLists && activeListName == null -> EmptyMessage(
            "You have lists, but none is set active. Set one active in the Lists tab, or tap above to search across all your lists."
        )
        results.isEmpty() -> EmptyMessage(
            if (useAllLists) "None of your lists have a counter entered for these enemies yet."
            else "\"$activeListName\" doesn't have a counter entered for these enemies yet."
        )
        else -> {
            Column {
                results.forEach { r ->
                    val hero = heroesById[r.heroId]
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(10.dp)
                        ) {
                            if (hero != null) AvatarCircle(hero.image, hero.name, AvatarSize.SMALL)
                            Text(hero?.name ?: "Unknown", modifier = Modifier.weight(1f))
                            Text(
                                "counters ${r.matchedEnemyCount}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "your #${r.bestPosition + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
