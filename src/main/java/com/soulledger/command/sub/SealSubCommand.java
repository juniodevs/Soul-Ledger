package com.soulledger.command.sub;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.command.SubCommand;
import com.soulledger.manager.PactManager;
import com.soulledger.model.Pact;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SealSubCommand implements SubCommand {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;

    public SealSubCommand(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
    }

    @Override
    public String getName() {
        return "seal";
    }

    @Override
    public String getDescription() {
        return "Sela um pacto específico";
    }

    @Override
    public String getSyntax() {
        return "/sl seal <pact_id>";
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
            sender.sendMessage(plugin.getFormattedMessage("usage_seal"));
            return true;
        }
        Player player = (Player) sender;
        String pactId = args[1];

        if (pactId.equalsIgnoreCase("heartlink") || pactId.equalsIgnoreCase("heart_link")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.getFormattedMessage("heartlink_usage"));
                return true;
            }
            String targetName = args[2];
            pactManager.requestHeartLink(player, targetName);
            return true;
        }

        Pact target = pactManager.getLoadedPacts().get(pactId);
        if (target == null) {
            sender.sendMessage(plugin.getFormattedMessage("pact_not_found"));
            return true;
        }
        if (!player.hasPermission(target.getPermission())) {
            sender.sendMessage(plugin.getFormattedMessage("no_permission"));
            return true;
        }
        pactManager.sealPact(player, pactId);
        return true;
    }

    @Override
    public List<String> getTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(pactManager.getLoadedPacts().keySet());
        }
        return null;
    }
}
