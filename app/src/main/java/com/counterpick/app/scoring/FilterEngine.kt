package com.counterpick.app.scoring

object RoleLaneUniverse {
    val ROLES = listOf("Tank", "Fighter", "Assassin", "Mage", "Marksman", "Support")
    val LANES = listOf("Exp Lane", "Jungle", "Mid Lane", "Gold Lane", "Roam")
}

data class RoleLaneFilterState(
    val roles: Set<String> = RoleLaneUniverse.ROLES.toSet(),
    val lanes: Set<String> = RoleLaneUniverse.LANES.toSet()
) {
    fun toggleRole(role: String): RoleLaneFilterState =
        copy(roles = if (role in roles) roles - role else roles + role)

    fun toggleLane(lane: String): RoleLaneFilterState =
        copy(lanes = if (lane in lanes) lanes - lane else lanes + lane)

    fun withAllRoles(selectAll: Boolean): RoleLaneFilterState =
        copy(roles = if (selectAll) RoleLaneUniverse.ROLES.toSet() else emptySet())

    fun withAllLanes(selectAll: Boolean): RoleLaneFilterState =
        copy(lanes = if (selectAll) RoleLaneUniverse.LANES.toSet() else emptySet())
}

interface RoleLaneTagged {
    val roles: List<String>
    val lanes: List<String>
}

object FilterEngine {

    fun <T : RoleLaneTagged> matches(item: T, state: RoleLaneFilterState): Boolean {
        val roleOk = item.roles.isEmpty() || item.roles.any { it in state.roles }
        val laneOk = item.lanes.isEmpty() || item.lanes.any { it in state.lanes }
        return roleOk && laneOk
    }

    fun <T : RoleLaneTagged> filter(items: List<T>, state: RoleLaneFilterState): List<T> =
        items.filter { matches(it, state) }
}
