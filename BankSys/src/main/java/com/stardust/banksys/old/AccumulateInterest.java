/*package com.stardust.banksys.utils;

import java.util.TimerTask;

import org.bukkit.entity.Player;

import com.stardust.banksys.DBActions;

public class AccumulateInterest extends TimerTask {
    private Player player;

    public AccumulateInterest(Player player) {
        this.player = player;
        final double playerBankBalance = DBActions.loadBankBalance(player.getUniqueId());
        DBActions.saveBankBalance(player.getUniqueId(), playerBankBalance*(1 + ConstantManager.inst().interest_rate_per_spawner/8760));
    }

    @Override
    public void run() {
        try {
            System.out.println("Accumulating interest at a rate of " + DBActions.loadInterestRate(player.getUniqueId()));
        } catch (Exception ex) {
            System.out.println("error running thread " + ex.getMessage());
        }
    }
    
}
*/