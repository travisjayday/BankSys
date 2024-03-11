package com.stardust.banksys;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.stardust.banksys.utils.ConstantManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.awt.event.ComponentListener;
import java.util.*;

public class BankRegionSelector implements Listener {
    UUID activePlayer;
    BankSys.BankRegion activeRegion;
    Location pos1;

    ItemStack selectionWand;
    Location pos2;
    char faceDir;
    Location bankPos1;
    Location bankPos2;

    /** used to store the player's last held item when it gets replaced with wand */
    ItemStack oldHandItem;
    boolean cuiEnabled;

    Map<BankSys.BankRegion, DBActions.SavedRegion> boundaries;

    BankRegionSelector() {
        selectionWand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = selectionWand.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Bank Region Selector");
        List<Component> loreList = new ArrayList<>();
        for (String str : ConstantManager.inst().selection_wand_lore.split("\n"))
            loreList.add(Component.text(str));
        meta.lore(loreList);
        selectionWand.setItemMeta(meta);
        cuiEnabled = false;
        boundaries = new HashMap<>();
        for (BankSys.BankRegion reg : BankSys.BankRegion.values()) {
            DBActions.SavedRegion loc = DBActions.loadRegion(reg);
            if (loc != null) {
                boundaries.put(reg, loc);
            }
        }
        recomputeBankBorder();
    }

    public void onCommandSelectRegion(Player player, BankSys.BankRegion region) {
        switch (region) {
            case entrance1 -> ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_prompt_entrace1);
            case entrance2 -> ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_prompt_entrace2);
            case pvp1 -> ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_prompt_pvp1);
            case pvp2 -> ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_prompt_pvp2);
            case waitroom -> ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_prompt_waitroom);
        }
        ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_prompt_usage);
        activePlayer = player.getUniqueId();
        activeRegion = region;
        pos1 = null;
        pos2 = null;
        oldHandItem = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(selectionWand);
    }

    private void enableCUI(Player player, boolean enable) {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(actor);
        if (session != null) {
            if (enable)
                session.setUseServerCUI(true);
            else
                session.setUseServerCUI(false);
            session.updateServerCUI(actor);
            cuiEnabled = enable;
        }
    }

    private void clearWESelection(Player player) {
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().getIfPresent(actor);
        if (session != null) {
            session.getRegionSelector(BukkitAdapter.adapt(player.getWorld())).clear();
            session.dispatchCUISelection(actor);
            session.updateServerCUI(actor);
        }
    }

    private void recomputeBankBorder() {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        World world = null;
        for (Map.Entry<BankSys.BankRegion, DBActions.SavedRegion> entry : boundaries.entrySet()) {
            if (entry.getKey() == BankSys.BankRegion.waitroom) continue;
            world = entry.getValue().pos1.getWorld();
            maxX = Math.max(maxX, entry.getValue().pos1.getBlockX());
            maxX = Math.max(maxX, entry.getValue().pos2.getBlockX());
            maxZ = Math.max(maxZ, entry.getValue().pos1.getBlockZ());
            maxZ = Math.max(maxZ, entry.getValue().pos2.getBlockZ());
            minX = Math.min(minX, entry.getValue().pos1.getBlockX());
            minX = Math.min(minX, entry.getValue().pos2.getBlockX());
            minZ = Math.min(minZ, entry.getValue().pos1.getBlockZ());
            minZ = Math.min(minZ, entry.getValue().pos2.getBlockZ());
        }

        /*
        if (world != null) {
            bankPos1 = new Location(world, minX, 50, minZ);
            bankPos2 = new Location(world, maxX, 50, maxZ);
            for (int i = 50; i < 150; i++) {
                bankPos1.getBlock().setType(Material.DIAMOND_BLOCK);
                bankPos1.add(0, 1, 0);
                bankPos2.getBlock().setType(Material.EMERALD_BLOCK);
                bankPos2.add(0, 1, 0);
            }
        }*/
    }

    void finishSel(Player player) {
        if (pos1 != null && pos2 != null) {
            /*if (activeRegion == BankSys.BankRegion.entrance1 || activeRegion == BankSys.BankRegion.entrance2) {
                BankerNPC.spawn(pos1, pos2);
            }*/
            DBActions.SavedRegion savedRegion = new DBActions.SavedRegion(pos1, pos2, player.getFacing().getDirection());
            DBActions.saveRegion(activeRegion, savedRegion);
            boundaries.put(activeRegion, savedRegion);
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_wand_confirmed);
            recomputeBankBorder();
            clearWESelection(player);
            resetSelection(player);
        }
        else {
            ConstantManager.sendFormattedMessage(player, ConstantManager.inst().selection_wand_failed);
        }
    }

    /** Selection wand events */
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (activePlayer == player.getUniqueId()) {
            if (!cuiEnabled) enableCUI(player, true);

            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock() != null) {
                    pos2 = event.getClickedBlock().getLocation();
                }
            }
            else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    // finish selection
                    finishSel(player);
                }
                else {
                    if (event.getClickedBlock() != null) {
                        pos1 = event.getClickedBlock().getLocation();
                    }
                }
            }
            else if (event.getAction() == Action.LEFT_CLICK_AIR) {
                if (player.isSneaking()) finishSel(player);
            }
        }
    }

    void resetSelection(Player player) {
        activePlayer = null;
        pos1 = null;
        pos2 = null;
        player.getInventory().setItemInMainHand(oldHandItem);
        enableCUI(player, false);
    }

    /** If player changes away from selection wand, end base selection */
    @EventHandler
    public void onPlayerItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (activePlayer == player.getUniqueId()) {
            Inventory inventory = player.getInventory();
            if (inventory.contains(selectionWand)) {
                inventory.remove(selectionWand);
                resetSelection(player);
            }
        }
    }

    /** If player drops selection wand, send base selection */
    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (activePlayer == player.getUniqueId()) {
            event.getItemDrop().remove();
            resetSelection(player);
        }
    }

    public static boolean inRegion(Location testpoint, DBActions.SavedRegion boundary) {
        if (boundary == null) return false;
        int xMin = Math.min(boundary.pos1.getBlockX(), boundary.pos2.getBlockX());
        int xMax = Math.max(boundary.pos1.getBlockX(), boundary.pos2.getBlockX());
        int zMin = Math.min(boundary.pos1.getBlockZ(), boundary.pos2.getBlockZ());
        int zMax = Math.max(boundary.pos1.getBlockZ(), boundary.pos2.getBlockZ());

        return testpoint.getBlockX() >= xMin && testpoint.getBlockX() <= xMax
                && testpoint.getBlockZ() >= zMin && testpoint.getBlockZ() <= zMax;
    }

    private void onEnterRegion(Player player, BankSys.BankRegion region, Location from, Location to, Vector facing) {
        if (region.equals(BankSys.BankRegion.entrance1) || region.equals(BankSys.BankRegion.entrance2)) {
            Vector facingNow = new Vector(to.getBlockX() - from.getBlockX(), 0, to.getBlockZ() - from.getBlockZ());
            if (facingNow.getX() == facing.getX() && facingNow.getZ() == facing.getZ()) {
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().bank_enter_welcome);
                BankSys.getInstance().raidManager.onEnteredBank(player);
            }
        }
    }

    private void onExitRegion(Player player, BankSys.BankRegion region, Location from, Location to, Vector facing) {
        if (region.equals(BankSys.BankRegion.entrance1) || region.equals(BankSys.BankRegion.entrance2)) {
            Vector facingNow = new Vector(to.getBlockX() - from.getBlockX(), 0, to.getBlockZ() - from.getBlockZ());
            if (facingNow.getX() == -facing.getX() && facingNow.getZ() == -facing.getZ()) {
                ConstantManager.sendFormattedMessage(player, ConstantManager.inst().bank_leave_bye);
                BankSys.getInstance().raidManager.onExitedBank(player);
            }
        }
        else if (region.equals(BankSys.BankRegion.pvp1)) {
            if (!inRegion(to, boundaries.get(BankSys.BankRegion.pvp2)))
                BankSys.getInstance().raidManager.onExitedPVP(player);
        }
        else if (region.equals(BankSys.BankRegion.pvp2)) {
            if (!inRegion(to, boundaries.get(BankSys.BankRegion.pvp1)))
                BankSys.getInstance().raidManager.onExitedPVP(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((from.getBlockX()) != (to.getBlockX()) || (from.getBlockZ()) != (to.getBlockZ())) {
            for (Map.Entry<BankSys.BankRegion, DBActions.SavedRegion> entry : boundaries.entrySet()) {
                if (inRegion(from, entry.getValue()) && !inRegion(to, entry.getValue())) {
                    // left region
                    onExitRegion(event.getPlayer(), entry.getKey(), from, to, entry.getValue().facing);
                }
                else if (inRegion(to, entry.getValue()) && !inRegion(from, entry.getValue())) {
                    // entered region
                    onEnterRegion(event.getPlayer(), entry.getKey(), from, to, entry.getValue().facing);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClicked(InventoryClickEvent event) {
        if (BankSys.getInstance().sensitiveInventories.contains(event.getInventory())) {
            if (event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE || event.getAction() == InventoryAction.PICKUP_ONE || event.getAction() == InventoryAction.PICKUP_ALL) {
                // ok
            }
            else {
                event.setCancelled(true);
            }
        }
    }
}
