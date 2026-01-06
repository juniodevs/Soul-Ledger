package com.soulledger.listener;

import com.soulledger.manager.PactManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NecromancyListener implements Listener {

            @EventHandler
            public void onEntityTarget(EntityTargetEvent event) {
                if (!(event.getEntity() instanceof org.bukkit.entity.Zombie)) return;
                if (!(event.getTarget() instanceof Player)) return;
                LivingEntity entity = (LivingEntity) event.getEntity();
                if (entity.hasMetadata("necromancer_owner")) {
                    String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
                    Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
                    if (owner != null && owner.equals(event.getTarget())) {
                        event.setCancelled(true);
                    }
                }
            }
        // Mapeia entidades invocadas para o dono
        private final Map<UUID, UUID> summonedToOwner = new HashMap<>();
    private final PactManager pactManager;
    private final Plugin plugin;
    // Guarda o último mob morto por cada jogador
    private final Map<UUID, EntityDeathEvent> lastMobDeath = new HashMap<>();

    public NecromancyListener(PactManager pactManager, Plugin plugin) {
        this.pactManager = pactManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Salva o último mob morto para necromancia
        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = event.getEntity().getKiller();
            if (pactManager.hasNecromancyPact(killer)) {
                lastMobDeath.put(killer.getUniqueId(), event);
            }
        }
        // Se for uma alma invocada, devolve o coração ao dono
        LivingEntity entity = event.getEntity();
        if (entity.hasMetadata("necromancer_owner")) {
            String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
            Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
            if (owner != null && owner.isOnline()) {
                double currentMax = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.min(20.0, currentMax + 2));
                owner.sendMessage(org.bukkit.ChatColor.GREEN + "Você recuperou 1 coração ao perder sua alma invocada.");
            }
        }
    }

    public EntityDeathEvent getLastMobDeath(Player player) {
        return lastMobDeath.get(player.getUniqueId());
    }

    public void clearLastMobDeath(Player player) {
        lastMobDeath.remove(player.getUniqueId());
    }
}
