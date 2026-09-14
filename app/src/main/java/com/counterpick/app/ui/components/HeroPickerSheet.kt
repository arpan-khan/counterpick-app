package com.counterpick.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.counterpick.app.data.local.entity.HeroEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroPickerSheet(
    heroes: List<HeroEntity>,
    onPick: (HeroEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, heroes) {
        if (query.isBlank()) heroes
        else heroes.filter { it.name.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search hero") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                items(filtered, key = { it.id }) { hero ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(hero) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarCircle(imageUrl = hero.image, heroName = hero.name, size = AvatarSize.MEDIUM)
                        Column {
                            Text(hero.name)
                            RoleLanePillRow(roles = hero.roles, lanes = hero.lanes)
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
