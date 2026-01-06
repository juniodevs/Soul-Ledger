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

public class PactManager {

    private final SoulLedgerPlugin plugin;
    private final Map<String, Pact> loadedPacts = new HashMap<>();
    private final Map<UUID, List<String>> activePacts = new HashMap<>();
    private final NamespacedKey pdcKey;

    public PactManager(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pdcKey = new NamespacedKey(plugin, "active_pacts");
        loadPactsFromConfig();
    }

    public void loadPactsFromConfig() {
        loadedPacts.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pacts");
        if (section == null) return;

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

            Pact pact = new Pact(key, displayName, cost, effects, permission, icon);
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

        healthAttr.setBaseValue(currentBase - pact.getHealthCost());
        
        for (PotionEffect effect : pact.getEffects()) {
            player.addPotionEffect(effect);
        }

        currentPacts.add(pactId);
        activePacts.put(player.getUniqueId(), currentPacts);
        saveToPDC(player, currentPacts);

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);

        player.sendMessage(plugin.getFormattedMessage("pact_sealed").replace("%pact%", pact.getDisplayName()));

        return true;
    }

    public void revokeAllPacts(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(20.0);
        }

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

    private List<String> getPlayerPacts(Player player) {
        return activePacts.getOrDefault(player.getUniqueId(), new ArrayList<>());
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
}
