/*package com.stardust.banksys.old;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;

import com.stardust.banksys.DBActions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.stardust.banksys.utils.ConstantManager.inst();

public class BankerGui implements Listener {
    private static int n = 9;

    public static Inventory create(final Player player) {
        final Inventory inventory = Bukkit.createInventory(null, n,
                ConstantManager.inst().bank_name + " " + ConstantManager.inst().bank_balance + " $" + DBActions.loadBankBalance(player.getUniqueId()));
        
        final int depositBtn = 3;
        final int withdrawBtn = 4;
        final int interestBtn = 5;

        Material material = null; String name = ""; String[] lore = {};
        for (int i = 0; i < n; i++) {
            switch (i) {
                case depositBtn: 
                    material = Material.ACACIA_BUTTON;
                    name = ConstantManager.inst().deposit_name;
                    lore = ConstantManager.inst().deposit_lore.split("\n");
                    break;
                case withdrawBtn:
                    material = Material.ACACIA_FENCE;
                    name = ConstantManager.inst().withdraw_name;
                    lore = ConstantManager.inst().withdraw_lore.split("\n");
                    break;
                case interestBtn:
                    material = Material.SPAWNER;
                    name = ConstantManager.inst().interest_name;
                    lore = ConstantManager.inst().interest_lore.split("\n");
                    break;
                default: 
                    material = Material.BLUE_STAINED_GLASS_PANE;
                    name = "";
                    lore = null;
                    break;
            }

            inventory.setItem(i, createGuiItem(material, name, lore));  
        }
        return inventory;
    }

    private static ItemStack createGuiItem(final Material material, final String name, final String[] lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        
        if (name != null) meta.setDisplayName(name);
        if (lore != null) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private HashMap<Player, Timer> interestTimers = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        try {
            final Inventory inventory = event.getInventory();
            final String clickedItemName = event.getCurrentItem().getItemMeta().getDisplayName();
            Double amount = null;

            if (clickedItemName.equals(ConstantManager.inst().deposit_name)) {
                amount = ConstantManager.inst().deposit_amount;
                BankerNPC.deposit(player, amount, interestTimers);
                inventory.close();
                openBank(player);

            }
            else if (clickedItemName.equals(ConstantManager.inst().withdraw_name)) {
                amount = ConstantManager.inst().withdraw_amount;
                BankerNPC.withdraw(player, amount);
                inventory.close();
                openBank(player);
            }
            else if (clickedItemName.equals(ConstantManager.inst().interest_name)) {
                final ItemStack heldItem = event.getCursor();

                amount = ConstantManager.inst().interest_rate_per_spawner;
                if (heldItem.getType() == Material.SPAWNER) {
                    BankerNPC.increaseInterest(player, amount*heldItem.getAmount(), interestTimers);
                    // inventory.getItem(5).add(heldItem.getAmount());
                    player.setItemOnCursor(null);
                } else {
                    BankerNPC.message(player, ConstantManager.inst().spawner_needed);
                }
            };

            if (amount != null || clickedItemName.equals("decoration")) {
                event.setCancelled(true);
            }

        } catch (NullPointerException exception) {}
    }

    private void openBank(Player player) {
        final Inventory bank = BankerGui.create(player);
        player.openInventory(bank);
    }
}
*/