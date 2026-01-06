package com.soulledger.command.sub;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.command.SubCommand;
import com.soulledger.manager.PactManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadSubCommand implements SubCommand {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;

    public ReloadSubCommand(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarrega as configurações do plugin";
    }

    @Override
    public String getSyntax() {
        return "/sl reload";
    }

    @Override
    public String getPermission() {
        return "soulledger.admin";
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        pactManager.loadPactsFromConfig();
        sender.sendMessage(plugin.getFormattedMessage("config_reloaded"));
        return true;
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
