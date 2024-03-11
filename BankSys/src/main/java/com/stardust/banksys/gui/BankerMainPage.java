package com.stardust.banksys.gui;

import com.stardust.banksys.BankSys;
import com.stardust.banksys.DBActions;
import com.stardust.banksys.utils.ConstantManager;
import net.citizensnpcs.api.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getScheduler;

@Menu(title = "", type = InventoryType.HOPPER, dimensions = { 0, 5 })
@MenuSlot(slot = { 0, 1 }, title = "")
@MenuSlot(slot = { 0, 2 }, title = "")
@MenuSlot(slot = { 0, 3 }, title = "")
public class BankerMainPage extends InventoryMenuPage {
    OfflinePlayer playerWhoInitialized;
    MenuContext context;
    boolean admin;

    public BankerMainPage(Player player) {
        BankSys.BankAdminEditSession ses = BankSys.getInstance().getBankAdminSession(player.getUniqueId());
        admin = false;
        if (ses == null)
            playerWhoInitialized = player;
        else {
            playerWhoInitialized = Bukkit.getOfflinePlayer(ses.target);
            admin = true;
            player.sendMessage("Editing " + playerWhoInitialized.getName() + "'s Bank Account");
        }
    }

    void updateTitle() {
        DecimalFormat df = new DecimalFormat("0.00");
        double bal = DBActions.loadBankBalance(playerWhoInitialized.getUniqueId());
        context.setTitle(ConstantManager.inst().balance_str + " " + df.format(bal) + ConstantManager.inst().currency_sym);
    }

    void setItemTitle(int slot, String icon, String title) {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        try {
            item.setType(Material.valueOf(icon));
        } catch(Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] Config: Invalid icon string: " + icon);
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        item.setItemMeta(meta);
        context.getSlot(slot).setItemStack(item);
    }

    @Override
    public void initialise(MenuContext ctx) {
        context = ctx;
        getScheduler().runTaskLater(BankSys.getInstance(), this::updateTitle, 1);
        setItemTitle(1, ConstantManager.inst().deposit_icon, ConstantManager.inst().deposit_str);
        setItemTitle(2, ConstantManager.inst().withdraw_icon, ConstantManager.inst().withdraw_str);
        setItemTitle(3, ConstantManager.inst().interest_icon, ConstantManager.inst().interest_str);
    }

    @Override
    public void onClose(HumanEntity player) {
        super.onClose(player);
    }

    @ClickHandler(slot = { 0, 3 }, filter = { InventoryAction.PICKUP_ALL })
    public void onInterestClick(InventoryMenuSlot slot, CitizensInventoryClickEvent event) {
        if (event.getAction() == InventoryAction.PICKUP_ALL)
            context.getMenu().transition(new BankerInterestPage((Player) event.getWhoClicked()));
        event.setResult(Event.Result.DENY);
    }

    @ClickHandler(slot = { 0, 1 }, filter = { InventoryAction.PICKUP_ALL })
    public void onDepositClick(InventoryMenuSlot slot, CitizensInventoryClickEvent event) {
        if (event.getAction() == InventoryAction.PICKUP_ALL)
            context.getMenu().transition(InputMenus.stringSetter(() -> "0", s -> {
                double newval = Double.parseDouble(s);
                if (DBActions.loadPlayerBalance(playerWhoInitialized.getUniqueId()) >= newval || admin) {
                    ConstantManager.sendFormattedMessage((Player) event.getWhoClicked(),
                            ConstantManager.inst().deposited_str + " " + ConstantManager.inst().currency_sym + newval);
                    if (!admin)
                        DBActions.subPlayerBalance(playerWhoInitialized.getUniqueId(), newval);
                    DBActions.saveBankBalance(playerWhoInitialized.getUniqueId(),
                            DBActions.loadBankBalance(playerWhoInitialized.getUniqueId()) + newval);
                }
                else {
                    ConstantManager.sendFormattedMessage((Player) event.getWhoClicked(),
                            ConstantManager.inst().cannot_deposit
                    );
                }
            }));
        event.setResult(Event.Result.DENY);
    }

    @ClickHandler(slot = { 0, 2 }, filter = { InventoryAction.PICKUP_ALL })
    public void onWithdrewClick(InventoryMenuSlot slot, CitizensInventoryClickEvent event) {
        if (event.getAction() == InventoryAction.PICKUP_ALL)
            context.getMenu().transition(InputMenus.stringSetter(() -> "0", s -> {
                double newval = Double.parseDouble(s);
                double balance = DBActions.loadBankBalance(playerWhoInitialized.getUniqueId());
                if (balance >= newval) {
                    ConstantManager.sendFormattedMessage((Player) event.getWhoClicked(),
                            ConstantManager.inst().withdrew_str + " " + ConstantManager.inst().currency_sym + newval);
                    DBActions.saveBankBalance(playerWhoInitialized.getUniqueId(), balance - newval);
                    DBActions.addPlayerBalance(playerWhoInitialized.getUniqueId(), newval);
                } else {
                    ConstantManager.sendFormattedMessage((Player) event.getWhoClicked(),
                            ConstantManager.inst().cannot_withdraw
                    );
                }
            }));
        event.setResult(Event.Result.DENY);
    }
}
