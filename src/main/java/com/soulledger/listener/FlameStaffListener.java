package com.soulledger.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class FlameStaffListener implements Listener {
    private boolean isFlameStaff(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains("Flame Staff");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isFlameStaff(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand() != null && isFlameStaff(player.getInventory().getItemInMainHand())) {
            Location loc = player.getLocation().subtract(0, 1, 0);
            Block block = loc.getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.FIRE);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        for (ItemStack item : player.getInventory().getContents()) {
            if (isFlameStaff(item)) {
                player.getInventory().remove(item);
            }
        }
    }
}
