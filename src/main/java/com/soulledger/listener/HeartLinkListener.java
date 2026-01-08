package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.manager.PactManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class HeartLinkListener implements Listener {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;

    public HeartLinkListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!pactManager.isHeartLinked(player)) return;
        if (event.getFinalDamage() <= 0) return;

        Player partner = pactManager.getHeartPartner(player);
        if (partner == null || !partner.isOnline() || partner.isDead()) return;

        if (pactManager.isIgnoringDamage(partner.getUniqueId())) return;
        pactManager.markIgnoreDamage(partner.getUniqueId());
        try {
            double dmg = event.getFinalDamage();
            partner.damage(dmg);

            try {
                if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                    org.bukkit.event.entity.EntityDamageByEntityEvent edbe = (org.bukkit.event.entity.EntityDamageByEntityEvent) event;
                    org.bukkit.entity.Entity damager = edbe.getDamager();
                    org.bukkit.util.Vector dir = null;

                    if (damager != null) {
                        if (damager instanceof org.bukkit.entity.Projectile) {
                            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) damager;
                            Object shooter = proj.getShooter();
                            if (shooter instanceof org.bukkit.entity.Entity) {
                                dir = player.getLocation().toVector().subtract(((org.bukkit.entity.Entity) shooter).getLocation().toVector()).normalize();
                            }
                        } else if (damager instanceof org.bukkit.entity.Entity) {
                            dir = player.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize();
                        }
                    }

                    if (dir != null) {
                        double strength = 0.4;
                        strength = Math.max(0.2, Math.min(1.0, dmg / 6.0));
                        partner.setVelocity(dir.multiply(strength));
                    }
                }
            } catch (Exception ignoredKnockback) {
            }

        } catch (Exception ignored) {
        } finally {
            pactManager.unmarkIgnoreDamage(partner.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!pactManager.isHeartLinked(player)) return;
        double amount = event.getAmount();
        if (amount <= 0) return;

        Player partner = pactManager.getHeartPartner(player);
        if (partner == null || !partner.isOnline() || partner.isDead()) return;

        if (pactManager.isIgnoringHeal(partner.getUniqueId())) return;
        pactManager.markIgnoreHeal(partner.getUniqueId());
        try {
            double max = partner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double newHealth = Math.min(partner.getHealth() + amount, max);
            partner.setHealth(newHealth);
        } catch (Exception ignored) {
        } finally {
            pactManager.unmarkIgnoreHeal(partner.getUniqueId());
        }
    }
}
