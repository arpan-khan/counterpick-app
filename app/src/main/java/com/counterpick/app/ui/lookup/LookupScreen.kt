package com.counterpick.app.ui.lookup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counterpick.app.CounterPickApp
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.scoring.FilterEngine
import com.counterpick.app.scoring.Matchup
import com.counterpick.app.scoring.RoleLaneUniverse
import com.counterpick.app.scoring.toFilterable
import com.counterpick.app.ui.components.AvatarCircle
import com.counterpick.app.ui.components.AvatarSize
import com.counterpick.app.ui.components.HeroPickerSheet
import com.counterpick.app.ui.components.MultiSelectFilterGroup
import com.counterpick.app.ui.components.RoleLanePillRow
import com.counterpick.app.ui.theme.Green
import com.counterpick.app.ui.theme.Red
import com.counterpick.app.ui.theme.TextDim

@Composable
fun LookupScreen() {
    val app = LocalContext.current.applicationContext as CounterPickApp
    val viewModel: LookupViewModel = viewModel(factory = LookupViewModel.Factory(app.repository))
    val state by viewModel.uiState.collectAsState()

    var showPicker by remember { mutableStateOf(false) }

    val browseFiltered = remember(state.heroes, state.browseFilterState) {
        val filterableIds = FilterEngine
            .filter(state.heroes.map { it.toFilterable() }, state.browseFilterState)
            .map { it.heroId }
            .toSet()
        state.heroes.filter { it.id in filterableIds }
    }

    if (showPicker) {
        HeroPickerSheet(
            heroes = state.heroes,
            onPick = { hero ->
                viewModel.selectHero(hero.id)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Hero lookup", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.selectedHeroId?.let { viewModel.heroesById[it]?.name } ?: "Search hero…")
                }
                Spacer(Modifier.height(16.dp))

                val selectedHero = state.selectedHeroId?.let { viewModel.heroesById[it] }
                if (selectedHero != null) {
                    HeroProfileHeader(selectedHero)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MatchupColumn(
                            title = "Best picks against ${selectedHero.name}",
                            titleColor = Green,
                            matchups = state.bestPicksAgainst,
                            heroesById = viewModel.heroesById,
                            positive = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        MatchupColumn(
                            title = "${selectedHero.name} beats well",
                            titleColor = Red,
                            matchups = state.beatsWell,
                            heroesById = viewModel.heroesById,
                            positive = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Browse all heroes", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.resetBrowseFilters() }) { Text("Reset") }
                }
                Spacer(Modifier.height(8.dp))
                MultiSelectFilterGroup(
                    label = "Role",
                    options = RoleLaneUniverse.ROLES,
                    selected = state.browseFilterState.roles,
                    onToggle = { viewModel.toggleBrowseRole(it) },
                    onSelectAll = { viewModel.setAllBrowseRoles(true) },
                    onSelectNone = { viewModel.setAllBrowseRoles(false) }
                )
                Spacer(Modifier.height(10.dp))
                MultiSelectFilterGroup(
                    label = "Lane",
                    options = RoleLaneUniverse.LANES,
                    selected = state.browseFilterState.lanes,
                    onToggle = { viewModel.toggleBrowseLane(it) },
                    onSelectAll = { viewModel.setAllBrowseLanes(true) },
                    onSelectNone = { viewModel.setAllBrowseLanes(false) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        items(browseFiltered, key = { it.id }) { hero ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.selectHero(hero.id) }
            ) {
                AvatarCircle(imageUrl = hero.image, heroName = hero.name, size = AvatarSize.MEDIUM)
                Spacer(Modifier.height(3.dp))
                Text(hero.name, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HeroProfileHeader(hero: HeroEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarCircle(imageUrl = hero.image, heroName = hero.name, size = AvatarSize.LARGE)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(hero.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            RoleLanePillRow(roles = hero.roles, lanes = hero.lanes)
        }
    }
}

@Composable
private fun MatchupColumn(
    title: String,
    titleColor: Color,
    matchups: List<Matchup>,
    heroesById: Map<Int, HeroEntity>,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            if (matchups.isEmpty()) {
                Text("No data", color = TextDim, fontSize = 11.sp)
            } else {
                matchups.forEach { m ->
                    val hero = heroesById[m.other]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        AvatarCircle(imageUrl = hero?.image, heroName = hero?.name ?: "?", size = AvatarSize.SMALL)
                        Spacer(Modifier.width(6.dp))
                        Text(hero?.name ?: "Unknown", fontSize = 11.5.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(
                            "${if (positive) "+" else "-"}${"%.2f".format(m.score * 100)}%",
                            color = if (positive) Green else Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
