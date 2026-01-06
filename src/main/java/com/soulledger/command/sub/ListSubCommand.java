package com.soulledger.command.sub;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.command.SubCommand;
import com.soulledger.manager.PactManager;
import com.soulledger.model.Pact;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ListSubCommand implements SubCommand {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;

    public ListSubCommand(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Concede a lista de pactos disponíveis";
    }

    @Override
    public String getSyntax() {
        return "/sl list";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        sender.sendMessage(plugin.getFormattedMessage("list_header"));
        String listFormat = plugin.getFormattedMessage("pact_list_format");
        for (Pact p : pactManager.getLoadedPacts().values()) {
            String line = listFormat
                    .replace("%name%", p.getDisplayName())
                    .replace("%cost%", String.valueOf((int)p.getHealthCost()))
                    .replace("%id%", p.getId());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        return true;
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
