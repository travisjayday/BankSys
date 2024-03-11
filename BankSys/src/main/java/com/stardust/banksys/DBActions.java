package com.stardust.banksys;

import com.stardust.banksys.utils.ConstantManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DBActions {

    static Connection connection;
    public static class SavedRegion {
        Location pos1;
        Location pos2;
        Vector facing;

        SavedRegion(Location pos1, Location pos2, Vector facing) {
            this.pos1 = pos1.getBlock().getLocation();
            this.pos2 = pos2.getBlock().getLocation();
            this.facing = facing;
        }

        SavedRegion() {}

        @Override
        public String toString() {
            return pos1.getWorld().getUID() +
                    "," +
                    pos1.getBlockX() +
                    "," +
                    pos1.getBlockY() +
                    "," +
                    pos1.getBlockZ() +
                    "," +
                    pos2.getBlockX() +
                    "," +
                    pos2.getBlockY() +
                    "," +
                    pos2.getBlockZ() +
                    "," +
                    facing.getX() +
                    "," +
                    facing.getY() +
                    "," +
                    facing.getZ();
        }

        public static SavedRegion fromString(String str) {
            SavedRegion savedRegion = new SavedRegion();
            String[] parts = str.split(",");
            World world = Bukkit.getWorld(UUID.fromString(parts[0]));
            savedRegion.pos1 = new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            savedRegion.pos2 = new Location(world, Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6]));
            savedRegion.facing = new Vector(Double.parseDouble(parts[7]), Double.parseDouble(parts[8]), Double.parseDouble(parts[9]));
            return savedRegion;
        }
    }

    static boolean enableDB() {
        try {
            connect();
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] Class Not Found: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] enableDB: SQLException: " + e.getMessage());
            return false;
        }
        return true;
    }

    static void saveRegion(BankSys.BankRegion region, SavedRegion savedRegion) {
        FileConfiguration config = BankSys.getBankSysConfig();
        ConfigurationSection section = config.getConfigurationSection("regions");
        if (section == null)
            section = config.createSection("regions");
        section.set(region.toString(), savedRegion.toString());
        BankSys.getInstance().saveConfig();
    }

    static SavedRegion loadRegion(BankSys.BankRegion region) {
        FileConfiguration config = BankSys.getBankSysConfig();
        ConfigurationSection section = config.getConfigurationSection("regions");
        if (section == null) return null;
        String s = section.getString(region.toString());
        if (s == null) return null;
        return SavedRegion.fromString(s);
    }

    public static double loadBankBalance(UUID uuid) {
        if (!connectionOk("loadBankBalance")) return -1;
        String sql = "SELECT BankBalance FROM BankSysTable WHERE UUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBytes(1, uuidToBytes(uuid));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("BankBalance");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] loadBankBalance: " + e.getMessage());
        }
        return -1;
    }

    public static double loadPlayerBalance(UUID uuid) {
        return BankSys.getEconomy().getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public static void addPlayerBalance(UUID uuid, double amount) {
        BankSys.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public static void subPlayerBalance(UUID uuid, double amount) {
        BankSys.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(uuid), amount);
    }

    static long lastReconnectAttempt = 0;

    private static boolean connectionOk(String name) {
        try {
            if (!connection.isClosed())
                return true;
        } catch (Exception ignored) {}
        if (System.currentTimeMillis() - lastReconnectAttempt > 3000) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] [" + name + "] Connection to DB lost, trying to re-connect...");
            return enableDB();
        }
        return false;
    }

    public static void saveBankBalance(UUID uuid, double balance) {
        if (!connectionOk("saveBankBalance")) return;
        String sql = "INSERT INTO BankSysTable (UUID, BankBalance) VALUES (?, ?) ON DUPLICATE KEY UPDATE BankBalance = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBytes(1, uuidToBytes(uuid));
            pstmt.setDouble(2, balance);
            pstmt.setDouble(3, balance);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] saveBankBalance: " + e.getMessage());
        }
    }

    public static void saveSpawners(UUID uuid, int i) {
        if (!connectionOk("saveBankBalance")) return;
        String sql = "INSERT INTO BankSysTable (UUID, Spawners) VALUES (?, ?) ON DUPLICATE KEY UPDATE Spawners = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBytes(1, uuidToBytes(uuid));
            pstmt.setInt(2, i);
            pstmt.setInt(3, i);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] saveSpawners: " + e.getMessage());
        }
    }

    public static int loadSpawners(UUID uuid) {
        if (!connectionOk("loadSpawners")) return 1;
        String sql = "SELECT Spawners FROM BankSysTable WHERE UUID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBytes(1, uuidToBytes(uuid));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Spawners");
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] loadSpawners: " + e.getMessage());
        }
        return 1;
    }

    private static void connect() throws SQLException, ClassNotFoundException {
        String host = ConstantManager.inst().mysql_host;
        int port = ConstantManager.inst().mysql_port;
        String username = ConstantManager.inst().mysql_user;
        String password = ConstantManager.inst().mysql_password;
        String database = ConstantManager.inst().mysql_database;

        Class.forName("com.mysql.jdbc.Driver");
        String s = "jdbc:mysql://" + host+ ":" + port + "/" + database;
        Bukkit.getLogger().info("[BankSys] Connecting to MySQL database at " + s + " with credentials: " + username + " ***");
        connection = DriverManager.getConnection(s, username, password);

        try {
            Statement stmt = connection.createStatement();

            String sql = "CREATE TABLE IF NOT EXISTS BankSysTable (" +
                    "UUID BINARY(16) PRIMARY KEY," +
                    "Spawners INT DEFAULT 1," +
                    "BankBalance DOUBLE" +
                    ");";

            stmt.executeUpdate(sql);
            Bukkit.getLogger().info("[BankSys] Created SQL table");
            stmt.close();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BankSys] Failed to create table: " + e.getMessage());
        }
    }

    private static byte[] uuidToBytes(UUID uuid) {
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        return ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
    }

    public static void cleanup() {
        try {
            connection.close();
        } catch (Exception e) {

        }
    }
}
