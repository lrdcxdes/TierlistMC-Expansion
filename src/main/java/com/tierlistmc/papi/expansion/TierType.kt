package com.tierlistmc.papi.expansion

enum class TierType {
    NETHERPOT,
    DIAMOND,
    CRYSTAL
}

fun isTierTypeExists(tierTypeName: String): Boolean {
    return TierType.entries.any { it.name.equals(tierTypeName, ignoreCase = true) }
}
