package com.soulledger.command;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface SubCommand {
    String getName();
    String getDescription();
    String getSyntax();
    String getPermission();
    boolean perform(CommandSender sender, String[] args);
    List<String> getTabComplete(CommandSender sender, String[] args);
}
