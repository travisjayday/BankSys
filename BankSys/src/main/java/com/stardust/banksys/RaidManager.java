package com.stardust.banksys;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.stardust.banksys.gui.PVPBankerTrait;
import com.stardust.banksys.utils.ConstantManager;
import de.xite.scoreboard.api.TeamSetEvent;
import net.badbird5907.anticombatlog.AntiCombatLog;
import net.badbird5907.anticombatlog.api.events.CombatLogKillEvent;
import net.badbird5907.anticombatlog.api.events.CombatLogNPCSpawnEvent;
import net.badbird5907.anticombatlog.api.events.CombatTagEvent;
import net.badbird5907.anticombatlog.manager.NPCManager;
import net.badbird5907.anticombatlog.object.CombatNPCTrait;
import net.citizensnpcs.api.CitizensPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.bukkit.Sound.*;

public class RaidManager implements Listener {

    boolean ongoingRaid;
    BossBar raidBar;
    BossBar defenderBar;
    int vol = 1;
    int pit = 0;
    int secondsLeft;
    int initialRobbersCount;
    int initialDefendersCount;
    int defenderCooldown;
    Location leaveLoc;
    Set<UUID> robbers;
    Set<UUID> defenders;
    Set<UUID> robbersANDdefenders;
    Set<UUID> playersInBank;
    int robbersInRegion;
    int waitingForRobbersTimeout;
    boolean pvpOn;
    boolean raidCancelled;
    DummyConsoleSender dummySender;
    long timeSinceLastRaid;

    enum RaidResult {
        ROBBERS_WON,
        DEFENDERS_WON,
        ROBBER_TIMEOUT
    }

    RaidManager() {
        ongoingRaid = false;
        raidCancelled = false;
        robbersInRegion = 0;
        dummySender = new DummyConsoleSender();

        raidBar = Bukkit.createBossBar(ConstantManager.inst().bank_raid_boss_bar
                        .replace("&","§"),
                BarColor.valueOf(ConstantManager.inst().bank_raid_boss_bar_color), BarStyle.SOLID);
        raidBar.setProgress(0.0);
        defenderBar = Bukkit.createBossBar(ConstantManager.inst().bank_waiting_defenders_boss_bar
                        .replace("&","§"),
                BarColor.valueOf(ConstantManager.inst().bank_raid_boss_bar_color), BarStyle.SOLID);
        raidBar.setProgress(0.0);
        robbers = new HashSet<>();
        playersInBank = new HashSet<>();
        defenders = new HashSet<>();
        robbersANDdefenders = new HashSet<>();
        timeSinceLastRaid = 0;
    }

    public void raidEnded(RaidResult result) {
        if (result == RaidResult.ROBBER_TIMEOUT) {
            cleanup();
            return;
        }

        ConstantManager.broadcastFormattedMessage(ConstantManager.inst().bank_pvp_disabled);
        String s = result == RaidResult.ROBBERS_WON?
                ConstantManager.inst().raid_robbers_won : ConstantManager.inst().raid_defenders_won;
        double won = randRng(ConstantManager.instance.raid_win_min, ConstantManager.inst().raid_win_max);
        if (result == RaidResult.DEFENDERS_WON) won *= ConstantManager.inst().defender_win_fraction;

        DecimalFormat df = new DecimalFormat("0.00");
        s = s.replace("CASH", ConstantManager.inst().currency_sym + df.format(won));
        ConstantManager.broadcastFormattedMessage(s);

        Sound succ = ENTITY_PLAYER_LEVELUP;
        Sound fail = ENTITY_ITEM_BREAK;

        for (UUID uuid : robbersANDdefenders) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                AntiCombatLog.getInstance().clearCombatTag(player);
        }

        if (result == RaidResult.DEFENDERS_WON) {
            double per = won / initialDefendersCount;
            for (UUID uuid : defenders) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.playSound(player.getLocation(), succ, vol, pit);
                    DBActions.addPlayerBalance(uuid, per);
                    ConstantManager.sendFormattedMessage(player,
                            ConstantManager.inst().raid_defender_cash.replace("CASH",
                                    ConstantManager.inst().currency_sym + df.format(per)));
                }
            }
            for (UUID uuid : robbers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null)
                    player.playSound(player.getLocation(), fail, vol, pit);
            }
        }
        else {
            double per = won / initialRobbersCount;
            for (UUID uuid : robbers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.playSound(player.getLocation(), succ, vol, pit);
                    DBActions.addPlayerBalance(uuid, per);
                    ConstantManager.sendFormattedMessage(player,
                            ConstantManager.inst().raid_robber_cash.replace("CASH",
                                    ConstantManager.inst().currency_sym + df.format(per)));
                }
            }
            for (UUID uuid : defenders) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null)
                    player.playSound(player.getLocation(), fail, vol, pit);
            }
        }
        timeSinceLastRaid = System.currentTimeMillis();
        cleanup();
    }

    private void enablePVP(DBActions.SavedRegion region) {
        pvpOn = true;
    }

    private void updateRaidBar() {
        // If Raid got cancelled by admin, return immediately
        if (raidCancelled) {
            raidCancelled = false;
            return;
        }

        /* Show Raid bar to all players in a bank */
        if (!playersInBank.isEmpty()) {
            Set<UUID> haveBarAlready = raidBar.getPlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet());
            for (UUID uuid : playersInBank) {
                if (!haveBarAlready.contains(uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null)
                        raidBar.addPlayer(p);
                }
            }
        }

        // Waiting for robbers to join
        int min = (int) Math.round(ConstantManager.inst().minimum_robbers_for_raid);
        if (secondsLeft == (int) Math.round(ConstantManager.inst().seconds_per_raid) && robbersInRegion < min) {
            raidBar.setTitle(ConstantManager.inst().bank_waiting_raid_boss_bar
                    .replace("&","§")
                    .replace("SECONDS", "" + waitingForRobbersTimeout)
                    .replace("ROBBERS", "" + robbersInRegion)
                    .replace("MIN", "" + min));
            raidBar.setProgress(0);
            waitingForRobbersTimeout--;
            if (waitingForRobbersTimeout >= 0 && ongoingRaid)
                Bukkit.getScheduler().runTaskLater(BankSys.getInstance(), this::updateRaidBar, 20);
            else {
                raidEnded(RaidResult.ROBBER_TIMEOUT);
            }
            return;
        }

        // Call for Defenders
        if (defenderCooldown == (int) Math.round(ConstantManager.inst().seconds_waiting_for_defender_timeout)) {
            // got enough robbers. Call for defenders
            Component comp = Component.text(ConstantManager.inst().prompt_defend_bank.replace("&","§"));
            comp = comp.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/bank defend"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!robbers.contains(p.getUniqueId())) {
                    ConstantManager.sendFormattedMessage(p, ConstantManager.inst().msg_call_for_defender);
                    p.sendMessage(comp);
                }
            }
        }

        // Count Down waiting for defenders
        if (defenderCooldown > 0) {
            raidBar.setTitle(ConstantManager.inst().bank_waiting_defenders_boss_bar
                    .replace("&","§").replace("SECONDS", "" + defenderCooldown));
            defenderCooldown--;
            Bukkit.getScheduler().runTaskLater(BankSys.getInstance(), this::updateRaidBar, 20);
            return;
        }


        /* Raid starts here */
        if (secondsLeft == (int) Math.round(ConstantManager.inst().seconds_per_raid)) {
            // transitioning into raid
            DBActions.SavedRegion reg1 = DBActions.loadRegion(BankSys.BankRegion.pvp1);
            DBActions.SavedRegion reg2 = DBActions.loadRegion(BankSys.BankRegion.pvp2);
            if (reg1 == null) {
                if (reg2 == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "Fatal PVP regions not defined!");
                    return;
                }
                reg1 = reg2;
                enablePVP(reg2);
            }
            else {
                enablePVP(reg1);
                if (reg2 == null)
                    reg2 = reg1;
                else
                    enablePVP(reg2);
            }
            ConstantManager.broadcastFormattedMessage(ConstantManager.inst().bank_pvp_enabled);

            for (UUID defender : defenders) {
                deploy(Bukkit.getPlayer(defender), Math.random() > 0.5 ? reg1 : reg2);
                Player player = Bukkit.getPlayer(defender);
                if (player != null) {
                    player.playSound(player.getLocation(), ENTITY_PLAYER_TELEPORT, vol, pit);
                    ConstantManager.sendFormattedMessage(player, ConstantManager.inst().pvp_tagged.replace("SECONDS", ""+secondsLeft));
                }
                AntiCombatLog.silent_tag(defender, secondsLeft);
            }

            for (UUID robber : robbers) {
                Player player = Bukkit.getPlayer(robber);
                if (player != null) {
                    player.playSound(player.getLocation(), ENTITY_PLAYER_TELEPORT, vol, pit);
                    ConstantManager.sendFormattedMessage(player, ConstantManager.inst().pvp_tagged.replace("SECONDS", ""+secondsLeft));
                }
                AntiCombatLog.silent_tag(robber, secondsLeft);
            }

            initialRobbersCount = robbers.size();
            initialDefendersCount = defenders.size();

            DBActions.SavedRegion entrance = DBActions.loadRegion(BankSys.BankRegion.entrance1);
            assert entrance != null;
            leaveLoc = new Location(entrance.pos1.getWorld(),
                    (entrance.pos1.getBlockX() + entrance.pos2.getBlockX()) / 2.0,
                    (entrance.pos1.getBlockY() + entrance.pos2.getBlockY()) / 2.0,
                    (entrance.pos1.getBlockZ() + entrance.pos2.getBlockZ()) / 2.0);
            leaveLoc.add(entrance.facing.multiply(-2).setY(1));
        }

        // ensure players in bank get teleported away during a Raid
        if (!playersInBank.isEmpty()) {
            Set<UUID> toRemove = new HashSet<>();
            for (UUID uuid : playersInBank) {
                if (!robbersANDdefenders.contains(uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.teleport(leaveLoc);
                        ConstantManager.sendFormattedMessage(p, ConstantManager.inst().removed_from_bank);
                        toRemove.add(uuid);
                        raidBar.removePlayer(p);
                    }
                }
            }
            for (UUID uuid : toRemove) {
                playersInBank.remove(uuid);
            }
        }

        if (robbersInRegion == 0) {
            raidEnded(RaidResult.DEFENDERS_WON);
            return;
        }

        if (defenders.isEmpty()) {
            raidEnded(RaidResult.ROBBERS_WON);
        }

        String s = ConstantManager.inst().bank_raid_boss_bar
                .replace("&","§").replace("SECONDS", "" + secondsLeft);
        s = s.replace("ROBBERS", ""+robbersInRegion).replace("TOTAL", ""+ initialRobbersCount);
        raidBar.setTitle(s);
        raidBar.setProgress(secondsLeft / ConstantManager.inst().seconds_per_raid);
        if (secondsLeft != 0) {
            secondsLeft--;
            if (ongoingRaid)
                Bukkit.getScheduler().runTaskLater(BankSys.getInstance(), this::updateRaidBar, 20);
        }
        else {
            raidEnded(RaidResult.ROBBERS_WON);
        }
    }
    private double randRng(double min, double max) {
        return min + (max - min) * Math.random();
    }

    private void deploy(Player player, DBActions.SavedRegion reg) {
        double xmin = Math.min(reg.pos1.getBlockX(), reg.pos2.getBlockX())+1;
        double xmax = Math.max(reg.pos1.getBlockX(), reg.pos2.getBlockX())-1;
        double x = randRng(xmin, xmax);
        double ymin = Math.min(reg.pos1.getBlockY(), reg.pos2.getBlockY())+1;
        double ymax = Math.max(reg.pos1.getBlockY(), reg.pos2.getBlockY())-1;
        double y = randRng(ymin, ymax);
        double zmin = Math.min(reg.pos1.getBlockZ(), reg.pos2.getBlockZ())+1;
        double zmax = Math.max(reg.pos1.getBlockZ(), reg.pos2.getBlockZ())-1;
        double z = randRng(zmin, zmax);

        Location loc = new Location(reg.pos1.getWorld(), x, y, z);
        while (!loc.getWorld().getBlockAt(loc).isEmpty())
            loc.add(0, 1, 0);
        player.teleport(loc);
    }

    private void deployCenter(Player player, DBActions.SavedRegion reg) {
        double x = (reg.pos1.getBlockX() + reg.pos2.getBlockX()) / 2.0;
        double z = (reg.pos1.getBlockZ() + reg.pos2.getBlockZ()) / 2.0;
        double y = (reg.pos1.getBlockY() + reg.pos2.getBlockY()) / 2.0;
        Location loc = new Location(reg.pos1.getWorld(), x, y, z);
        if (loc.getWorld() == null) Bukkit.getServer().broadcast(Component.text("Banksys misconfigured! Regions misconfigured!"));
        while (!loc.getWorld().getBlockAt(loc).isEmpty())
            loc.add(0, 1, 0);
        player.teleport(loc);
    }

    public void onCommandRaidCancel(Player player) {
        if (ongoingRaid) {
            raidCancelled = true;
            cleanup();
        }
    }

    public void onCommandRaidStart() {
        if (ongoingRaid && defenderCooldown > 0) {
            defenderCooldown = 1;
        }
    }

    private void setDefenderPrefix(UUID uuid) {
        TagManager.setPrefix(uuid, ConstantManager.inst().defender_prefix.replace("&", "§") + " ");
    }

    private void setRobberPrefix(UUID uuid) {
        TagManager.setPrefix(uuid, ConstantManager.inst().robber_prefix.replace("&", "§") + " ");
    }

    public void onCommandBankDefend(Player player) {
        if (ongoingRaid) {
            if (defenderCooldown == 0) {
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().join_raid_error);
                return;
            }
            if (defenders.size() < ConstantManager.inst().maximum_defenders_for_raid) {
                setDefenderPrefix(player.getUniqueId());
                DBActions.SavedRegion reg = DBActions.loadRegion(BankSys.BankRegion.waitroom);
                if (reg != null) {
                    deployCenter(player, reg);
                    raidBar.addPlayer(player);
                    defenders.add(player.getUniqueId());
                    robbersANDdefenders.add(player.getUniqueId());
                }
                else {
                    Bukkit.broadcast(Component.text("Error. Define waitroom with /bank waitroom"));
                }
            }
            else {
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().msg_too_many_defenders);
            }
        }
    }

    public void onExitedPVP(Player player) {
        if (robbersANDdefenders.contains(player.getUniqueId())) {
            // robber trying to escape before raid starts
            DBActions.SavedRegion reg1 = DBActions.loadRegion(BankSys.BankRegion.pvp1);
            DBActions.SavedRegion reg2 = DBActions.loadRegion(BankSys.BankRegion.pvp2);
            if (reg1 != null) deployCenter(player, reg1);
            else if (reg2 != null) deployCenter(player, reg2);
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().raid_robber_try_escape);
        }
    }

    public void onExitedBank(Player player) {
        playersInBank.remove(player.getUniqueId());
        raidBar.removePlayer(player);
    }

    public void onEnteredBank(Player player) {
        if (pvpOn) {
            if (!robbersANDdefenders.contains(player.getUniqueId())) {
                player.teleport(leaveLoc);
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().cannot_enter_bank);
            }
        }
        else {
            playersInBank.add(player.getUniqueId());
        }
    }

    public void startRaid(Player player) {
        ConstantManager.sendFormattedMessage(player, ConstantManager.inst().pvpbanker_hurt);

        long deltaMinutes = ((System.currentTimeMillis() - timeSinceLastRaid) / 1000) / 60;
        if (deltaMinutes < ConstantManager.inst().minutes_until_next_raid) {
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().raided_too_recently);
            return;
        }

        if (pvpOn || defenders.contains(player.getUniqueId())) return;

        if (!BankRegionSelector.inRegion(player.getLocation(), DBActions.loadRegion(BankSys.BankRegion.pvp1)) &&
                !BankRegionSelector.inRegion(player.getLocation(), DBActions.loadRegion(BankSys.BankRegion.pvp2))) {
            return;
        }

        if (robbers.size() < ConstantManager.inst().maximum_robbers_for_raid) {
            if (!robbers.contains(player.getUniqueId())) {
                setRobberPrefix(player.getUniqueId());

                raidBar.addPlayer(player);
                raidBar.setVisible(true);
                robbers.add(player.getUniqueId());
                robbersANDdefenders.add(player.getUniqueId());
                robbersInRegion++;
            }
        }
        else {
            if (!robbers.contains(player.getUniqueId()))
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().msg_too_many_robbers);
        }

        if (!ongoingRaid) {
            waitingForRobbersTimeout = (int) Math.round(ConstantManager.inst().seconds_waiting_for_robber_timeout);
            defenderCooldown = (int) Math.round(ConstantManager.inst().seconds_waiting_for_defender_timeout);
            secondsLeft = (int) Math.round(ConstantManager.inst().seconds_per_raid);
            ongoingRaid = true;
            pvpOn = false;
            updateRaidBar();
        }
    }

    @EventHandler
    void onTeamSetEvent(TeamSetEvent event) {
        /*Bukkit.getLogger().info("TEAM SET EVENT");
        Player player = event.getPlayer();
        player.sendMessage("TEAM SET");
        if (robbers.contains(player.getUniqueId())) {
            event.setPrefix(ConstantManager.inst().robber_prefix);
        }*/
    }

    void cleanup() {
        ongoingRaid = false;
        pvpOn = false;
        for (UUID robber : robbers) {
            TagManager.resetPrefix(robber);
        }

        for (UUID defender : defenders) {
            TagManager.resetPrefix(defender);
        }

        robbersInRegion = 0;
        raidBar.removeAll();
        defenderBar.removeAll();
        defenders.clear();
        robbers.clear();
        playersInBank.clear();
        robbersANDdefenders.clear();
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "pb reload");
    }

    void fullcleanup() {
        cleanup();
    }

    @EventHandler
    void onPVP(DisallowedPVPEvent event) {
        Location defenderLoc = event.getDefender().getLocation();
        if (ongoingRaid && pvpOn
                && robbersANDdefenders.contains(event.getAttacker().getUniqueId())
                && robbersANDdefenders.contains(event.getDefender().getUniqueId())) {

            if (BankRegionSelector.inRegion(defenderLoc, DBActions.loadRegion(BankSys.BankRegion.pvp1)) ||
                    BankRegionSelector.inRegion(defenderLoc, DBActions.loadRegion(BankSys.BankRegion.pvp2))) {
                // unblock PVP
                event.setCancelled(true);
            }
        }
    }

    void removePlayerFromRaid(@Nullable Player player, UUID uuid, boolean teleportAway) {
        if (ongoingRaid && robbersANDdefenders.contains(uuid)) {
            if (teleportAway && player != null) player.teleport(leaveLoc);
            robbersANDdefenders.remove(uuid);
            playersInBank.remove(uuid);
            if (player != null)
                raidBar.removePlayer(player);
            if (robbers.contains(uuid)) {
                robbers.remove(uuid);
                robbersInRegion--;
                //robbersTeam.removePlayer(player);
            }
            else {
                defenders.remove(uuid);
                //defendersTeam.removePlayer(player);
            }
            TagManager.resetPrefix(uuid);
            //restoreTeams(player.getUniqueId());
        }
    }

    public boolean isInRaid(UUID uuid) {
        return robbersANDdefenders.contains(uuid);
    }

    public String getCurrentPrefix(UUID uuid) {
        if (robbers.contains(uuid))
            return ConstantManager.inst().robber_prefix.replace("&", "§") + " ";
        else if (defenders.contains(uuid))
            return ConstantManager.inst().defender_prefix.replace("&", "§") + " ";
        else
            return null;
    }

    @EventHandler
    void onExit(PlayerQuitEvent event) {
        //removePlayerFromRaid(event.getPlayer(), true);
        TagManager.resetPrefix(event.getPlayer().getUniqueId());
    }

    @EventHandler
    void onCombatLogKillEvent(CombatLogKillEvent event) {
        removePlayerFromRaid(null, event.getPlayer(), false);
    }

    @EventHandler
    void onCombatTaggedEvent(CombatTagEvent event) {
        if (isInRaid(event.getAttacker().getUniqueId()) || isInRaid(event.getVictim().getUniqueId())) {
            event.setCancelled(true);
        }
        if (event.getVictim().hasMetadata("NPC") && NPCManager.getNPCRegistry().getNPC(event.getVictim()).hasTrait(PVPBankerTrait.class)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onCombatLogNPCSpawnEvent(CombatLogNPCSpawnEvent event) {
        if (isInRaid(event.getPlayer().getUniqueId())) {
            event.setTime(secondsLeft);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        removePlayerFromRaid(event.getPlayer(), event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (robbersANDdefenders.contains(player.getUniqueId())) {
            if (robbers.contains(player.getUniqueId())) {
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().resume_raid_as_robber);
                raidBar.addPlayer(player);
                Bukkit.getScheduler().runTaskLater(BankSys.getInstance(), () -> setRobberPrefix(player.getUniqueId()), 20*4);
            }
            else {
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().resume_raid_as_defender);
                raidBar.addPlayer(player);
                Bukkit.getScheduler().runTaskLater(BankSys.getInstance(), () -> setDefenderPrefix(player.getUniqueId()), 20*4);
            }
        }
        else {
            if (defenderCooldown > 0) {
                Component comp = Component.text(ConstantManager.inst().prompt_defend_bank.replace("&","§"));
                comp = comp.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/bank defend"));
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().msg_call_for_defender);
                player.sendMessage(comp);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (robbersANDdefenders.contains(player.getUniqueId())) {
            player.sendMessage(event.getMessage());
            if (player.hasPermission("banksys.admin")) {
                return;
            }
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().command_denied_in_raid);
            event.setCancelled(true);
        }
    }
}
