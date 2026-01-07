package com.soulledger.manager;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.model.Pact;
import com.soulledger.model.PredatorBuff;
import com.soulledger.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PactManager {

    private final SoulLedgerPlugin plugin;
    private final Map<String, Pact> loadedPacts = new HashMap<>();
    private final Map<UUID, List<String>> activePacts = new HashMap<>();
    private final Map<UUID, Map<EntityType, Integer>> predatorKillCounters = new HashMap<>();
    private final NamespacedKey pdcKey;
    private Material globalCostItem;
    private final Random random = new Random();

    public PactManager(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pdcKey = new NamespacedKey(plugin, "active_pacts");
        loadPactsFromConfig();
        plugin.getServer().getOnlinePlayers().forEach(this::restorePacts);
    }

    public void loadPactsFromConfig() {
        loadedPacts.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("pacts");
        if (section == null) return;

        String globalCostItemName = plugin.getConfig().getString("settings.cost_item", "DIAMOND");
        this.globalCostItem = parseMaterial(globalCostItemName);

        for (String key : section.getKeys(false)) {
            ConfigurationSection pactSection = section.getConfigurationSection(key);
            if (pactSection == null) continue;

            Pact pact = parsePact(key, pactSection);
            loadedPacts.put(key, pact);
        }
        plugin.getLogger().info("Loaded " + loadedPacts.size() + " pacts.");
    }

    private Material parseMaterial(String name) {
        if (name == null || name.equalsIgnoreCase("OFF")) return null;
        return Material.getMaterial(name.toUpperCase());
    }

    private Pact parsePact(String id, ConfigurationSection section) {
        String displayName = section.getString("display_name", id);
        double cost = section.getDouble("cost_health", 0.0);
        String permission = section.getString("permission", "soulledger.use");
        Material icon = parseMaterial(section.getString("icon", "NETHER_STAR"));
        Material costItem = parseMaterial(section.getString("cost_item"));

        Map<Attribute, Double> attributes = new HashMap<>();
        ConfigurationSection attrSection = section.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String attrKey : attrSection.getKeys(false)) {
                try {
                    attributes.put(Attribute.valueOf(attrKey.toUpperCase()), attrSection.getDouble(attrKey));
                } catch (Exception ignored) {}
            }
        }

        List<PotionEffect> effects = section.getStringList("effects").stream()
                .map(this::parsePotionEffect)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (id.equalsIgnoreCase("predator") || id.equalsIgnoreCase("predador")) {
            ConfigurationSection buffsSection = section.getConfigurationSection("mob_buffs");
            if (buffsSection == null) {
                buffsSection = loadExternalMobBuffs(id);
            }
            Map<EntityType, PredatorBuff> buffs = parsePredatorBuffs(buffsSection);
            return new Pact(id, displayName, cost, costItem, attributes, effects, permission, icon, buffs);
        }

        return new Pact(id, displayName, cost, costItem, attributes, effects, permission, icon);
    }

    private PotionEffect parsePotionEffect(String raw) {
        try {
            String[] parts = raw.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return type != null ? new PotionEffect(type, Integer.MAX_VALUE, amplifier) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<EntityType, PredatorBuff> parsePredatorBuffs(ConfigurationSection section) {
        Map<EntityType, PredatorBuff> buffs = new HashMap<>();
        if (section == null) return buffs;

        for (String key : section.getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key.toUpperCase());
                ConfigurationSection mobSection = section.getConfigurationSection(key);

                List<PotionEffect> normalBuffs = new ArrayList<>();
                List<PotionEffect> playerKillBuffs = new ArrayList<>();

                if (mobSection != null) {
                    // Formato novo: lista de buffs em 'buffs' e buffs por matar player em 'player_kill_buffs'
                    normalBuffs.addAll(parsePotionEffectList(mobSection, "buffs"));
                    playerKillBuffs.addAll(parsePotionEffectList(mobSection, "player_kill_buffs"));

                    // Compatibilidade: chaves simples 'effect', 'amplifier', 'duration'
                    if (normalBuffs.isEmpty() && mobSection.contains("effect")) {
                        String effectName = mobSection.getString("effect", "SPEED");
                        int amp = mobSection.getInt("amplifier", 0);
                        int dur = mobSection.getInt("duration", 30);
                        PotionEffectType typeEff = PotionEffectType.getByName(effectName.toUpperCase());
                        if (typeEff != null) normalBuffs.add(new PotionEffect(typeEff, dur * 20, amp));
                    }
                } else {
                    // Suporte para formato simplificado: 'MOB: EFFECT:AMP:DUR'
                    String value = section.getString(key);
                    if (value != null && value.contains(":")) {
                        String[] parts = value.split(":");
                        String effectName = parts[0];
                        int amp = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                        int dur = parts.length > 2 ? Integer.parseInt(parts[2]) : 30;
                        PotionEffectType typeEff = PotionEffectType.getByName(effectName.toUpperCase());
                        if (typeEff != null) normalBuffs.add(new PotionEffect(typeEff, dur * 20, amp));
                    } else {
                        continue;
                    }
                }

                if (!normalBuffs.isEmpty()) {
                    buffs.put(type, new PredatorBuff(type, normalBuffs, playerKillBuffs));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing predator buff for: " + key);
            }
        }
        return buffs;
    }

    private List<PotionEffect> parsePotionEffectList(ConfigurationSection mobSection, String key) {
        List<PotionEffect> list = new ArrayList<>();
        if (mobSection == null || !mobSection.isList(key)) return list;

        List<?> raw = mobSection.getList(key);
        for (Object obj : raw) {
            try {
                if (obj instanceof String) {
                    String s = (String) obj;
                    String[] parts = s.split(":");
                    String effectName = parts[0];
                    int amp = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    int dur = parts.length > 2 ? Integer.parseInt(parts[2]) : 30;
                    PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                    if (type != null) list.add(new PotionEffect(type, dur * 20, amp));
                } else if (obj instanceof java.util.Map) {
                    java.util.Map map = (java.util.Map) obj;
                    String effectName = map.get("effect") != null ? map.get("effect").toString() : null;
                    if (effectName == null) continue;
                    int amp = map.get("amplifier") != null ? Integer.parseInt(map.get("amplifier").toString()) : 0;
                    int dur = map.get("duration") != null ? Integer.parseInt(map.get("duration").toString()) : 30;
                    PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                    if (type != null) list.add(new PotionEffect(type, dur * 20, amp));
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private ConfigurationSection loadExternalMobBuffs(String pactId) {
        try {
            File file = new File(plugin.getDataFolder(), "mob_buffs.yml");
            FileConfiguration fconf = null;
            if (file.exists()) {
                fconf = YamlConfiguration.loadConfiguration(file);
            } else {
                InputStream is = plugin.getResource("mob_buffs.yml");
                if (is != null) {
                    fconf = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                }
            }

            if (fconf != null && fconf.isConfigurationSection(pactId + ".mob_buffs")) {
                return fconf.getConfigurationSection(pactId + ".mob_buffs");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load external mob_buffs.yml: " + e.getMessage());
        }
        return null;
    }

    public boolean sealPact(Player player, String pactId) {
        Pact pact = loadedPacts.get(pactId);
        if (pact == null) return false;

        if (hasPact(player, pactId)) {
            player.sendMessage(plugin.getFormattedMessage("already_has"));
            return false;
        }

        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr == null || healthAttr.getBaseValue() <= pact.getHealthCost() + 1.0) {
            player.sendMessage(plugin.getFormattedMessage("insufficient_health"));
            return false;
        }

        if (!consumeCostItem(player, pact)) return false;

        applyPactEffects(player, pact);
        savePact(player, pactId);
        playSealEffects(player);
        
        player.sendMessage(plugin.getFormattedMessage("pact_sealed").replace("%pact%", pact.getDisplayName()));
        giveSpecialItems(player, pactId);

        return true;
    }

    private boolean consumeCostItem(Player player, Pact pact) {
        Material cost = pact.getCostItem() != null ? pact.getCostItem() : globalCostItem;
        if (cost == null) return true;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != cost || hand.getAmount() <= 0) {
            player.sendMessage(plugin.getFormattedMessage("must_hold_item").replace("%item%", cost.name().toLowerCase()));
            return false;
        }

        hand.setAmount(hand.getAmount() - 1);
        return true;
    }

    private void applyPactEffects(Player player, Pact pact) {
        if (pact.getHealthCost() > 0) {
            AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) health.setBaseValue(health.getBaseValue() - pact.getHealthCost());
        }

        pact.getAttributeModifiers().forEach((attr, val) -> {
            AttributeInstance instance = player.getAttribute(attr);
            if (instance != null) instance.setBaseValue(instance.getBaseValue() + val);
        });

        pact.getEffects().forEach(player::addPotionEffect);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
    }

    private void playSealEffects(Player player) {
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
    }

    private void giveSpecialItems(Player player, String pactId) {
        if (pactId.equalsIgnoreCase("necromancy") || pactId.equalsIgnoreCase("necromancia")) {
            giveItem(player, Material.BOOK, "soul_staff_name", "soul_staff_lore", true);
        } else if (pactId.equalsIgnoreCase("flame")) {
            giveItem(player, Material.BLAZE_ROD, "Flame Staff", null, false);
        }
    }

    private void giveItem(Player player, Material mat, String nameKey, String loreKey, boolean isConfigKey) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = isConfigKey ? plugin.getConfig().getString("messages." + getLang() + "." + nameKey, nameKey) : nameKey;
        meta.setDisplayName(ColorUtil.color(name));
        
        if (loreKey != null) {
            List<String> lore = plugin.getConfig().getStringList("messages." + getLang() + "." + loreKey);
            meta.setLore(ColorUtil.color(lore));
        }

        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    private String getLang() {
        return plugin.getConfig().getString("settings.language", "en");
    }

    public void restorePacts(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        String data = pdc.get(pdcKey, PersistentDataType.STRING);
        if (data == null) return;

        List<String> pacts = Arrays.asList(data.split(","));
        pacts.forEach(id -> {
            Pact pact = loadedPacts.get(id);
            if (pact != null) pact.getEffects().forEach(player::addPotionEffect);
        });
        activePacts.put(player.getUniqueId(), new ArrayList<>(pacts));
    }

    private void savePact(Player player, String pactId) {
        List<String> pacts = activePacts.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        pacts.add(pactId);
        player.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, String.join(",", pacts));
    }

    public boolean hasPact(Player player, String pactId) {
        return activePacts.getOrDefault(player.getUniqueId(), Collections.emptyList()).contains(pactId);
    }

    public void applyPredatorBuff(Player player, LivingEntity victim) {
        if (!hasPact(player, "predator") && !hasPact(player, "predador")) return;

        Pact predator = loadedPacts.get("predator");
        if (predator == null) predator = loadedPacts.get("predador");
        if (predator == null || predator.getPredatorBuffs() == null) return;

        PredatorBuff buff = predator.getPredatorBuffs().get(victim.getType());
        if (buff != null) {
            List<String> applied = new ArrayList<>();

            buff.getBuffs().forEach(pe -> {
                player.addPotionEffect(pe);
                applied.add(pe.getType().getName().toLowerCase().replace("_", " ") + " " + (pe.getAmplifier() + 1) + " for " + (pe.getDuration() / 20) + "s");
            });

            if (victim instanceof Player) {
                buff.getPlayerKillBuffs().forEach(pe -> {
                    player.addPotionEffect(pe);
                    applied.add(pe.getType().getName().toLowerCase().replace("_", " ") + " " + (pe.getAmplifier() + 1) + " for " + (pe.getDuration() / 20) + "s");
                });
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.5f);

            String effectsStr = String.join(", ", applied);
            String msg = plugin.getFormattedMessage("predator_buff_message")
                    .replace("%mob%", victim.getType().name().toLowerCase().replace("_", " "))
                    .replace("%effects%", effectsStr);
            player.sendMessage(msg);
        }
    }

    public Map<String, Pact> getLoadedPacts() { return loadedPacts; }

    public Material getGlobalCostItem() { return globalCostItem; }

    public List<String> getPlayerPacts(Player player) {
        return activePacts.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void revokeAllPacts(Player player) {
        activePacts.remove(player.getUniqueId());
        player.getPersistentDataContainer().remove(pdcKey);
        forceResetAttributes(player);
    }

    public void forceResetAttributes(Player player) {
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) health.setBaseValue(health.getDefaultValue());
        
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public boolean hasNecromancyPact(Player player) {
        return hasPact(player, "necromancy") || hasPact(player, "necromancia");
    }
}
