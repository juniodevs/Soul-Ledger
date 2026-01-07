package com.soulledger.model;

import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredatorBuff {
    private final EntityType mobType;
    private final List<PotionEffect> buffs;
    private final List<PotionEffect> playerKillBuffs;

    public PredatorBuff(EntityType mobType, List<PotionEffect> buffs, List<PotionEffect> playerKillBuffs) {
        this.mobType = mobType;
        this.buffs = buffs != null ? buffs : new ArrayList<>();
        this.playerKillBuffs = playerKillBuffs != null ? playerKillBuffs : new ArrayList<>();
    }

    public EntityType getMobType() { return mobType; }
    public List<PotionEffect> getBuffs() { return Collections.unmodifiableList(buffs); }
    public List<PotionEffect> getPlayerKillBuffs() { return Collections.unmodifiableList(playerKillBuffs); }
}
