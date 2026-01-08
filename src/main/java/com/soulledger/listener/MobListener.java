package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;

public class MobListener implements Listener {

    private final SoulLedgerPlugin plugin;
    private final Map<UUID, List<LivingEntity>> capturedSouls = new HashMap<>();

    public MobListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            plugin.getPactManager().applyPredatorBuff(killer, victim);

            if (plugin.getPactManager().hasNecromancyPact(killer)) {
                captureSoul(killer, victim);
            }
        }

        try {
            if (victim.hasMetadata("necromancer_owner") || victim.hasMetadata("familiar_owner")) {
                String ownerId = null;
                if (victim.hasMetadata("necromancer_owner")) {
                    ownerId = victim.getMetadata("necromancer_owner").get(0).value().toString();
                } else {
                    ownerId = victim.getMetadata("familiar_owner").get(0).value().toString();
                }
                java.util.UUID uuid = java.util.UUID.fromString(ownerId);
                Player owner = plugin.getServer().getPlayer(uuid);
                if (owner != null) {
                    List<LivingEntity> souls = summonedSouls.get(owner.getUniqueId());
                    if (souls != null) souls.remove(victim);

                    if (victim.hasMetadata("familiar_owner")) {
                        victim.getWorld().spawnParticle(org.bukkit.Particle.HEART, victim.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                        victim.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, victim.getLocation().add(0, 0.5, 0), 8, 0.2, 0.2, 0.2, 0.01);
                        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_WOLF_DEATH, 1f, 1f);
                    } else {
                        victim.getWorld().spawnParticle(org.bukkit.Particle.SOUL, victim.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
                        victim.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, victim.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
                        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1f);
                    }

                    org.bukkit.attribute.AttributeInstance healthAttr = owner.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                    if (healthAttr != null) {
                        healthAttr.setBaseValue(healthAttr.getBaseValue() + 2.0);
                    }

                    org.bukkit.event.entity.EntityDamageEvent last = victim.getLastDamageCause();
                    if (last != null && last.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && victim.getType() == org.bukkit.entity.EntityType.CREEPER) {
                        owner.sendMessage(plugin.getFormattedMessage("soul_recovered_creeper"));
                    } else {
                        owner.sendMessage(plugin.getFormattedMessage("soul_recovered"));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        org.bukkit.entity.Entity entity = event.getEntity();
        Player target = (Player) event.getTarget();
        try {
            String ownerId = null;
            if (entity.hasMetadata("necromancer_owner")) {
                ownerId = entity.getMetadata("necromancer_owner").get(0).value().toString();
            } else if (entity.hasMetadata("familiar_owner")) {
                ownerId = entity.getMetadata("familiar_owner").get(0).value().toString();
            }
            if (ownerId != null && ownerId.equals(target.getUniqueId().toString())) {
                event.setCancelled(true);
                event.setTarget(null);
            }
        } catch (Exception ignored) {}
    }

    private void captureSoul(Player player, LivingEntity entity) {
        List<LivingEntity> souls = capturedSouls.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        if (souls.size() < 10) {
            souls.add(entity);
        }
    }

    public List<LivingEntity> getCapturedSouls(Player player) {
        return capturedSouls.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    private final Map<UUID, List<LivingEntity>> summonedSouls = new HashMap<>();

    public void addSummonedSoul(Player player, LivingEntity entity) {
        summonedSouls.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(entity);
    }

    public List<LivingEntity> getSummonedSouls(Player player) {
        return summonedSouls.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void removeAllSouls(Player player) {
        List<LivingEntity> souls = summonedSouls.remove(player.getUniqueId());
        if (souls != null) {
            souls.forEach(LivingEntity::remove);
        }
        capturedSouls.remove(player.getUniqueId());
    }
}
