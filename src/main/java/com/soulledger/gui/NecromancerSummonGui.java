package com.soulledger.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.util.*;

public class NecromancerSummonGui implements Listener {
    private final Plugin plugin;
    private final com.soulledger.listener.NecromancyListener necromancyListener;
    private final String GUI_TITLE = ChatColor.DARK_PURPLE + "Invocar Alma";
    private final List<EntityType> summonableTypes = Arrays.asList(
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.ENDERMAN
    );

    public NecromancerSummonGui(Plugin plugin, com.soulledger.listener.NecromancyListener necromancyListener) {
        this.plugin = plugin;
        this.necromancyListener = necromancyListener;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);
        int slot = 0;
        for (EntityType type : summonableTypes) {
            ItemStack head;
            switch (type) {
                case ZOMBIE:
                    head = new ItemStack(Material.ZOMBIE_HEAD); break;
                case SKELETON:
                    head = new ItemStack(Material.SKELETON_SKULL); break;
                case CREEPER:
                    head = new ItemStack(Material.CREEPER_HEAD); break;
                case ENDERMAN:
                    head = new ItemStack(Material.ENDER_PEARL); break;
                default:
                    continue;
            }
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Invocar " + type.name().toLowerCase());
                meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "summon_type"),
                    PersistentDataType.STRING,
                    type.name()
                );
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (!meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "summon_type"), PersistentDataType.STRING)) return;

        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        if (maxHealth <= 2.0) {
            if (plugin != null && plugin instanceof com.soulledger.SoulLedgerPlugin) {
                player.sendMessage(((com.soulledger.SoulLedgerPlugin)plugin).getFormattedMessage("necromancer_low_health"));
            } else {
                player.sendMessage(ChatColor.RED + "You cannot summon souls with only 1 heart of life!");
            }
            player.closeInventory();
            return;
        }
        String typeName = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "summon_type"), PersistentDataType.STRING);
        EntityType type = null;
        try {
            type = EntityType.valueOf(typeName);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Erro ao identificar tipo de mob: " + typeName);
            player.closeInventory();
            return;
        }
        player.closeInventory();
        try {
            org.bukkit.entity.LivingEntity summoned = (org.bukkit.entity.LivingEntity) player.getWorld().spawnEntity(player.getLocation(), type);
            summoned.setCustomName(ChatColor.DARK_PURPLE + "Alma Invocada por " + player.getName());
            summoned.setCustomNameVisible(true);
            summoned.setRemoveWhenFarAway(true);
            summoned.setMetadata("necromancer_owner", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getUniqueId().toString()));
            necromancyListener.addSummonedSoul(player, summoned);
            player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1, 0.7f);
            double currentMax = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(2, currentMax - 2));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!summoned.isDead()) summoned.setHealth(0.0);
            }, 20 * 30);
            if (plugin != null && plugin instanceof com.soulledger.SoulLedgerPlugin) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + ((com.soulledger.SoulLedgerPlugin)plugin).getFormattedMessage("soul_summoned"));
            } else {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Você invocou uma alma! Sua vida máxima foi reduzida em 1 coração. Quando a alma morrer, você recupera.");
            }
        } catch (Exception ex) {
            if (plugin != null && plugin instanceof com.soulledger.SoulLedgerPlugin) {
                player.sendMessage(ChatColor.RED + ((com.soulledger.SoulLedgerPlugin)plugin).getFormattedMessage("summon_error") + ex.getMessage());
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao invocar mob: " + ex.getMessage());
            }
        }
    }
}
