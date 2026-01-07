package com.soulledger.listener;

import com.soulledger.SoulLedgerPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;

public class MobListener implements Listener {

    private final SoulLedgerPlugin plugin;
    private final Map<UUID, List<LivingEntity>> capturedSouls = new HashMap<>();

    public MobListener(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;

        // Lógica do Pacto Predador
        plugin.getPactManager().applyPredatorBuff(killer, victim);

        // Lógica de Captura de Almas (Necromancia)
        if (plugin.getPactManager().hasNecromancyPact(killer)) {
            captureSoul(killer, victim);
        }
    }

    private void captureSoul(Player player, LivingEntity entity) {
        List<LivingEntity> souls = capturedSouls.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        if (souls.size() < 10) { // Limite arbitrário para exemplo
            souls.add(entity);
        }
    }

    public List<LivingEntity> getCapturedSouls(Player player) {
        return capturedSouls.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    private final Map<UUID, List<LivingEntity>> summonedSouls = new HashMap<>();

    public void addSummonedSoul(Player player, LivingEntity entity) {
        summonedSouls.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(entity);
    }

    public List<LivingEntity> getSummonedSouls(Player player) {
        return summonedSouls.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void removeAllSouls(Player player) {
        List<LivingEntity> souls = summonedSouls.remove(player.getUniqueId());
        if (souls != null) {
            souls.forEach(LivingEntity::remove);
        }
        capturedSouls.remove(player.getUniqueId());
    }
}
