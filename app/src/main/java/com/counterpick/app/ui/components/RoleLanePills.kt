package com.counterpick.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.counterpick.app.ui.theme.RoleColors
import com.counterpick.app.ui.theme.Surface3
import com.counterpick.app.ui.theme.TextDim

@Composable
fun RoleLanePillRow(
    roles: List<String>,
    lanes: List<String>,
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp
) {
    Row(modifier = modifier) {
        roles.forEach { role ->
            Pill(text = role, color = RoleColors.forRole(role), background = RoleColors.forRole(role).copy(alpha = 0.13f))
            Spacer(Modifier.width(spacing))
        }
        lanes.forEach { lane ->
            Pill(text = lane, color = TextDim, background = Surface3)
            Spacer(Modifier.width(spacing))
        }
    }
}

@Composable
private fun Pill(text: String, color: Color, background: Color) {
    Surface(
        color = background,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}
