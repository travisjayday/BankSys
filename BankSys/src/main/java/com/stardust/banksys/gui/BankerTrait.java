package com.stardust.banksys.gui;


import com.stardust.banksys.BankSys;
import com.stardust.banksys.gui.BankerInterestPage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.gui.CitizensInventoryClickEvent;
import net.citizensnpcs.api.gui.InventoryMenu;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.plugin.java.JavaPlugin;

@TraitName("bankertrait")
public class BankerTrait extends Trait {
    public BankerTrait() {
        super("bankertrait");
        plugin = JavaPlugin.getPlugin(BankSys.class);
    }

    public static void spawn(final Location loc) {
        NPC banker = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Banker");
        banker.addTrait(new BankerTrait());
        banker.spawn(loc);
    }

    BankSys plugin = null;

    boolean SomeSetting = false;

    // see the 'Persistence API' section
    @Persist("isBanker") boolean isBanker = true;

    // An example event handler. All traits will be registered automatically as Spigot event Listeners
    @EventHandler
    public void click(net.citizensnpcs.api.event.NPCRightClickEvent event){
        //Handle a click on a NPC. The event has a getNPC() method.
        //Be sure to check event.getNPC() == this.getNPC() so you only handle clicks on this NPC!
        if (event.getNPC() == this.getNPC()) {
             InventoryMenu.createSelfRegistered(new BankerMainPage(event.getClicker())).present(event.getClicker());
        }
    }
}