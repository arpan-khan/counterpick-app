package com.counterpick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.counterpick.app.ui.theme.Surface3

enum class AvatarSize(val dp: Dp, val fontSp: Int) {
    SMALL(30.dp, 12),
    MEDIUM(38.dp, 13),
    LARGE(64.dp, 22)
}

@Composable
fun AvatarCircle(
    imageUrl: String?,
    heroName: String,
    size: AvatarSize,
    modifier: Modifier = Modifier
) {
    var loadFailed by remember(imageUrl) { mutableStateOf(imageUrl.isNullOrBlank()) }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Surface3)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!loadFailed && imageUrl != null) {

            AsyncImage(
                model = imageUrl,
                contentDescription = heroName,
                modifier = Modifier.size(size.dp).clip(CircleShape),
                onError = { loadFailed = true }
            )
        } else {
            Text(
                text = heroName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = size.fontSp.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
