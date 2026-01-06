package com.soulledger.gui;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.manager.PactManager;
import com.soulledger.model.Pact;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PactGui implements Listener {

    private final SoulLedgerPlugin plugin;
    private final PactManager pactManager;
    private final NamespacedKey pactKey;
    private final String GUI_TITLE;

    public PactGui(SoulLedgerPlugin plugin) {
        this.plugin = plugin;
        this.pactManager = plugin.getPactManager();
        this.pactKey = new NamespacedKey(plugin, "gui_pact_id");
        this.GUI_TITLE = ChatColor.DARK_PURPLE + "Soul Ledger - Pacts";
    }

    public void openPactMenu(Player player) {
        int size = 9 * 3;
        if (pactManager.getLoadedPacts().size() > 27) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, GUI_TITLE);

        for (Pact pact : pactManager.getLoadedPacts().values()) {
            ItemStack item = new ItemStack(pact.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', pact.getDisplayName()));
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.RED + (int)pact.getHealthCost() + " HP");
                lore.add("");
                lore.add(ChatColor.GRAY + "Effects:");
                pact.getEffects().forEach(effect -> 
                    lore.add(ChatColor.DARK_AQUA + " - " + effect.getType().getName() + " " + (effect.getAmplifier() + 1))
                );
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to seal this pact!");
                
                meta.setLore(lore);
                
                meta.getPersistentDataContainer().set(pactKey, PersistentDataType.STRING, pact.getId());
                
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(pactKey, PersistentDataType.STRING)) {
            String pactId = clicked.getItemMeta().getPersistentDataContainer().get(pactKey, PersistentDataType.STRING);
            
            Pact pact = pactManager.getLoadedPacts().get(pactId);
            if (pact != null && !player.hasPermission(pact.getPermission())) {
                 player.sendMessage(plugin.getFormattedMessage("no_permission"));
                 player.closeInventory();
                 return;
            }

            boolean success = pactManager.sealPact(player, pactId);
            if (success) {
                player.closeInventory();
            }
        }
    }
}
