package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.*;

public class MobListener implements Listener {

    private final SoulLedgerPlugin plugin;
    private final Map<UUID, List<LivingEntity>> summonedSouls = new HashMap<>();
    private final Map<UUID, EntityDeathEvent> lastMobDeath = new HashMap<>();

    public MobListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        try {
            LivingEntity entity = event.getEntity();
            
            if (entity.getKiller() != null) {
                Player killer = entity.getKiller();
                if (plugin.getPactManager().hasNecromancyPact(killer)) {
                    lastMobDeath.put(killer.getUniqueId(), event);
                }
            }

            if (entity.hasMetadata("necromancer_owner")) {
                handleSummonedSoulDeath(entity, event);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[SoulLedger] Erro em onEntityDeath: " + e.getMessage());
        }
    }

    private void handleSummonedSoulDeath(LivingEntity entity, EntityDeathEvent event) {
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        entity.getWorld().spawnParticle(org.bukkit.Particle.SOUL, entity.getLocation(), 80, 1.2, 2, 1.2, 0.25);
        entity.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, entity.getLocation(), 40, 1, 1.5, 1, 0.15);
        entity.getWorld().playSound(entity.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.7f);

        String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
        Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
        if (owner != null && owner.isOnline()) {
            recoverHealth(owner);
            List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
            if (list != null) {
                list.remove(entity);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof org.bukkit.entity.Creeper && entity.hasMetadata("necromancer_owner")) {
            String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
            Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
            if (owner != null && owner.isOnline()) {
                recoverHealth(owner);
                owner.sendMessage(ChatColor.GREEN + plugin.getFormattedMessage("soul_recovered_creeper"));
                List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
                if (list != null) {
                    list.remove(entity);
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        Entity entity = event.getEntity();
        if (entity.hasMetadata("necromancer_owner")) {
            String ownerId = entity.getMetadata("necromancer_owner").get(0).asString();
            if (ownerId.equals(event.getTarget().getUniqueId().toString())) {
                event.setCancelled(true);
            }
        }
    }

    private void recoverHealth(Player player) {
        double currentMax = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.min(20.0, currentMax + 2));
        player.sendMessage(ChatColor.GREEN + plugin.getFormattedMessage("soul_recovered"));
    }

    public void addSummonedSoul(Player owner, LivingEntity soul) {
        summonedSouls.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>()).add(soul);
    }

    public List<LivingEntity> getSummonedSouls(Player owner) {
        return summonedSouls.getOrDefault(owner.getUniqueId(), Collections.emptyList());
    }

    public void removeAllSouls(Player owner) {
        List<LivingEntity> list = summonedSouls.get(owner.getUniqueId());
        if (list != null) {
            for (LivingEntity soul : new ArrayList<>(list)) {
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
