package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final SoulLedgerPlugin plugin;

    public GuiListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        plugin.getPactGui().handleInternalClick(event);
        
        if (plugin.getNecromancerSummonGui() != null) {
            plugin.getNecromancerSummonGui().handleInternalClick(event);
        }
    }
}
