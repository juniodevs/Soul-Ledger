package com.soulledger.gui;

import com.soulledger.SoulLedgerPlugin;
import com.soulledger.manager.PactManager;
import com.soulledger.model.Pact;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        int size = 9 * 4; // 36 slots for a more spacious layout
        if (pactManager.getLoadedPacts().size() > 27) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "❖ Soul Ledger ❖");

        int slot = 10;
        for (Pact pact : pactManager.getLoadedPacts().values()) {
            ItemStack item = new ItemStack(pact.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', pact.getDisplayName()));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Sacrifice: " + ChatColor.RED + (pact.getHealthCost() > 0 ? "-" : "+") + Math.abs((int)pact.getHealthCost()) + " HP");
                if (!pact.getAttributeModifiers().isEmpty()) {
                    lore.add("");
                    lore.add(ChatColor.GOLD + "Attributes:");
                    for (Map.Entry<Attribute, Double> entry : pact.getAttributeModifiers().entrySet()) {
                        String sign = entry.getValue() > 0 ? ChatColor.GREEN + "+" : ChatColor.RED + "-";
                        lore.add(ChatColor.YELLOW + "  " + entry.getKey().name().replace("GENERIC_", "").replace("_", " ") + ": " + sign + Math.abs(entry.getValue()));
                    }
                }
                if (!pact.getEffects().isEmpty()) {
                    lore.add("");
                    lore.add(ChatColor.AQUA + "Effects:");
                    for (PotionEffect effect : pact.getEffects()) {
                        String effectName = effect.getType().getName().replace("_", " ").toLowerCase();
                        String effectText = ChatColor.BLUE + "  " + Character.toUpperCase(effectName.charAt(0)) + effectName.substring(1) + " " + ChatColor.WHITE + "Lv." + (effect.getAmplifier() + 1);
                        lore.add(effectText);
                    }
                }
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Click to seal this pact!");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(pactKey, PersistentDataType.STRING, pact.getId());
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
            if (slot == 17 || slot == 26 || slot == 35) slot += 2; // Decorative spacing
        }

        // Decorative border
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "❖ Soul Ledger ❖")) return;
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
