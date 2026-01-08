package com.soulledger;

import com.soulledger.command.PactCommand;
import com.soulledger.gui.PactGui;
import com.soulledger.gui.NecromancerSummonGui;
import com.soulledger.listener.GuiListener;
import com.soulledger.listener.HeartLinkListener;
import com.soulledger.listener.ItemListener;
import com.soulledger.listener.MobListener;
import com.soulledger.listener.PlayerDeathListener;
import com.soulledger.manager.PactManager;
import com.soulledger.util.ColorUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class SoulLedgerPlugin extends JavaPlugin {

    private MobListener mobListener;
    private NecromancerSummonGui necromancerSummonGui;
    private PactManager pactManager;
    private PactGui pactGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("mob_buffs.yml", false);

        this.pactManager = new PactManager(this);
        this.pactGui = new PactGui(this);
        this.mobListener = new MobListener(this);
        this.necromancerSummonGui = new NecromancerSummonGui(this, mobListener);

        registerEvents();
        registerCommands();

        getLogger().info("Soul Ledger started. Death awaits.");
    }

    private void registerEvents() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(mobListener, this);
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new ItemListener(this), this);
        pm.registerEvents(new HeartLinkListener(this), this);
        pm.registerEvents(new GuiListener(this), this);
    }

    private void registerCommands() {
        getCommand("soulledger").setExecutor(new PactCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("Soul Ledger disabled.");
    }

    public PactManager getPactManager() { return pactManager; }
    public MobListener getMobListener() { return mobListener; }
    public NecromancerSummonGui getNecromancerSummonGui() { return necromancerSummonGui; }
    public PactGui getPactGui() { return pactGui; }

    public com.soulledger.command.sub.ListSoulsSubCommand createListSoulsSubCommand() {
        return new com.soulledger.command.sub.ListSoulsSubCommand(mobListener);
    }

    public String getFormattedMessage(String key) {
        String lang = getConfig().getString("settings.language", "en");
        String prefix = getConfig().getString("messages." + lang + ".prefix", "");
        String msg = getConfig().getString("messages." + lang + "." + key);
        
        if (msg == null) {
            msg = getConfig().getString("messages.en." + key, "Message not found: " + key);
        }
        
        return ColorUtil.color(msg.replace("%prefix%", prefix));
    }
}
