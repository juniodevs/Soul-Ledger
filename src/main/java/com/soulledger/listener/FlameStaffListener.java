package com.soulledger.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class FlameStaffListener implements Listener {
    private final Set<java.util.UUID> flameActive = new HashSet<>();
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
        if (!flameActive.contains(player.getUniqueId())) return;
        if (event.getFrom().getY() == event.getTo().getY()) {
            Location loc = player.getLocation().subtract(0, 0, 0);
            Block block = loc.getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.FIRE);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (isFlameStaff(item)) {
            java.util.UUID uuid = player.getUniqueId();
            if (flameActive.contains(uuid)) {
                flameActive.remove(uuid);
                player.sendMessage("§6Flame Staff deactivated!");
            } else {
                flameActive.add(uuid);
                player.sendMessage("§6Flame Staff activated!");
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        flameActive.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (isFlameStaff(item)) {
                player.getInventory().remove(item);
            }
        }
    }
}
