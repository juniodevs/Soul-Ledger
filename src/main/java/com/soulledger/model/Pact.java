package com.soulledger.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

public class Pact {
    private final String id;
    private final String displayName;
    private final double healthCost;
    private final org.bukkit.Material costItem;
    private final Map<Attribute, Double> attributeModifiers;
    private final List<PotionEffect> effects;
    private final String permission;
    private final Material icon;
    private final Map<org.bukkit.entity.EntityType, PredatorBuff> mobBuffs;

        public Pact(String id, String displayName, double healthCost, org.bukkit.Material costItem, Map<Attribute, Double> attributeModifiers, List<PotionEffect> effects, String permission, Material icon) {
            this(id, displayName, healthCost, costItem, attributeModifiers, effects, permission, icon, null);
        }

        public Pact(String id, String displayName, double healthCost, org.bukkit.Material costItem, Map<Attribute, Double> attributeModifiers, List<PotionEffect> effects, String permission, Material icon, Map<org.bukkit.entity.EntityType, PredatorBuff> mobBuffs) {
            this.id = id;
            this.displayName = displayName;
            this.healthCost = healthCost;
            this.costItem = costItem;
            this.attributeModifiers = attributeModifiers;
            this.effects = effects;
            this.permission = permission;
            this.icon = icon;
            this.mobBuffs = mobBuffs;
        }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getHealthCost() {
        return healthCost;
    }

    public Map<Attribute, Double> getAttributeModifiers() {
        return attributeModifiers;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }

        public PredatorBuff getBuffForMob(org.bukkit.entity.EntityType entityType) {
            if (mobBuffs == null) return null;
            return mobBuffs.get(entityType);
        }

        public Map<org.bukkit.entity.EntityType, PredatorBuff> getMobBuffs() {
            if (mobBuffs == null) return null;
            return new java.util.HashMap<>(mobBuffs);
        }

    public String getPermission() {
        return permission;
    }

    public Material getIcon() {
        return icon;
    }

    public Material getCostItem() {
        return costItem;
    }
}
