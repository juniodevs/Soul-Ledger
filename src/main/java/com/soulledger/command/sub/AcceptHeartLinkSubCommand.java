package com.soulledger.command.sub;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.command.SubCommand;
import com.soulledger.manager.PactManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptHeartLinkSubCommand implements SubCommand {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;

    public AcceptHeartLinkSubCommand(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    @Override
    public String getName() {
        return "acceptheartlink";
    }

    @Override
    public String getDescription() {
        return "Accept a Heart Link request";
    }

    @Override
    public String getSyntax() {
        return "/sl acceptheartlink <player>";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getFormattedMessage("only_players"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getFormattedMessage("heartlink_accept_usage"));
            return true;
        }
        Player player = (Player) sender;
        String requester = args[1];
        pactManager.acceptHeartLink(player, requester);
        return true;
    }

    @Override
    public java.util.List<String> getTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
