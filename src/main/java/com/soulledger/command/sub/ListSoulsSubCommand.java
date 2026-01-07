package com.soulledger.command.sub;

import com.soulledger.listener.MobListener;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.soulledger.command.SubCommand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.*;

public class ListSoulsSubCommand implements SubCommand {
    private final MobListener mobListener;
    public ListSoulsSubCommand(MobListener mobListener) {
        this.mobListener = mobListener;
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
        com.soulledger.SoulLedgerPlugin plugin = (com.soulledger.SoulLedgerPlugin) sender.getServer().getPluginManager().getPlugin("SoulLedger");
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getFormattedMessage("only_players"));
            return true;
        }
        Player player = (Player) sender;
        List<LivingEntity> souls = mobListener.getSummonedSouls(player);
        if (souls.isEmpty()) {
            player.sendMessage(plugin.getFormattedMessage("no_souls_summoned"));
            return true;
        }
        player.sendMessage(plugin.getFormattedMessage("souls_list_header"));
        for (LivingEntity soul : souls) {
            String msg = plugin.getFormattedMessage("soul_list_entry")
                    .replace("%type%", soul.getType().name())
                    .replace("%x%", String.valueOf(soul.getLocation().getBlockX()))
                    .replace("%y%", String.valueOf(soul.getLocation().getBlockY()))
                    .replace("%z%", String.valueOf(soul.getLocation().getBlockZ()));
            player.sendMessage(msg);
        }
        return true;
    }
    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}
