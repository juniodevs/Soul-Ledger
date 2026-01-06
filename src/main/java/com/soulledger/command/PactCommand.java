package com.soulledger.command;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.command.sub.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PactCommand implements CommandExecutor, TabCompleter {

    private final SoulLedgerPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public PactCommand(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        registerSubCommand(new SealSubCommand(plugin));
        registerSubCommand(new ListSubCommand(plugin));
        registerSubCommand(new ReloadSubCommand(plugin));
        registerSubCommand(new GuiSubCommand(plugin));
        // Registrar comando listsouls
        registerSubCommand(plugin.createListSoulsSubCommand());
    }

    private void registerSubCommand(SubCommand command) {
        subCommands.put(command.getName(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getPactGui().openPactMenu((Player) sender);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        
        if (subName.equals("menu")) subName = "gui"; 

        if (!subCommands.containsKey(subName)) {
            sender.sendMessage(plugin.getFormattedMessage("unknown_command"));
            return true;
        }

        SubCommand subCommand = subCommands.get(subName);

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(plugin.getFormattedMessage("no_permission"));
            return true;
        }

        subCommand.perform(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = subCommands.values().stream()
                    .filter(sub -> sub.getPermission() == null || sender.hasPermission(sub.getPermission()))
                    .map(SubCommand::getName)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[0], commands, new ArrayList<>());
        } else if (args.length >= 2) {
            String subName = args[0].toLowerCase();
            if (subCommands.containsKey(subName)) {
                return subCommands.get(subName).getTabComplete(sender, args);
            }
        }
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_PURPLE + "=== Soul Ledger Help ===");
        sender.sendMessage(ChatColor.GOLD + "v" + plugin.getDescription().getVersion());
        for (SubCommand sub : subCommands.values()) {
            if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + sub.getSyntax() + ChatColor.GRAY + " - " + sub.getDescription());
            }
        }
    }
}
