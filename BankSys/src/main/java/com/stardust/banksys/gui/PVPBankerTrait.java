package com.stardust.banksys.gui;


import com.stardust.banksys.BankSys;
import com.stardust.banksys.RaidManager;
import com.stardust.banksys.utils.ConstantManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.goals.MoveToGoal;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.gui.InventoryMenu;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitFactory;
import net.citizensnpcs.api.trait.TraitName;
import net.kyori.adventure.text.Component;
import net.royawesome.jlibnoise.module.source.Const;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import scala.collection.immutable.Stream;

import java.util.HashMap;
import java.util.UUID;

@TraitName("pvpbankertrait")
public class PVPBankerTrait extends Trait {
    RaidManager raidManager;

    public PVPBankerTrait() {
        super("pvpbankertrait");
        plugin = JavaPlugin.getPlugin(BankSys.class);
        raidManager = plugin.raidManager;
    }

    public static void spawn(final Location loc) {
        NPC banker = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "PVPBanker");
        banker.addTrait(new PVPBankerTrait());
        banker.spawn(loc);
    }

    @Override
    public void onSpawn() {
        this.getNPC().setProtected(false);
        LivingEntity npc = (LivingEntity) this.getNPC().getEntity();
        npc.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000);
        npc.setHealth(1000);
        this.spawnLoc = this.getNPC().getStoredLocation();
        Bukkit.getLogger().info("Moving forward to " + spawnLoc);
        this.getNPC().getDefaultGoalController().addGoal(new MoveToGoal(this.getNPC(), spawnLoc), 1);
    }
    BankSys plugin = null;

    // see the 'Persistence API' section
    @Persist("spawnLoc") Location spawnLoc = null;

    @EventHandler
    public void click(net.citizensnpcs.api.event.NPCRightClickEvent event){
        //Handle a click on a NPC. The event has a getNPC() method.
        //Be sure to check event.getNPC() == this.getNPC() so you only handle clicks on this NPC!
        if (event.getNPC() == this.getNPC()) {
            ConstantManager.sendFormattedMessage(event.getClicker(), ConstantManager.inst().pvpbanker_interact);
        }
    }

    @EventHandler
    public void onDamage(NPCDamageByEntityEvent event) {
        if (event.getNPC() == this.getNPC()) {
            raidManager.startRaid((Player) event.getDamager());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                LivingEntity npc = (LivingEntity) this.getNPC().getEntity();
                npc.setHealth(1000);
            }, 20);
        }
    }
}