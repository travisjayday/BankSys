package com.stardust.banksys.utils;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

public class ConstantManager {
    public String mysql_host = "127.0.0.1";
    public String mysql_user = "minecraft";
    public String mysql_password = "minecraftIsCool123!";
    public String mysql_database = "mcsql";
    public Integer mysql_port = 3306;
    public Double interest_rate_per_spawner = 0.1;
    public Double interest_period_in_minutes = 60.0;
    public Double minimum_robbers_for_raid = 2.0;
    public Double maximum_robbers_for_raid = 4.0;
    public Double maximum_defenders_for_raid = 4.0;
    public Double seconds_per_raid = 300.0;
    public Double minutes_until_next_raid = 2.0;
    public Double seconds_waiting_for_robber_timeout = 30.0;
    public Double seconds_waiting_for_defender_timeout = 30.0;
    public Double raid_win_min = 10.0;
    public Double raid_win_max = 100.0;
    public Double defender_win_fraction = 0.2;
    public Double rank_default_max_interest = 1.0;
    public String rank_low_perm = "rank.obsidian";
    public Double rank_low_max_interest = 1.3;
    public String rank_medium_perm = "rank.vip";
    public Double rank_medium_max_interest = 1.6;
    public String rank_high_perm = "rank.titan";
    public Double rank_high_max_interest = 2.0;
    public String deposit_icon = Material.CRAFTING_TABLE.toString();
    public String withdraw_icon = Material.FURNACE.toString();
    public String interest_icon = Material.BIRCH_PLANKS.toString();
    public String payday_command = "&l&6Der nächste PayDay ist in";
    public String payday_broadcast = "&l&6[PayDay] &eDein stündlicher Payday beträgt SUMME auf ZINS%";
    public String interest_rate_str = "Zinssatz:";
    public String balance_str = "Kontostand:";
    public String deposited_str = "&l&5Eingezahlt";
    public String withdrew_str = "&l&5Abgehoben";
    public String deposit_str = "Einzahlen";
    public String withdraw_str = "Abheben";
    public String interest_str = "Zinsen";
    public String cannot_deposit = "&c&lNicht genug Guthaben für diese Einzahlung";
    public String cannot_withdraw = "&c&lNicht genug Guthaben für diese Abhebung";
    public String bank_admin_on = "&c&lDu bist jetzt Bank-Admin und kannst PLAYERNAME's Bank Konto verwalten!";
    public String bank_admin_off = "&c&lDu bist NICHT mehr Bank-Admin";
    public String currency_sym = "$";
    public String spawner_interest_lore = "Dieser Spawner erhöht deinen Zins\nFüge mehr hinzu für mehr Zinsen";
    public String selection_prompt_entrace1 = "&lBank &nEingang1 wählen";
    public String selection_prompt_entrace2 = "&lBank &nEingang2 wählen";
    public String selection_prompt_pvp1 = "&lBankbereich auswählen &nPVP1";
    public String selection_prompt_pvp2 = "&lBankbereich auswählen &nPVP2";
    public String selection_prompt_waitroom = "&lWartezone auswählen";
    public String selection_prompt_usage = "&5&lPosition1: &fLinksklick\n&5&lPosition2: &fRechtsklick\n&5&lFertig: &fShift + Linksklick";
    public String selection_wand_confirmed = "&5&lAuswahl bestätigt!";
    public String selection_wand_failed = "&c&lDu musst zuerst Positionen auswählen, bevor du bestätigst!";
    public String selection_wand_lore = "Linksklick für pos1\nRechtsklick für pos2\nShift+Linksklick zum Abschließen";
    public String bank_leave_bye = "&l&6[Server] &eAuf Wiedersehen, komm wieder!";
    public String bank_enter_welcome = "&l&6[Server] &eWillkommen bei &f&nDer Bank!";
    public String robber_prefix = "&c&l[Räuber]";
    // English
    public String defender_prefix = "&9&l[Verteidiger]";
    public String bank_raid_boss_bar = "&l&4[&c&lSECONDSs&4&l] &f&lBanküberfall &c&lROBBERS/TOTAL&f&l Räuber am Leben";
    public String bank_waiting_raid_boss_bar = "&l&4[&c&lSECONDSs&l&4] &f&lWarten auf mehr Räuber... &c&lROBBERS/MIN";
    public String bank_waiting_defenders_boss_bar = "&l&4[&c&lSECONDSs&l&4] &f&lWarten auf Verteidiger...";
    public String msg_too_many_robbers = "Sorry, es gibt bereits zu viele Räuber in diesem Überfall!";
    public String msg_too_many_defenders = "Sorry, es gibt bereits zu viele Verteidiger in diesem Überfall!";
    public String bank_raid_boss_bar_color = "RED";
    public String prompt_defend_bank = "&c&l&nKlicke hier, um die Bank zu verteidigen!";
    public String msg_call_for_defender = "Die Bank wird angegriffen!";
    public String bank_pvp_enabled = "&c&lPVP ist AKTIVIERT in &f&nDer Bank";
    public String raid_robbers_won = "&c&lDie Räuber haben den Überfall GEWONNEN! &f\nDie Gewinner teilen sich CASH!";
    public String raid_robber_cash = "&5&lDu hast CASH erhalten";
    public String raid_defender_cash = "&5&lDu hast CASH erhalten";
    public String pvpbanker_interact = "&r&aWenn du und deine Freunde mich &c&langreifen&a kannst du diese Bank &c&lüberfallen&a!";
    public String pvpbanker_hurt = "&c&lOuch!";
    public String raid_defenders_won = "&c&lDie Verteidiger haben die Bank verteidigt!\n&fDie Gewinner teilen sich CASH!";
    public String join_raid_error = "&c&lSorry, der Überfall läuft bereits!";
    public String bank_pvp_disabled = "&a&lPVP ist DEAKTIVIERT in &f&nDer Bank";
    public String command_denied_in_raid = "&c&lDu kannst während eines Banküberfalls keine Befehle ausführen";
    public String removed_from_bank = "&c&lDu wurdest von der Bank entfernt, weil ein Banküberfall begonnen hat";
    public String cannot_enter_bank = "&c&lDu kannst die Bank während eines Überfalls nicht betreten";
    public String raid_robber_try_escape = "&c&lDu kannst während eines Überfalls nicht entkommen";
    public String cant_increase_interest = "&c&lDu hast den höchsten Zinssatz erreicht, der mit deinem Rang erreichbar ist.";
    public String resume_raid_as_robber = "&c&lDer Banküberfall wirld als Räuber fortgesetzt";
    public String resume_raid_as_defender = "&9&lDer Banküberfall wird als Verteidiger fortgesetzt";
    public String raided_too_recently = "&c&lDer letzte Banküberfall fand vor Kurzem statt. Warte etwas, bevor der nächste Überfall stattfinden kann.";
    public String pvp_tagged = "&c&lYou have been PVP tagged for SECONDS seconds.";
    public String player_not_found = "&c&lSpieler \"PLAYERNAME\" konnte nicht gefunden werden!";
    public String plugin_usage = "Nutzung des Banking-Plugins:\n" +
            "/bank entranceN - Bank Eingang N=1,2 wählen\n" +
            "/bank pvpN      - PVP-Bereich der Bank N=1,2 wählen\n" +
            "/bank waitroom  - Wartezone für PVP wählen\n" +
            "/bank bankernpc - Einen Bankier-NPC spawnen\n" +
            "/bank admin NAME  - Konto bearbeiten von spieler NAME";
    /*public String payday_broadcast = "&l&6[Server] &ePayDay at &f&nThe Bank!";
    public Double interest_rate_per_spawner = 0.1;
    public Double interest_period_in_minutes = 2.0;
    public String interest_rate_str = "Interest Rate:";
    public String balance_str = "Bank Balance:";
    public String deposited_str = "&l&5Deposited";
    public String withdrew_str = "&l&5Withdrew";
    public String deposit_str = "Deposit";
    public String withdraw_str = "Withdraw";
    public String interest_str = "Interest";
    public String cannot_deposit = "&c&lYou have insufficient funds for this deposit";
    public String cannot_withdraw = "&c&lYou have insufficient funds for this withdrawal";
    public String bank_admin_on = "&c&lYou are now Bank Admin!";
    public String bank_admin_off = "&c&lYou are NOT Bank Admin anymore";
    public String currency_sym = "$";

    public String spawner_interest_lore = "This spawner increases your interest\nAdd more for more interest";

    public String selection_prompt_entrace1 = "&lSelect Bank &nEntrance1";
    public String selection_prompt_entrace2 = "&lSelect Bank &nEntrance2";
    
    public String selection_prompt_pvp1 = "&lSelect Bank Region &nPVP1";
    public String selection_prompt_pvp2 = "&lSelect Bank Region &nPVP2";
    public String selection_prompt_waitroom = "&lSelect Region Waitroom";
    public String selection_prompt_usage = "&5&lPosition1: &fLeft Click\n&5&lPosition2: &fRight Click\n&5&lFinish: &fShift + Left Click";

    public String selection_wand_confirmed = "&5&lSelection Done!";
    public String selection_wand_failed = "&c&lYou need to select positions before confirming!";
    public String selection_wand_lore = "Left click for pos1\nRight for pos2\nShift+LeftClick to Finish";

    public String bank_leave_bye = "&l&6[Server] &eBye bye, come again!";
    public String bank_enter_welcome = "&l&6[Server] &eWelcome to &f&nThe Bank!";
    public String plugin_usage =
            "Banking Plugin Usage:\n" +
                    "/bank entranceN - Select Bank Entrance N=1,2\n" +
                    "/bank pvpN      - Select Bank PVP Region N=1,2\n" +
                    "/bank waitroom  - Select Waitroom for PVP\n" +
                    "/bank bankernpc - Spawns a Banker NPC\n" +
                    "/bank admin     - Toggle allow removing spawners from Banker";

    public String defender_prefix = "&9&l[Defender]";
    public String bank_raid_boss_bar = "&l&4[&c&lSECONDSs&4&l] &f&lBank Raid &c&lROBBERS/TOTAL&f&l Robbers Alive";
    public String bank_waiting_raid_boss_bar = "&l&4[&c&lSECONDSs&l&4] &f&lWaiting for more Robbers... &c&lROBBERS/MIN";
    public String bank_waiting_defenders_boss_bar = "&l&4[&c&lSECONDSs&l&4] &f&lWaiting for defenders...";
    public String msg_too_many_robbers = "Sorry, there are too many robbers in this raid already!";
    public String msg_too_many_defenders = "Sorry, there are too many defenders in this raid already!";
    public String bank_raid_boss_bar_color = "RED";
    public String prompt_defend_bank = "&c&l&nClick here to defend the Bank!";
    public String msg_call_for_defender = "The bank is under attack!";
    public String bank_pvp_enabled = "&c&lPVP is ON in &f&nThe Bank";
    public String raid_robbers_won = "&c&lThe Robbers have WON the raid! &f\nThe winners are splitting CASH!";
    public String raid_robber_cash = "&5&lYou received CASH";
    public String raid_defender_cash = "&5&lYou received CASH";
    public String raid_robber_try_escape = "&c&lYou can't escape yet! Waiting for defenders...";
    public String pvpbanker_interact = "&r&aIf you and your friends &c&lAttack&a me, I'm afraid you can &c&lRob&a this bank!";
    public String pvpbanker_hurt = "&c&lOuch!";
    public String raid_defenders_won = "&c&lThe Defenders defended The Bank!\n&fThe winners are splitting CASH!";
    public String join_raid_error = "&c&lSorry the raid is already ongoing!";
    public String bank_pvp_disabled = "&a&lPVP is OFF in &f&nThe Bank";

                    */

    /*public String command_denied_in_raid = "&c&lYou can't run commands during a Bank Raid";
    public String removed_from_bank = "&c&lYou have been removed from the bank because a Bank Raid has begun";
    public String cannot_enter_bank = "&c&lYou can't enter the bank during a Raid";
    public String raid_robber_try_escape = "&c&lYou can't escape the Bank during a Raid";
    public String cant_increase_interest = "&c&lYou've reached the highest interest rate attainable with your Rank.";*/


    public ConstantManager(Plugin plugin) {
        instance = this;
        initConstants(plugin);
    }
    public static ConstantManager instance;
    public static ConstantManager inst() {
        return instance;
    }
    public void initConstants(Plugin plugin) {
        try {
            final Class<ConstantManager> yourClass = ConstantManager.class;
            final Field[] fields = yourClass.getFields();

            for (Field f : fields) {
                String name = f.getName();
                Object value = f.get(this);

                switch (f.getType().getName()) {
                    case "java.lang.String":
                        value = plugin.getConfig().getString(name, (String) value);
                        break;
                    case "java.lang.Double":
                        value = plugin.getConfig().getDouble(name, (Double) value);
                        break;
                    case "java.lang.Integer":
                        value = plugin.getConfig().getInt(name, (Integer) value);
                        break;
                    default: continue;
                }
                plugin.getConfig().set(name, value);
                f.set(this, value);
            }
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().info(e.toString());
        }
    }

    public static void sendFormattedMessage(Player player, String message) {
        for (String s : message.split("\n"))
            player.sendMessage(Component.text(s.replace('&', '§')));
    }

    public static void broadcastFormattedMessage(String message) {
        for (String s : message.split("\n"))
            Bukkit.getServer().broadcast(Component.text(s.replace('&', '§')));
    }
}
