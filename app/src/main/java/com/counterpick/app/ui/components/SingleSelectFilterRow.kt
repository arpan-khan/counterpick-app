package com.counterpick.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SingleSelectFilterRow(
    label: String,
    allLabel: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, contentPadding = PaddingValues(horizontal = 2.dp)) {
        item {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = { onSelect("") },
                label = { Text(allLabel) },
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        items(options) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) },
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
