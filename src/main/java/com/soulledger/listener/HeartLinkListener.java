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
