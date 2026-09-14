package com.counterpick.app.ui.lists

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counterpick.app.CounterPickApp
import com.counterpick.app.data.local.entity.CustomCounterEntity
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.ui.components.AvatarCircle
import com.counterpick.app.ui.components.AvatarSize
import com.counterpick.app.ui.components.DragReorderColumn
import com.counterpick.app.ui.components.HeroPickerSheet
import com.counterpick.app.ui.theme.Gold
import com.counterpick.app.ui.theme.TextDim

@Composable
fun ListsScreen() {
    val app = LocalContext.current.applicationContext as CounterPickApp
    val viewModel: ListsViewModel = viewModel(
        factory = ListsViewModel.Factory(app.repository, app.customListsRepository)
    )
    val state by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showEnemyPicker by remember { mutableStateOf(false) }
    var counterPickerForEntry by remember { mutableStateOf<Long?>(null) }

    if (showCreateDialog) {
        NameDialog(
            title = "New list",
            onConfirm = { name -> viewModel.createList(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
    if (showRenameDialog) {
        NameDialog(
            title = "Rename list",
            onConfirm = { name -> viewModel.renameSelectedList(name); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
    if (showEnemyPicker) {
        HeroPickerSheet(
            heroes = state.heroes,
            onPick = { hero -> viewModel.addEnemyEntry(hero.id); showEnemyPicker = false },
            onDismiss = { showEnemyPicker = false }
        )
    }
    counterPickerForEntry?.let { entryId ->
        HeroPickerSheet(
            heroes = state.heroes,
            onPick = { hero -> viewModel.addCounter(entryId, hero.id); counterPickerForEntry = null },
            onDismiss = { counterPickerForEntry = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("My Lists", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Custom lists",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim
            )
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.lists, key = { it.listId }) { list ->
                    FilterChip(
                        selected = state.selectedListId == list.listId,
                        onClick = { viewModel.selectList(list.listId) },
                        label = { Text(list.name) },
                        leadingIcon = {
                            if (state.activeListId == list.listId) {
                                Icon(Icons.Filled.Star, contentDescription = "Active", tint = Gold, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showCreateDialog = true },
                        label = { Text("New list") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        val selectedList = state.lists.find { it.listId == state.selectedListId }
        if (selectedList != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.activeListId != selectedList.listId) {
                    TextButton(onClick = { viewModel.setActiveList(selectedList.listId) }) {
                        Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Set active")
                    }
                }
                TextButton(onClick = { showRenameDialog = true }) { Text("Rename") }
                TextButton(onClick = { viewModel.deleteList(selectedList.listId) }) { Text("Delete") }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Button(onClick = { showEnemyPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add enemy")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (state.entries.isEmpty()) {
                    item { Text("No enemies added to this list yet.", color = TextDim) }
                } else {
                    items(state.entries, key = { it.entryId }) { entry ->
                        EntryCard(
                            entry = entry,
                            heroesById = state.heroesById,
                            onAddCounter = { counterPickerForEntry = entry.entryId },
                            onRemoveCounter = { counterId -> viewModel.removeCounter(counterId, entry.entryId) },
                            onDeleteEntry = { viewModel.deleteEntry(entry.entryId) },
                            onReorderPreview = { from, to -> viewModel.previewReorder(entry.entryId, from, to) },
                            onReorderCommit = { viewModel.commitReorder(entry.entryId) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(
    entry: EntryUiModel,
    heroesById: Map<Int, HeroEntity>,
    onAddCounter: () -> Unit,
    onRemoveCounter: (Long) -> Unit,
    onDeleteEntry: () -> Unit,
    onReorderPreview: (Int, Int) -> Unit,
    onReorderCommit: () -> Unit
) {
    val enemyHero = heroesById[entry.enemyHeroId]
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (enemyHero != null) AvatarCircle(enemyHero.image, enemyHero.name, AvatarSize.SMALL)
                Spacer(Modifier.width(8.dp))
                Text(
                    "vs ${enemyHero?.name ?: "Unknown"}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteEntry) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove enemy")
                }
            }
            Spacer(Modifier.height(8.dp))

            if (entry.counters.isEmpty()) {
                Text("No counters added yet.", color = TextDim, fontSize = 12.sp)
            } else {
                DragReorderColumn(
                    items = entry.counters,
                    key = { it.counterId },
                    onReorder = { from, to -> onReorderPreview(from, to) },
                    onDragEnd = onReorderCommit,
                    itemHeight = 48.dp
                ) { counter, isDragging ->
                    CounterRow(
                        counter = counter,
                        hero = heroesById[counter.counterHeroId],
                        position = entry.counters.indexOf(counter),
                        isDragging = isDragging,
                        onRemove = { onRemoveCounter(counter.counterId) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onAddCounter) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add counter")
            }
        }
    }
}

@Composable
private fun CounterRow(
    counter: CustomCounterEntity,
    hero: HeroEntity?,
    position: Int,
    isDragging: Boolean,
    onRemove: () -> Unit
) {
    Surface(
        color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Filled.DragHandle, contentDescription = "Drag to reorder", tint = TextDim)
            Spacer(Modifier.width(6.dp))
            Text("#${position + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
            if (hero != null) AvatarCircle(hero.image, hero.name, AvatarSize.SMALL)
            Spacer(Modifier.width(8.dp))
            Text(hero?.name ?: "Unknown", modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove counter", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
