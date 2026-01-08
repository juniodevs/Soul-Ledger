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
import org.bukkit.event.player.PlayerRespawnEvent;

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

        if (pactManager.isHeartLinked(player)) {
            Player partner = pactManager.getHeartPartner(player);
            if (partner != null && partner.isOnline() && !partner.isDead()) {
                try {
                    partner.setHealth(0.0);
                } catch (Exception ignored) {
                    partner.damage(1000.0);
                }
            }
            pactManager.removeHeartLink(player);
        }

        pactManager.revokeAllPacts(player);
        plugin.getMobListener().removeAllSouls(player);
        player.sendMessage(plugin.getFormattedMessage("pact_revoked"));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pactManager.getPlayerPacts(player).isEmpty()) {
                 pactManager.forceResetAttributes(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        pactManager.restorePacts(event.getPlayer());
    }
}
