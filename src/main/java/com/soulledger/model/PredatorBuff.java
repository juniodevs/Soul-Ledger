package com.soulledger.model;

import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PredatorBuff {
    private final EntityType mobType;
    private final PotionEffect effect;
    private final int durationTicks;

    public PredatorBuff(EntityType mobType, PotionEffectType effectType, int amplifier, int durationSeconds) {
        this.mobType = mobType;
        this.effect = new PotionEffect(effectType, durationSeconds * 20, amplifier);
        this.durationTicks = durationSeconds * 20;
    }

    public EntityType getMobType() {
        return mobType;
    }

    public PotionEffect getEffect() {
        return effect;
    }

    public int getDurationTicks() {
        return durationTicks;
    }
}
