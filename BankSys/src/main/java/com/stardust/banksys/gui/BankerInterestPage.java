package com.stardust.banksys.gui;

import com.stardust.banksys.BankSys;
import com.stardust.banksys.DBActions;
import com.stardust.banksys.utils.ConstantManager;
import net.citizensnpcs.api.gui.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Math.round;
import static org.bukkit.Bukkit.getScheduler;

@Menu(title = "", type = InventoryType.BARREL, dimensions = { 3, 9 })
public class BankerInterestPage extends InventoryMenuPage {
    MenuContext context;
    OfflinePlayer playerWhoInitialized;
    ItemStack spawner;
    static Component spawnerDisplayName = Component.text("&4&kdd&6&lZINS_SPAWNER&4&kdd".replace("&", "§"));

    public BankerInterestPage(Player player) {
        BankSys.BankAdminEditSession ses = BankSys.getInstance().getBankAdminSession(player.getUniqueId());
        if (ses == null)
            playerWhoInitialized = player;
        else {
            playerWhoInitialized = Bukkit.getOfflinePlayer(ses.target);
            player.sendMessage("Editing " + playerWhoInitialized.getName() + "'s Bank Account");
        }
    }

    void updateTitle() {
        DecimalFormat df = new DecimalFormat("0.00");
        int spawners = DBActions.loadSpawners(playerWhoInitialized.getUniqueId());
        context.setTitle(ConstantManager.inst().interest_rate_str + " " +
                df.format(spawners * ConstantManager.inst().interest_rate_per_spawner) + "%");
        context.getInventory();
    }
    @Override
    public void initialise(MenuContext menuContext) {
        context = menuContext;

        int spawners = DBActions.loadSpawners(playerWhoInitialized.getUniqueId());
        spawner = BankSys.getInstance().getZinsSpawner(1);
        for (int i = 0; i < spawners; i++) {
            context.getSlot(i).setItemStack(spawner);
        }
        for (int i = 0; i < context.getInventory().getStorageContents().length; i++) {
            context.getSlot(i).addClickHandler(this::onSlotClicked);
        }
        getScheduler().runTaskLater(BankSys.getInstance(), this::updateTitle, 1);

        BankSys.getInstance().sensitiveInventories.add(context.getInventory());
    }

    @Override
    public void onClose(HumanEntity player) {
        BankSys.getInstance().sensitiveInventories.remove(context.getInventory());
    }

    double getMaxInterest(HumanEntity player) {
        if (player.hasPermission(ConstantManager.inst().rank_high_perm))
            return ConstantManager.inst().rank_high_max_interest;
        else if (player.hasPermission(ConstantManager.inst().rank_medium_perm))
            return ConstantManager.inst().rank_medium_max_interest;
        else if (player.hasPermission(ConstantManager.inst().rank_low_perm))
            return ConstantManager.inst().rank_low_max_interest;
        else
            return ConstantManager.inst().rank_default_max_interest;
    }

    public void onSlotClicked(InventoryClickEvent event) {


        // Allow admin to manipulate spawners
        if (BankSys.getInstance().isBankAdmin(event.getWhoClicked().getUniqueId())) {
            getScheduler().runTaskLater(BankSys.getInstance(), () -> {
                int spawners = 0;
                for (int i = 0; i < context.getInventory().getStorageContents().length; i++) {
                    ItemStack item = context.getSlot(i).getCurrentItem();
                    if (item != null && item.getType().equals(Material.SPAWNER)) {
                       spawners++;
                    }
                }
                DBActions.saveSpawners(playerWhoInitialized.getUniqueId(), spawners);
                updateTitle();
            }, 1);
            return;
        }

        if (event.getClick().isShiftClick()) {
            Bukkit.getLogger().info("Cancel!");
            event.setCancelled(true);
            return;
        }

        //
        if ((event.getAction() == InventoryAction.PLACE_ONE || event.getAction() == InventoryAction.PLACE_ALL)) {
            if (event.getCurrentItem() != null) {
                event.setResult(Event.Result.DENY);
                return;
            }
            ItemStack cursor = event.getCursor();
            if (cursor.getType().equals(Material.SPAWNER) && cursor.getItemMeta().getEnchants().containsKey(Enchantment.PROTECTION_ENVIRONMENTAL) && cursor.getItemMeta().getEnchants().get(Enchantment.PROTECTION_ENVIRONMENTAL) == 10) {

                double max_interest = getMaxInterest(event.getWhoClicked());
                if ((DBActions.loadSpawners(event.getWhoClicked().getUniqueId())+1) * ConstantManager.inst().interest_rate_per_spawner >
                    max_interest) {
                    event.setResult(Event.Result.DENY);
                    ConstantManager.sendFormattedMessage((Player) event.getWhoClicked(), ConstantManager.inst().cant_increase_interest);
                    return;
                }
                getScheduler().runTaskLater(BankSys.getInstance(), () -> {
                    UUID uuid = event.getWhoClicked().getUniqueId();
                    DBActions.saveSpawners(uuid, DBActions.loadSpawners(uuid) + 1);
                    updateTitle();
                }, 1);

                if (event.getAction() == InventoryAction.PLACE_ALL) {
                    event.setCurrentItem(spawner);
                    event.getCursor().setAmount(event.getCursor().getAmount() - 1);
                    event.setResult(Event.Result.DENY);
                } else {
                    event.setResult(Event.Result.ALLOW);
                }
            } else {
                event.setResult(Event.Result.DENY);
            }
            return;
        }
        event.setResult(Event.Result.DENY);
    }
}