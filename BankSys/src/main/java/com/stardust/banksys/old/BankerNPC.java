/*package com.stardust.banksys.old;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.stardust.banksys.DBActions;
import com.stardust.banksys.gui.BankerTrait;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;

import com.stardust.banksys.utils.AccumulateInterest;
import com.stardust.banksys.utils.ConstantManager.inst();
import com.willfp.eco.core.integrations.economy.EconomyManager;

import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.ChatColor;
import net.citizensnpcs.api.CitizensAPI;

public class BankerNPC {

    private static NPC banker;


    public static void message(final Player player, final String message) {
        player.sendMessage(ChatColor.GOLD + "[" + ConstantManager.inst().banker_name + "] " + ChatColor.YELLOW + message);
    }

    public static void withdraw(final Player player, double amount) {
        final double playerBankBalance = DBActions.loadBankBalance(player.getUniqueId());
        if (playerBankBalance < amount) {
            amount = playerBankBalance;
        } 

        EconomyManager.giveMoney(player, amount);
        final double newAmount = playerBankBalance - amount;
        DBActions.saveBankBalance(player.getUniqueId(), newAmount);
        message(player, "You have withdrawn $" + amount + "\nYou now have $" + newAmount + " in the bank");
    }

    public static void deposit(final Player player, final double amount, HashMap<Player, Timer> interestTimers) {
        try {
            if (Economy.getMoneyExact(player.getUniqueId()).doubleValue() < amount) {
                message(player, ConstantManager.inst().cannot_deposit);
                return;
            }
        } catch (UserDoesNotExistException e) {
            return;
        }

        final double playerBankBalance = DBActions.loadBankBalance(player.getUniqueId());
        if (playerBankBalance == 0) {
           initalizeInterestAccumulation(player, interestTimers);
        }
        EconomyManager.removeMoney(player, amount);
        DBActions.saveBankBalance(player.getUniqueId(), playerBankBalance + amount);
    }

    public static void increaseInterest(final Player player, final double amount, final HashMap<Player, Timer> interestTimers) {
        final Double currentInterestRate = DBActions.loadInterestRate(player.getUniqueId());
        final Double newAmount = currentInterestRate + amount;
        DBActions.saveInterestRate(player.getUniqueId(), newAmount);

        initalizeInterestAccumulation(player, interestTimers);
        message(player, "You have increased your interest rate by " + amount + "%\nYou now have an interest rate of " + newAmount + "%");
    }

    private static void initalizeInterestAccumulation(final Player player, final HashMap<Player, Timer> interestTimers) {
        if (interestTimers.containsKey(player)) {
            interestTimers.get(player).cancel();
        }

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(
            new AccumulateInterest(player), 
            0, 
            TimeUnit.HOURS.toMillis(ConstantManager.inst().interest_period)
        );
        interestTimers.put(player, timer);
    }
}*/
