package com.soulledger.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NecromancerStickListener implements Listener {
    private boolean isNecroStick(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String lang = org.bukkit.Bukkit.getPluginManager().getPlugin("SoulLedger").getConfig().getString("settings.language", "en");
        String name = org.bukkit.Bukkit.getPluginManager().getPlugin("SoulLedger").getConfig().getString("messages." + lang + ".soul_staff_name", "Cajado das Almas");
        return meta.hasDisplayName() && meta.getDisplayName().contains(name);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isNecroStick(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (ItemStack item : event.getNewItems().values()) {
            if (isNecroStick(item)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isNecroStick(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove(); // destrói o item ao invés de droppar
            event.setCancelled(true);
        }
    }
}