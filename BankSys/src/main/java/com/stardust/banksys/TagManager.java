package com.stardust.banksys;

import de.xite.scoreboard.utils.Teams;
import de.xite.scoreboard.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import java.util.*;

import static de.xite.scoreboard.modules.ranks.RankManager.setPrefixSuffix;

public class TagManager {

    public Map<UUID, String> oldPrefixes;

    private static TagManager instance;

    private TagManager() {
        oldPrefixes = new HashMap<>();
    }

    private static TagManager getInstance() {
        if (instance == null)
            instance = new TagManager();
        return instance;
    }

    static void setPrefixInternal(UUID uuid, String prefix) {

    }
    static void setPrefix(UUID uuid, String prefix) {
        setPrefix(uuid, prefix, false);
    }

    static void setPrefix(UUID uuid, String prefix, boolean reset) {
        if (getInstance().oldPrefixes.containsKey(uuid)) return;
        Player p = Bukkit.getPlayer(uuid);
        Teams teams = Teams.get(p);
        if (teams != null) {
            ChatColor nameColor = teams.getNameColor();
            for (Player all : Bukkit.getOnlinePlayers()) {
                Team t = all.getScoreboard().getTeam(teams.getTeamName());
                if (t == null)
                    t = all.getScoreboard().registerNewTeam(teams.getTeamName());

                if (!reset)
                    getInstance().oldPrefixes.put(uuid, teams.getPrefix());
                setPrefixSuffix(p, t, prefix, teams.getSuffix(), teams.getPlayerListName());

                if(nameColor != null && Version.isAbove_1_13())
                    t.setColor(nameColor);
                t.addEntry(p.getName());
            }
        }
    }

    static void resetPrefix(UUID uuid) {
        Bukkit.getLogger().info("Resetting prefix...");
        String pastPrefix = getInstance().oldPrefixes.get(uuid);
        if (pastPrefix != null)
            setPrefix(uuid, pastPrefix, true);
        getInstance().oldPrefixes.remove(uuid);
    }
}
