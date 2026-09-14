package com.counterpick.app.ui.theme

import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0A0D12)
val Surface = Color(0xFF121722)
val Surface2 = Color(0xFF1A2130)
val Surface3 = Color(0xFF212A3B)
val Border = Color(0xFF252E3F)
val BorderSoft = Color(0xFF1B2230)
val TextMain = Color(0xFFEEF1F6)
val TextDim = Color(0xFF8891A3)
val TextFaint = Color(0xFF525B6C)

val Gold = Color(0xFFFFB238)
val Blue = Color(0xFF4C8DFF)
val Red = Color(0xFFFF4D5E)
val Green = Color(0xFF2FD883)

object RoleColors {
    val Tank = Blue
    val Fighter = Red
    val Assassin = Gold
    val Mage = Color(0xFFB07CFF)
    val Marksman = Green
    val Support = Color(0xFF3FBFB0)

    fun forRole(role: String): Color = when (role) {
        "Tank" -> Tank
        "Fighter" -> Fighter
        "Assassin" -> Assassin
        "Mage" -> Mage
        "Marksman" -> Marksman
        "Support" -> Support
        else -> TextDim
    }
}
