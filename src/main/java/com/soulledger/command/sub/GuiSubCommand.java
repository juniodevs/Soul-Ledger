package com.soulledger.command.sub;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class GuiSubCommand implements SubCommand {

    private final SoulLedgerPlugin plugin;

    public GuiSubCommand(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getDescription() {
        return "Abre o menu de pactos";
    }

    @Override
    public String getSyntax() {
        return "/sl gui";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            plugin.getPactGui().openPactMenu((Player) sender);
        } else {
            sender.sendMessage(plugin.getFormattedMessage("only_players"));
        }
        return true;
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
