package com.tierlistmc.papi.expansion

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class LeaveListener(private val expansion: TierlistMC) : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        expansion.onPlayerLeave(event.player.name)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        expansion.onPlayerLeave(event.player.name)
    }
}