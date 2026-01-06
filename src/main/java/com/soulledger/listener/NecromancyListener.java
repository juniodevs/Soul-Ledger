package com.soulledger.listener;
import org.bukkit.event.entity.EntityExplodeEvent;

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
import java.util.List;

public class NecromancyListener implements Listener {
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof org.bukkit.entity.Creeper && entity.hasMetadata("necromancer_owner")) {
            String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
            Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
            if (owner != null && owner.isOnline()) {
                double currentMax = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.min(20.0, currentMax + 2));
                owner.sendMessage(org.bukkit.ChatColor.GREEN + "Você recuperou 1 coração ao perder sua alma invocada (Creeper explodiu).");
                List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
                if (list != null) {
                    list.remove(entity);
                }
            }
        }
    }

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
    // Mapeia jogador -> lista de almas invocadas
    private final Map<UUID, List<LivingEntity>> summonedSouls = new HashMap<>();
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
                // Remove da lista de almas
                List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
                if (list != null) {
                    list.remove(entity);
                }
            }
        }
    }

    // Adiciona alma à lista do necromante
    public void addSummonedSoul(Player owner, LivingEntity soul) {
        summonedSouls.computeIfAbsent(owner.getUniqueId(), k -> new java.util.ArrayList<>()).add(soul);
    }

    // Lista as almas invocadas pelo jogador
    public java.util.List<LivingEntity> getSummonedSouls(Player owner) {
        return summonedSouls.getOrDefault(owner.getUniqueId(), java.util.Collections.emptyList());
    }

    // Remove todas as almas do necromante (ex: ao perder pacto)
    public void removeAllSouls(Player owner) {
        List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
        if (list != null) {
            for (LivingEntity soul : new java.util.ArrayList<>(list)) {
                if (!soul.isDead()) soul.remove();
            }
            list.clear();
        }
    }

    public EntityDeathEvent getLastMobDeath(Player player) {
        return lastMobDeath.get(player.getUniqueId());
    }

    public void clearLastMobDeath(Player player) {
        lastMobDeath.remove(player.getUniqueId());
    }
}
