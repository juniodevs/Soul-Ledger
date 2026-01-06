package com.soulledger.command.sub;

import com.soulledger.listener.NecromancyListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.soulledger.command.SubCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.*;

public class ListSoulsSubCommand implements SubCommand {
    private final NecromancyListener necromancyListener;
    public ListSoulsSubCommand(NecromancyListener necromancyListener) {
        this.necromancyListener = necromancyListener;
    }
    @Override
    public String getName() { return "listsouls"; }

    @Override
    public String getDescription() { return "Lista as almas invocadas pelo necromante."; }

    @Override
    public String getSyntax() { return "/sl listsouls"; }
    @Override
    public String getPermission() { return null; }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores.");
            return true;
        }
        Player player = (Player) sender;
        List<LivingEntity> souls = necromancyListener.getSummonedSouls(player);
        if (souls.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Você não possui almas invocadas no momento.");
            return true;
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Almas invocadas:");
        for (LivingEntity soul : souls) {
            player.sendMessage(ChatColor.DARK_PURPLE + "- " + soul.getType().name() + " em " + soul.getLocation().getBlockX() + ", " + soul.getLocation().getBlockY() + ", " + soul.getLocation().getBlockZ());
        }
        return true;
    }
    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
