package com.soulledger.model;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import java.util.List;

public class Pact {
    private final String id;
    private final String displayName;
    private final double healthCost;
    private final List<PotionEffect> effects;
    private final String permission;
    private final Material icon;

    public Pact(String id, String displayName, double healthCost, List<PotionEffect> effects, String permission, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.healthCost = healthCost;
        this.effects = effects;
        this.permission = permission;
        this.icon = icon;
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

    public List<PotionEffect> getEffects() {
        return effects;
    }

    public String getPermission() {
        return permission;
    }

    public Material getIcon() {
        return icon;
    }
}
