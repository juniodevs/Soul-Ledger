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
        try {
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
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em onEntityExplode: " + e.getMessage());
        }
    }

            @EventHandler
            public void onEntityTarget(EntityTargetEvent event) {
        try {
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
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em onEntityTarget: " + e.getMessage());
        }
            }
    private final Map<UUID, List<LivingEntity>> summonedSouls = new HashMap<>();
    private final PactManager pactManager;
    private final Plugin plugin;
    private final Map<UUID, EntityDeathEvent> lastMobDeath = new HashMap<>();

    public NecromancyListener(PactManager pactManager, Plugin plugin) {
        this.pactManager = pactManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        try {
            if (event.getEntity().getKiller() instanceof Player) {
                Player killer = event.getEntity().getKiller();
                if (pactManager.hasNecromancyPact(killer)) {
                    lastMobDeath.put(killer.getUniqueId(), event);
                }
            }
            LivingEntity entity = event.getEntity();
            if (entity.hasMetadata("necromancer_owner")) {
                String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
                Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
                if (owner != null && owner.isOnline()) {
                    double currentMax = owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.min(20.0, currentMax + 2));
                    owner.sendMessage(org.bukkit.ChatColor.GREEN + "Você recuperou 1 coração ao perder sua alma invocada.");
                    List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
                    if (list != null) {
                        list.remove(entity);
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em onEntityDeath: " + e.getMessage());
        }
    }

    public void addSummonedSoul(Player owner, LivingEntity soul) {
        try {
            summonedSouls.computeIfAbsent(owner.getUniqueId(), k -> new java.util.ArrayList<>()).add(soul);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em addSummonedSoul: " + e.getMessage());
        }
    }

    public java.util.List<LivingEntity> getSummonedSouls(Player owner) {
        try {
            return summonedSouls.getOrDefault(owner.getUniqueId(), java.util.Collections.emptyList());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em getSummonedSouls: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public void removeAllSouls(Player owner) {
        try {
            List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
            if (list != null) {
                for (LivingEntity soul : new java.util.ArrayList<>(list)) {
                    if (!soul.isDead()) soul.remove();
                }
                list.clear();
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em removeAllSouls: " + e.getMessage());
        }
    }

    public EntityDeathEvent getLastMobDeath(Player player) {
        try {
            return lastMobDeath.get(player.getUniqueId());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em getLastMobDeath: " + e.getMessage());
            return null;
        }
    }

    public void clearLastMobDeath(Player player) {
        try {
            lastMobDeath.remove(player.getUniqueId());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SoulLedger] Erro em clearLastMobDeath: " + e.getMessage());
        }
    }
}
