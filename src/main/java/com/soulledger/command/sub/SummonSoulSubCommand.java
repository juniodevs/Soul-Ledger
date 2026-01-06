package com.soulledger.command.sub;

import com.soulledger.listener.NecromancyListener;
import com.soulledger.manager.PactManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

public class SummonSoulSubCommand implements TabExecutor {
    private final NecromancyListener necromancyListener;
    private final PactManager pactManager;

    public SummonSoulSubCommand(NecromancyListener necromancyListener, PactManager pactManager) {
        this.necromancyListener = necromancyListener;
        this.pactManager = pactManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Comando apenas para jogadores.");
            return true;
        }
        Player player = (Player) sender;
        if (!pactManager.hasNecromancyPact(player)) {
            player.sendMessage(ChatColor.RED + "Você não possui o pacto de necromancia.");
            return true;
        }
        EntityDeathEvent lastDeath = necromancyListener.getLastMobDeath(player);
        if (lastDeath == null) {
            player.sendMessage(ChatColor.RED + "Você ainda não matou nenhum mob recentemente.");
            return true;
        }
        LivingEntity mob = (LivingEntity) lastDeath.getEntity();
        EntityType type = mob.getType();
        LivingEntity summoned = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), type);
        summoned.setCustomName(ChatColor.DARK_PURPLE + "Alma Invocada por " + player.getName());
        summoned.setCustomNameVisible(true);
        summoned.setRemoveWhenFarAway(true);
        // Marca entidade como necromante e vincula ao jogador
        summoned.setMetadata("necromancer_owner", new org.bukkit.metadata.FixedMetadataValue(Bukkit.getPluginManager().getPlugin("SoulLedger"), player.getUniqueId().toString()));
        // Efeitos visuais
        player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1, 0.7f);
        // Reduz vida máxima em 2 (1 coração)
        double currentMax = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(2, currentMax - 2));
        // Remove mob após 30 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!summoned.isDead()) summoned.remove();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("SoulLedger"), 20 * 30);
        // Limpa último mob
        necromancyListener.clearLastMobDeath(player);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Você invocou uma alma! Sua vida máxima foi reduzida em 1 coração. Quando a alma morrer, você recupera.");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return java.util.Collections.emptyList();
    }
}
