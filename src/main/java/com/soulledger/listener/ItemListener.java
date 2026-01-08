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

    private boolean isFamiliarStaff(ItemStack item) {
        if (item == null || item.getType() != Material.BONE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String lang = plugin.getConfig().getString("settings.language", "en");
        String name = plugin.getConfig().getString("messages." + lang + ".familiar_staff_name", "Familiar Whistle");
        return meta.hasDisplayName() && meta.getDisplayName().contains(name);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isFlameStaff(item) || isNecroStick(item) || isFamiliarStaff(item)) {
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
        } else if (isFamiliarStaff(item)) {
            if (pactManager.hasPact(player, "familiar")) {
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                if (maxHealth <= 2.0) {
                    player.sendMessage(plugin.getFormattedMessage("familiar_low_health"));
                    event.setCancelled(true);
                    return;
                }
                event.setCancelled(true);
                try {
                        org.bukkit.entity.Wolf wolf = (org.bukkit.entity.Wolf) player.getWorld().spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.WOLF);
                    String customName = plugin.getFormattedMessage("summoned_familiar_name").replace("%player%", player.getName());
                    wolf.setCustomName(org.bukkit.ChatColor.DARK_AQUA + customName);
                    wolf.setCustomNameVisible(true);
                    wolf.setRemoveWhenFarAway(true);
                    wolf.setOwner(player);
                    wolf.setMetadata("familiar_owner", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getUniqueId().toString()));
                    plugin.getMobListener().addSummonedSoul(player, wolf);

                    wolf.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 20 * 6, 0));
                    player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, wolf.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.02);
                    player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, wolf.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
                    player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation(), 8, 0.5, 1, 0.5, 0.1);
                    player.getWorld().playSound(wolf.getLocation(), org.bukkit.Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1f);

                    double currentMax = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(2, currentMax - 2));
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!wolf.isDead()) wolf.setHealth(0.0);
                        else {
                        }
                    }, 20 * 30);
                    player.sendMessage(org.bukkit.ChatColor.AQUA + plugin.getFormattedMessage("familiar_summoned"));
                } catch (Exception ex) {
                    player.sendMessage(org.bukkit.ChatColor.RED + plugin.getFormattedMessage("summon_error") + ex.getMessage());
                }
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
        
        player.getInventory().all(Material.BOOK).values().forEach(item -> {
            if (isNecroStick(item)) {
                player.getInventory().remove(item);
            }
        });
        
        player.getInventory().all(Material.BONE).values().forEach(item -> {
            if (isFamiliarStaff(item)) {
                player.getInventory().remove(item);
            }
        });
    }
}
