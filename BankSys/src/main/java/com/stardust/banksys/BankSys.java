package com.stardust.banksys;

import com.stardust.banksys.gui.BankerTrait;
import com.stardust.banksys.gui.PVPBankerTrait;
import com.stardust.banksys.utils.Cmd;
import com.stardust.banksys.utils.ConstantManager;
import com.stardust.banksys.utils.Tabinator;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.stardust.banksys.BankSys.PluginCommands.*;

public final class BankSys extends JavaPlugin implements CommandExecutor {

    private BankRegionSelector selector;
    @Getter
    private static BankSys instance;
    private static Economy econ;
    public RaidManager raidManager;

    public static class BankAdminEditSession {
        public UUID admin;
        public UUID target;
        BankAdminEditSession(UUID admin, UUID target) {
            this.admin = admin;
            this.target = target;
        }
    }

    private List<BankAdminEditSession> bankAdminEditSessions;

    private ConstantManager constantManager;

    public Set<Inventory> sensitiveInventories;

    private static FileConfiguration config;
    protected enum PluginCommands {
        bank,
        bank_entrance1,
        bank_entrance2,
        bank_pvp1,
        bank_pvp2,
        bank_waitroom,
        bank_bankernpc,
        bank_pvpbankernpc,
        bank_admin,
        bank_admin_ONLINEPLAYER,
        bank_defend,
        bank_raid,
        bank_raid_cancel,
        bank_raid_start,
        bank_spawner,
        bank_spawner_give,
        bank_spawner_give_ONLINEPLAYER,
        bank_spawner_give_ONLINEPLAYER_NUMBER
    }

    public Map<PluginCommands, String> requiredPermMap = new HashMap<>();

    protected enum BankRegion {
        entrance1,
        entrance2,
        pvp1,
        pvp2,
        waitroom
    }

    Tabinator tabinator;
    boolean failed = false;
    String failureReason = "";
    long nextPayday;

    private boolean checkDependency(String name) {
        if (getServer().getPluginManager().getPlugin(name) == null
                || !getServer().getPluginManager().getPlugin(name).isEnabled()) {
            getLogger().log(Level.SEVERE, name + " not found or not enabled");
            getLogger().log(Level.SEVERE, "Disabling...");
            failed = true;
            failureReason = name;
            return false;
        }
        return true;
    }

    public boolean isInRaid(UUID uuid) {
        return raidManager.isInRaid(uuid);
    }

    public String getCurrentPrefix(UUID uuid) {
        return raidManager.getCurrentPrefix(uuid);
    }

    @Override
    public void onEnable() {

        requiredPermMap.put(  bank_admin, "bank.admin");
        requiredPermMap.put(  bank_admin_ONLINEPLAYER, "bank.admin");
        requiredPermMap.put(  bank_pvp1, "bank.admin");
        requiredPermMap.put(  bank_pvp2, "bank.admin");
        requiredPermMap.put(  bank_entrance1, "bank.admin");
        requiredPermMap.put(  bank_entrance2, "bank.admin");
        requiredPermMap.put(  bank_bankernpc, "bank.admin");
        requiredPermMap.put(  bank_pvpbankernpc, "bank.admin");
        requiredPermMap.put(  bank_raid, "bank.admin");
        requiredPermMap.put(  bank_spawner, "bank.admin");
        requiredPermMap.put(  bank_spawner_give_ONLINEPLAYER_NUMBER, "bank.admin");
        requiredPermMap.put(bank_spawner_give, "bank.admin");
        requiredPermMap.put(bank_spawner_give_ONLINEPLAYER, "bank.admin");
        // Plugin startup logic
        config = getConfig();
        Bukkit.getLogger().info("[BankSys] Enabling BankSys....");
        getCommand("bank").setExecutor(this);

        if (checkDependency("Citizens")) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory()
                    .registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(BankerTrait.class));
            net.citizensnpcs.api.CitizensAPI.getTraitFactory()
                    .registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(PVPBankerTrait.class));
        }
        if (checkDependency("Vault")) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                getLogger().log(Level.SEVERE, "Failed to find a Vault Economy. You can't run Vault by itself without an Economy!");
                failed = true;
                failureReason = "Vault";
                return;
            }
            econ = rsp.getProvider();
        }
        checkDependency("WorldEdit");
        checkDependency("LuckPerms");
        checkDependency("PowerBoard");

        if (failed) return;

        config.options().copyDefaults(true);
        constantManager = new ConstantManager(this);
        tabinator = new Tabinator(PluginCommands.class);
        tabinator.setReqPerms(requiredPermMap);

        saveConfig();

        if (!DBActions.enableDB()) {
            Bukkit.getServer().broadcast(Component.text("[BankSys] Could not connect to MySQL DB!"));
            getLogger().log(Level.SEVERE, "Failed to connect to SQL DB! Please edit the config.yaml to set up the right credentials!");
            failed = true;
            return;
        }

        bankAdminEditSessions = new ArrayList<>();
        instance = this;
        sensitiveInventories = new HashSet<>();

        DecimalFormat df = new DecimalFormat("0.00");
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    double rate = DBActions.loadSpawners(player.getUniqueId()) * ConstantManager.inst().interest_rate_per_spawner;
                    double balance = DBActions.loadBankBalance(player.getUniqueId());
                    double newBalance = balance * (1 + rate/100);
                    DBActions.saveBankBalance(player.getUniqueId(), newBalance);
                    String t1 = ConstantManager.inst().payday_broadcast.replace("ZINS", df.format(rate));
                    String t2 = t1.replace("SUMME", df.format(newBalance-balance));
                    ConstantManager.sendFormattedMessage(player, t2);
                }
                nextPayday = Math.round(System.currentTimeMillis() + ConstantManager.inst().interest_period_in_minutes * 60 * 1000);
            }
        }.runTaskTimer(this, 20, Math.round(ConstantManager.inst().interest_period_in_minutes * 60 * 20));
        nextPayday = Math.round(System.currentTimeMillis() + ConstantManager.inst().interest_period_in_minutes * 60 * 1000);

        selector = new BankRegionSelector();
        getServer().getPluginManager().registerEvents(selector, this);
        raidManager = new RaidManager();
        getServer().getPluginManager().registerEvents(raidManager, this);

        if (!failed && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new PlaceholderAPIExpansion(this).register();
        }
    }

    public boolean isBankAdmin(UUID uuid) {
        return bankAdminEditSessions.stream().anyMatch(session -> session.admin.equals(uuid));
    }

    public static FileConfiguration getBankSysConfig() {
        return config;
    }

    public static Economy getEconomy() {
        return econ;
    }


    /** Called when tab completion is required. Given a list of current arguments, returns a list
     * of suggested strings for tab completion. Just let Tabinator auto handle the logic for this */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return tabinator.onTabComplete(sender, cmd, alias, args);
    }

    /**
     * Base Command Handler
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        final Player player = (Player) sender;
        if (failed) {
            player.sendMessage("BankSys failed to load. Please check server logs for dependency issues!");
            player.sendMessage("Plugin \"" + failureReason + "\" is probably the reason");
            return true;
        }
        Bukkit.getLogger().info(command.getName().toString());

        if (command.getName().equals("payday")) {
            this.onCommandPayday(player);
            return true;
        }

        Cmd.build(tabinator.getReqPerms())
                .addCase(bank,              () -> this.onCommandBank(player))
                .addCase(bank_entrance1,    () -> selector.onCommandSelectRegion(player, BankRegion.entrance1))
                .addCase(bank_entrance2,    () -> selector.onCommandSelectRegion(player, BankRegion.entrance2))
                .addCase(bank_pvp1,         () -> selector.onCommandSelectRegion(player, BankRegion.pvp1))
                .addCase(bank_pvp2,         () -> selector.onCommandSelectRegion(player, BankRegion.pvp2))
                .addCase(bank_waitroom,     () -> selector.onCommandSelectRegion(player, BankRegion.waitroom))
                .addCase(bank_bankernpc,    () -> this.onCommandBanker(player))
                .addCase(bank_pvpbankernpc, () -> this.onCommandPVPBanker(player))
                .addCase(bank_admin,        this::onCommandBankAdmin)
                .addCase(bank_spawner, Cmd.build(tabinator.getReqPerms())
                        .addCase(bank_spawner_give, this::onCommandSpawner)
                        )
                .addCase(bank_defend,       () -> raidManager.onCommandBankDefend(player))
                .addCase(bank_raid, Cmd.build(tabinator.getReqPerms())
                        .addCase(bank_raid_cancel,  () -> raidManager.onCommandRaidCancel(player))
                        .addCase(bank_raid_start,   () -> raidManager.onCommandRaidStart())
                        .addDefaultCase(()             -> messageGeneralUsage(player)))
                /* Example on how to add subcommands
                .addCase(base_restore, Cmd.build()
                        .addCase(base_restore,      () -> this.onCommandBaseRestore(player, true))
                        .addCase(base_restore_now,  () -> this.onCommandBaseRestore(player, false))
                        .addDefaultCase(()             -> messageRestoreUsage(player)))
                .addCase(base_delete, Cmd.build()
                        .addCase(base_delete,       () -> this.onCommandBaseDelete(player, true))
                        .addCase(base_delete_now,   () -> this.onCommandBaseDelete(player, false))
                        .addDefaultCase(()             -> messageDeleteUsage(player)))*/
                .addDefaultCase(                    () -> messageGeneralUsage(player))
                .execute(player, args);
        return true;
    }

    public ItemStack getZinsSpawner(int amount) {

        Component spawnerDisplayName = Component.text("&4&kdd&6&lZINS_SPAWNER&4&kdd".replace("&", "§"));
        ItemStack spawner = ItemStack.empty();
        spawner.setType(Material.SPAWNER);
        ItemMeta meta = spawner.getItemMeta();
        meta.displayName(spawnerDisplayName);
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 10, true);
        spawner.setItemMeta(meta);
        spawner.setAmount(amount);

        List<Component> loreList = new ArrayList<>();
        for (String s : ConstantManager.inst().spawner_interest_lore.split("\n"))
            loreList.add(Component.text(s));
        spawner.lore(loreList);

        return spawner;
    }

    private boolean onCommandSpawner(Player player, String[] args) {
        UUID uuid = tabinator.getPlayerFromCommand(player, args, bank_spawner_give_ONLINEPLAYER);
        Integer num = tabinator.getIntFromCommand(args, bank_spawner_give_ONLINEPLAYER_NUMBER);
        if (uuid != null && num != null) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                target.getInventory().addItem(getZinsSpawner(num));
            }
        }
        return true;
    }

    private void messageGeneralUsage(Player player) {
        ConstantManager.sendFormattedMessage(player, ConstantManager.inst().plugin_usage);
    }

    private boolean onCommandBankAdmin(Player player, String[] args) {
        UUID target = tabinator.getPlayerFromCommand(player, args, bank_admin_ONLINEPLAYER);
        if (target == null || Bukkit.getPlayer(target) == null) {
            if (args.length == 2)
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().player_not_found.replace("PLAYERNAME", args[1]));
            else
                messageGeneralUsage(player);
            return false;
        }
        if (isBankAdmin(player.getUniqueId())) {
            bankAdminEditSessions.removeIf(session -> session.admin.equals(player.getUniqueId()));
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().bank_admin_off);
        } else {
            bankAdminEditSessions.add(new BankAdminEditSession(player.getUniqueId(), target));
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().bank_admin_on.replace("PLAYERNAME", Bukkit.getPlayer(target).getName()));
        }
        return true;
    }

    public BankAdminEditSession getBankAdminSession(UUID adminId) {
        return bankAdminEditSessions.stream()
                .filter(session -> session.admin.equals(adminId))
                .findFirst()
                .orElse(null); // Return null if no session matches the admin ID
    }

    private void onCommandPayday(Player player) {
        long next = Math.round((float) (nextPayday - System.currentTimeMillis()) / 1000 / 60);
        String t;
        if (next == 1)
            t = "1ner Minute";
        else if (next == 0)
            t = "weniger als 1ner Minute";
        else
            t = next + " Minuten";
        ConstantManager.sendFormattedMessage(player, ConstantManager.inst().payday_command + " " + t);
    }

    private void onCommandBank(Player player) {
        messageGeneralUsage(player);
    }

    private void onCommandBanker(Player player) {
        BankerTrait.spawn(player.getTargetBlock(null, 10).getLocation().add(0.5, 1, 0.5));
    }

    private void onCommandPVPBanker(Player player) {
        PVPBankerTrait.spawn(player.getTargetBlock(null, 10).getLocation().add(0.5, 1, 0.5));
    }

    public double getBankBalance(OfflinePlayer player) {
        return DBActions.loadBankBalance(player.getUniqueId());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (raidManager != null)
            raidManager.fullcleanup();
        DBActions.cleanup();
        instance = null;
    }
}
