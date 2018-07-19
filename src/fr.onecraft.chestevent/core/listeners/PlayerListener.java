package fr.onecraft.chestevent.core.listeners;

import fr.onecraft.chestevent.ChestEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private ChestEvent plugin;

    public PlayerListener(ChestEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        plugin.getPagers().remove(event.getPlayer().getUniqueId());
    }
}