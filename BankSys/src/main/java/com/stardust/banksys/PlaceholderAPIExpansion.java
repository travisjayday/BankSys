package com.stardust.banksys;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import de.xite.scoreboard.main.PowerBoard;
import de.xite.scoreboard.utils.Teams;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    private final BankSys plugin; //

    public PlaceholderAPIExpansion(BankSys plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors()); //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "BankSys";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion(); //
    }

    /*@Override
    public boolean persist() {
        return true; //
    }*/

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("playerbalance")) {
            DecimalFormat df = new DecimalFormat("0.00");
            double bal = plugin.getBankBalance(player);
            return df.format(bal);
        }
        return null;
    }
}
