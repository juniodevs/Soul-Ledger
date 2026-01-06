package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.manager.PactManager;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerDeathListener implements Listener {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;

    public PlayerDeathListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        pactManager.revokeAllPacts(player);
        
        player.sendMessage(plugin.getFormattedMessage("pact_revoked"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        pactManager.restorePacts(event.getPlayer());
    }
}
