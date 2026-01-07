package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.manager.PactManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ItemListener implements Listener {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;
    private final Set<UUID> flameActive = new HashSet<>();

    public ItemListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    private boolean isFlameStaff(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains("Flame Staff");
    }

    private boolean isNecroStick(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String lang = plugin.getConfig().getString("settings.language", "en");
        String name = plugin.getConfig().getString("messages." + lang + ".soul_staff_name", "Cajado das Almas");
        return meta.hasDisplayName() && meta.getDisplayName().contains(name);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isFlameStaff(item) || isNecroStick(item)) {
            event.getItemDrop().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (isFlameStaff(item)) {
            UUID uuid = player.getUniqueId();
            if (flameActive.contains(uuid)) {
                flameActive.remove(uuid);
                player.sendMessage("§6Flame Staff deactivated!");
            } else {
                flameActive.add(uuid);
                player.sendMessage("§6Flame Staff activated!");
            }
            event.setCancelled(true);
        } else if (isNecroStick(item)) {
            if (pactManager.hasNecromancyPact(player)) {
                plugin.getNecromancerSummonGui().open(player);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!flameActive.contains(player.getUniqueId())) return;
        
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
            Block block = player.getLocation().getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.FIRE);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        flameActive.remove(player.getUniqueId());
        
        player.getInventory().all(Material.BLAZE_ROD).values().forEach(item -> {
            if (isFlameStaff(item)) {
                player.getInventory().remove(item);
            }
        });
    }
}
