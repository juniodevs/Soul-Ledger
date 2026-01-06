package com.soulledger.manager;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.model.Pact;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;

public class PactManager {

    public boolean hasNecromancyPact(Player player) {
        List<String> pacts = getPlayerPacts(player);
        return pacts.stream().anyMatch(p -> p.equalsIgnoreCase("necromancia") || p.equalsIgnoreCase("necromancy"));
    }

    private final SoulLedgerPlugin plugin;
    private final Map<String, Pact> loadedPacts = new HashMap<>();
    private final Map<UUID, List<String>> activePacts = new HashMap<>();
    private final NamespacedKey pdcKey;
    private Material globalCostItem;

    public PactManager(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pdcKey = new NamespacedKey(plugin, "active_pacts");
        loadPactsFromConfig();
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            restorePacts(p);
        }
    }

    public void loadPactsFromConfig() {
        loadedPacts.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pacts");
        if (section == null) return;
        String globalCostItemName = plugin.getConfig().getString("settings.cost_item", "DIAMOND");
        if (globalCostItemName != null) globalCostItemName = globalCostItemName.trim();
        if (globalCostItemName == null || globalCostItemName.isEmpty()) globalCostItemName = "DIAMOND";
        if ("OFF".equalsIgnoreCase(globalCostItemName)) {
            globalCostItem = null;
        } else {
            globalCostItem = Material.getMaterial(globalCostItemName.toUpperCase());
        }
        plugin.getLogger().info("[SoulLedger] settings.cost_item (raw) = '" + plugin.getConfig().getString("settings.cost_item") + "' -> trimmed='" + globalCostItemName + "' -> resolved globalCostItem = " + (globalCostItem == null ? "OFF" : globalCostItem.name()));

        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display_name", key);
            double cost = section.getDouble(key + ".cost_health", 0.0);
            String permission = section.getString(key + ".permission", "soulledger.use");
            String iconName = section.getString(key + ".icon", "NETHER_STAR");
            Material icon = Material.getMaterial(iconName.toUpperCase());
            if (icon == null) {
                plugin.getLogger().warning("Invalid icon material for pact " + key + ": " + iconName + ". Using NETHER_STAR.");
                icon = Material.NETHER_STAR;
            }

            String costItemName = section.getString(key + ".cost_item", null);
            if (costItemName != null) costItemName = costItemName.trim();
            Material costItem;
            if (costItemName == null || costItemName.isEmpty()) {
                // No per-pact value -> inherit global setting (which may be null/off)
                costItem = globalCostItem;
            } else if ("OFF".equalsIgnoreCase(costItemName)) {
                costItem = null;
            } else {
                costItem = Material.getMaterial(costItemName.toUpperCase());
                if (costItem == null) costItem = globalCostItem;
            }

            Map<Attribute, Double> attributeModifiers = new HashMap<>();
            if (section.isConfigurationSection(key + ".attributes")) {
                ConfigurationSection attrSection = section.getConfigurationSection(key + ".attributes");
                for (String attrKey : attrSection.getKeys(false)) {
                    try {
                        Attribute attribute = Attribute.valueOf(attrKey.toUpperCase());
                        double value = attrSection.getDouble(attrKey);
                        attributeModifiers.put(attribute, value);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid attribute " + attrKey + " in pact " + key);
                    }
                }
            }

            List<String> rawEffects = section.getStringList(key + ".effects");
            List<PotionEffect> effects = new ArrayList<>();

            for (String raw : rawEffects) {
                try {
                    String[] parts = raw.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    if (type != null) {
                        effects.add(new PotionEffect(type, Integer.MAX_VALUE, amplifier));
                    } else {
                        plugin.getLogger().warning("Invalid potion type in config: " + parts[0]);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error parsing effect: " + raw);
                }
            }

            Pact pact = new Pact(key, displayName, cost, costItem, attributeModifiers, effects, permission, icon);
            loadedPacts.put(key, pact);
        }
        plugin.getLogger().info("Loaded " + loadedPacts.size() + " pacts.");
    }

    public boolean sealPact(Player player, String pactId) {
        Pact pact = loadedPacts.get(pactId);
        if (pact == null) return false;

        List<String> currentPacts = getPlayerPacts(player);
        if (currentPacts.contains(pactId)) {
            player.sendMessage(plugin.getFormattedMessage("already_has"));
            return false;
        }

        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr == null) return false;

        double currentBase = healthAttr.getBaseValue();
        if (currentBase <= pact.getHealthCost() + 1.0) {
            player.sendMessage(plugin.getFormattedMessage("insufficient_health"));
            return false;
        }

        Material effectiveCostItem = pact.getCostItem() != null ? pact.getCostItem() : globalCostItem;
        if (effectiveCostItem != null) {
            org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() != effectiveCostItem || hand.getAmount() <= 0) {
                String itemName = effectiveCostItem.name().replace("_", " ").toLowerCase();
                player.sendMessage(plugin.getFormattedMessage("must_hold_item").replace("%item%", itemName));
                return false;
            }
            hand.setAmount(hand.getAmount() - 1);
        }

        if (pact.getHealthCost() != 0) {
            healthAttr.setBaseValue(currentBase - pact.getHealthCost());
        }

        for (Map.Entry<Attribute, Double> entry : pact.getAttributeModifiers().entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null) {
                instance.setBaseValue(instance.getBaseValue() + entry.getValue());
            }
        }
        
        for (PotionEffect effect : pact.getEffects()) {
            player.addPotionEffect(effect);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 10, 0.5, 1.2, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 8, 0.3, 0.8, 0.3, 0.03);

        currentPacts.add(pactId);
        activePacts.put(player.getUniqueId(), currentPacts);
        saveToPDC(player, currentPacts);

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);

        player.sendMessage(plugin.getFormattedMessage("pact_sealed").replace("%pact%", pact.getDisplayName()));

        if (pactId.equalsIgnoreCase("necromancy") || pactId.equalsIgnoreCase("necromancia")) {
            org.bukkit.inventory.ItemStack stick = new org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK);
            org.bukkit.inventory.meta.ItemMeta meta = stick.getItemMeta();
            String lang = plugin.getConfig().getString("settings.language", "en");
            String name = plugin.getConfig().getString("messages." + lang + ".soul_staff_name", "Cajado das Almas");
            java.util.List<String> lore = plugin.getConfig().getStringList("messages." + lang + ".soul_staff_lore");
            meta.setDisplayName(org.bukkit.ChatColor.DARK_PURPLE + name);
            meta.setLore(lore);
            meta.setUnbreakable(true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.VANISHING_CURSE, 1, true);
            stick.setItemMeta(meta);
            stick.setAmount(1);
            player.getInventory().addItem(stick);
        }
        return true;
    }

    public void revokeAllPacts(Player player) {
                org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() == org.bukkit.Material.STICK && item.hasItemMeta()) {
                        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                        String lang = plugin.getConfig().getString("settings.language", "en");
                        String name = plugin.getConfig().getString("messages." + lang + ".soul_staff_name", "Cajado das Almas");
                        if (meta.hasDisplayName() && meta.getDisplayName().contains(name)) {
                            inv.remove(item);
                        }
                    }
                }
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(healthAttr.getDefaultValue());
        }

        for (Attribute attr : Attribute.values()) {
            try {
                if (attr == Attribute.GENERIC_MAX_HEALTH) continue;
                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    instance.setBaseValue(instance.getDefaultValue());
                    instance.getModifiers().forEach(instance::removeModifier);
                }
            } catch (Exception ignored) {}
        }

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        for (PotionEffect effect : player.getActivePotionEffects()) {
             player.removePotionEffect(effect.getType());
        }

        activePacts.remove(player.getUniqueId());
        player.getPersistentDataContainer().remove(pdcKey);
    }

    public void restorePacts(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (pdc.has(pdcKey, PersistentDataType.STRING)) {
            String data = pdc.get(pdcKey, PersistentDataType.STRING);
            if (data == null || data.isEmpty()) return;

            String[] ids = data.split(",");
            List<String> list = new ArrayList<>();
            
            for (String id : ids) {
                Pact pact = loadedPacts.get(id);
                if (pact != null) {
                    list.add(id);
                     for (PotionEffect effect : pact.getEffects()) {
                        plugin.getLogger().info("Restoring effect " + effect.getType() + " for " + player.getName());
                        player.addPotionEffect(effect);
                    }
                }
            }
            activePacts.put(player.getUniqueId(), list);
        }
    }

    public List<String> getPlayerPacts(Player player) {
        return activePacts.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public void forceResetAttributes(Player player) {
        for (Attribute attr : Attribute.values()) {
            try {
                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    instance.setBaseValue(instance.getDefaultValue());
                    instance.getModifiers().forEach(instance::removeModifier);
                }
            } catch (Exception ignored) {}
        }
        
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    private void saveToPDC(Player player, List<String> pacts) {
        if (pacts.isEmpty()) {
            player.getPersistentDataContainer().remove(pdcKey);
        } else {
            String data = String.join(",", pacts);
            player.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, data);
        }
    }

    public Map<String, Pact> getLoadedPacts() {
        return loadedPacts;
    }

    public Material getGlobalCostItem() {
        return globalCostItem;
    }
}
