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
    private final com.soulledger.gui.NecromancerSummonGui necromancerSummonGui;

    public NecromancerStickUseListener(PactManager pactManager, NecromancyListener necromancyListener) {
        this.pactManager = pactManager;
        this.necromancyListener = necromancyListener;
        this.necromancerSummonGui = new com.soulledger.gui.NecromancerSummonGui(org.bukkit.Bukkit.getPluginManager().getPlugin("SoulLedger"), necromancyListener);
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
        // Abrir GUI de invocação
        necromancerSummonGui.open(player);
        event.setCancelled(true);
    }
}