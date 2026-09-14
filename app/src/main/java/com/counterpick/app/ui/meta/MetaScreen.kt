package com.counterpick.app.ui.meta

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counterpick.app.CounterPickApp
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.scoring.RoleLaneUniverse
import com.counterpick.app.scoring.TierBucket
import com.counterpick.app.scoring.tierBucketFor
import com.counterpick.app.ui.components.AvatarCircle
import com.counterpick.app.ui.components.AvatarSize
import com.counterpick.app.ui.components.MultiSelectFilterGroup
import com.counterpick.app.ui.theme.Blue
import com.counterpick.app.ui.theme.Gold
import com.counterpick.app.ui.theme.Green
import com.counterpick.app.ui.theme.TextDim

@Composable
fun MetaScreen() {
    val app = LocalContext.current.applicationContext as CounterPickApp
    val viewModel: MetaViewModel = viewModel(factory = MetaViewModel.Factory(app.repository))
    val state by viewModel.uiState.collectAsState()
    val visibleGroups = viewModel.visibleGroupsFor(state)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Meta tier list", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            state.creditText?.let {
                Text(it, fontSize = 11.sp, color = TextDim)
            }
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(16.dp))
        }

        if (state.isLoading) {
            item { Text("Loading…", color = TextDim) }
        } else if (visibleGroups.isEmpty()) {
            item { Text("No heroes match this filter.", color = TextDim) }
        } else {
            items(visibleGroups, key = { it.first }) { (tier, heroes) ->
                TierRow(tier = tier, heroes = heroes)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun TierRow(tier: String, heroes: List<HeroEntity>) {
    val bucketColor = when (tierBucketFor(tier)) {
        TierBucket.S -> Gold
        TierBucket.A -> Green
        TierBucket.B -> Blue
        TierBucket.OTHER -> TextDim
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = bucketColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        tier,
                        color = bucketColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "${heroes.size} hero${if (heroes.size == 1) "" else "es"}",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(heroes, key = { it.id }) { hero ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                        AvatarCircle(imageUrl = hero.image, heroName = hero.name, size = AvatarSize.LARGE)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            hero.name,
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
