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

public class PactGui {

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
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "❖ Soul Ledger ❖");
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
        int i = 0;
        for (Pact pact : pactManager.getLoadedPacts().values()) {
            if (i >= slots.length) break;
            ItemStack item = new ItemStack(pact.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', pact.getDisplayName()));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Sacrifice: " + ChatColor.RED + (pact.getHealthCost() > 0 ? "-" : "+") + Math.abs((int)pact.getHealthCost()) + " HP");
                org.bukkit.Material displayCost = pact.getCostItem() != null ? pact.getCostItem() : pactManager.getGlobalCostItem();
                if (displayCost != null) {
                    lore.add(ChatColor.GRAY + "Required: " + ChatColor.AQUA + displayCost.name().replace("_", " ").toLowerCase());
                }
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
            inv.setItem(slots[i], item);
            i++;
        }
        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }
        for (int j = 0; j < size; j++) {
            if (inv.getItem(j) == null) inv.setItem(j, border);
        }
        player.openInventory(inv);
    }

    public void handleInternalClick(InventoryClickEvent event) {
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
