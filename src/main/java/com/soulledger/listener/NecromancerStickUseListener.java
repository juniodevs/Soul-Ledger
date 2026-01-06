package com.soulledger.listener;

import com.soulledger.manager.PactManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

public class NecromancerStickUseListener implements Listener {
    private final PactManager pactManager;
    private final NecromancyListener necromancyListener;

    public NecromancerStickUseListener(PactManager pactManager, NecromancyListener necromancyListener) {
        this.pactManager = pactManager;
        this.necromancyListener = necromancyListener;
    }

    private boolean isNecroStick(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains("Cajado das Almas");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isNecroStick(item)) return;
        if (!pactManager.hasNecromancyPact(player)) return;
        // Invocar alma
        var lastDeath = necromancyListener.getLastMobDeath(player);
        if (lastDeath == null) {
            player.sendMessage(ChatColor.RED + "Você ainda não matou nenhum mob recentemente.");
            return;
        }
        LivingEntity mob = (LivingEntity) lastDeath.getEntity();
        EntityType type = mob.getType();
        LivingEntity summoned = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), type);
        summoned.setCustomName(ChatColor.DARK_PURPLE + "Alma Invocada por " + player.getName());
        summoned.setCustomNameVisible(true);
        summoned.setRemoveWhenFarAway(true);
        summoned.setMetadata("necromancer_owner", new org.bukkit.metadata.FixedMetadataValue(Bukkit.getPluginManager().getPlugin("SoulLedger"), player.getUniqueId().toString()));
        player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1, 0.7f);
        double currentMax = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(2, currentMax - 2));
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("SoulLedger"), () -> {
            if (!summoned.isDead()) summoned.remove();
        }, 20 * 30);
        necromancyListener.clearLastMobDeath(player);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Você invocou uma alma! Sua vida máxima foi reduzida em 1 coração. Quando a alma morrer, você recupera.");
        event.setCancelled(true);
    }
}