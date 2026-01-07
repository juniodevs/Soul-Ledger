package com.soulledger;

import com.soulledger.command.PactCommand;
import com.soulledger.gui.PactGui;
import com.soulledger.listener.PlayerDeathListener;
import com.soulledger.manager.PactManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class SoulLedgerPlugin extends JavaPlugin {

    private com.soulledger.listener.MobListener mobListener;
    private com.soulledger.gui.NecromancerSummonGui necromancerSummonGui;
    private PactManager pactManager;
    private PactGui pactGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pactManager = new PactManager(this);
        this.pactGui = new PactGui(this);
        this.mobListener = new com.soulledger.listener.MobListener(this);
        this.necromancerSummonGui = new com.soulledger.gui.NecromancerSummonGui(this, mobListener);

        getServer().getPluginManager().registerEvents(mobListener, this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        getServer().getPluginManager().registerEvents(new com.soulledger.listener.ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new com.soulledger.listener.GuiListener(this), this);

        getCommand("soulledger").setExecutor(new PactCommand(this));

        getLogger().info("Soul Ledger started. Death awaits.");
    }

    public com.soulledger.command.sub.ListSoulsSubCommand createListSoulsSubCommand() {
        return new com.soulledger.command.sub.ListSoulsSubCommand(mobListener);
    }

    public com.soulledger.listener.MobListener getMobListener() {
        return mobListener;
    }

    public com.soulledger.gui.NecromancerSummonGui getNecromancerSummonGui() {
        return necromancerSummonGui;
    }
    
    public PactGui getPactGui() {
        return pactGui;
    }

    @Override
    public void onDisable() {
        getLogger().info("Soul Ledger disabled.");
    }

    public PactManager getPactManager() {
        return pactManager;
    }

    public String getFormattedMessage(String key) {
        String lang = getConfig().getString("settings.language", "en");
        String path = "messages." + lang + "." + key;
        String prefix = getConfig().getString("messages." + lang + ".prefix", "");
        
        String msg = getConfig().getString(path);
        if (msg == null) {
            msg = getConfig().getString("messages.en." + key, "Message not found: " + key);
        }
        
        msg = msg.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
