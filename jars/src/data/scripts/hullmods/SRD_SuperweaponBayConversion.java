//By Nicke535
//This hullmod converts all Large energy weapon slots into Super Large energy weapon slots (by cheating a bit with OP costs), as well as adds 1 fighter bay for each un-fitted Super Large slot
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.ArrayList;

public class SRD_SuperweaponBayConversion extends BaseHullMod {
    //How many times more OP does a superweapon cost? Preferably use something higher than x100, since there *might* be ships capable of fitting that
    public final static float OP_COST_MULT = 1000f;

    //Used for saving data, this is dangerously volatile stuff


    //Affects OP costs, those effects must be in applyBeforeShipCreation
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyMult(id, 1f/OP_COST_MULT);
    }

    //Most of our bonuses and effects needs to be set in applyEffectsAfterShipCreation, due to depending on weapon loadout
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Used for tracking how many additional bays we want; starts at the maximum number of slots we have
        int emptySlots = 2;

        //Removes non-super-heavy weapons from our super-heavy slots
        ArrayList<String> removeList = new ArrayList<String>();
        for (String slotID : ship.getVariant().getNonBuiltInWeaponSlots()) {
            //Only care about Large Energy slots
            if (ship.getVariant().getSlot(slotID).getSlotSize().equals(WeaponAPI.WeaponSize.LARGE) && ship.getVariant().getSlot(slotID).getWeaponType().equals(WeaponAPI.WeaponType.ENERGY)) {
                //Checks for our unique tag; this *could* be used by other mods, if they are insane enough to follow my methodology. Remove all weapons that lack it, and any weapon that has it contributes to a non-empty slot
                if (!ship.getVariant().getWeaponSpec(slotID).hasTag("NICTOY_SUPERHEAVY")) {
                    removeList.add(slotID);
                } else if (ship.getVariant().getWeaponSpec(slotID).hasTag("NICTOY_SUPERHEAVY")) {
                    emptySlots--;
                }
            }
        }
        for (String s : removeList) {
            ship.getVariant().clearSlot(s);
        }

        //If we have too few clear slots, we delete some fighter LPCs due to a vanilla bug that triggers when removing fighter bays via hullmod
        if (emptySlots < 2) {
            //Removes the fighter LPC in slot 6 (index 5), if we have any
            if (ship.getVariant().getWing(5) != null) {
                if (Global.getSector() != null) {
                    if (Global.getSector().getPlayerFleet() != null) {
                        Global.getSector().getPlayerFleet().getCargo().addFighters(ship.getVariant().getWingId(5), 1);
                    }
                }
                ship.getVariant().setWingId(5 ,null);
            }

            if (emptySlots < 1) {
                //Removes the fighter LPC in slot 5 (index 4), if we have any
                if (ship.getVariant().getWing(4) != null) {
                    if (Global.getSector() != null) {
                        if (Global.getSector().getPlayerFleet() != null) {
                            Global.getSector().getPlayerFleet().getCargo().addFighters(ship.getVariant().getWingId(4), 1);
                        }
                    }
                    ship.getVariant().setWingId(4 ,null);
                }
            }
        }

        //Increase our number of fighter bays by 1 for each unfitted slot we have, by adding a fake, hidden hullmod [WOW this is an ugly way to solve things]
        //First, remove the hidden hullmods we are not supposed to have
        ArrayList<String> deletionList = new ArrayList<String>();
        boolean alreadyHasHullmod = false;
        for (String s : ship.getVariant().getNonBuiltInHullmods()) {
            //If this is the hullmod we are gonna add later, just leave it and don't add it later
            if (s.equals("SRD_hiddenmod_extra_carrier_bays_" + emptySlots)) {
                alreadyHasHullmod = true;
            } else if (s.contains("SRD_hiddenmod_extra_carrier_bays_")) {
                deletionList.add(s);
            }
        }
        for (String s : deletionList) {
            ship.getVariant().removeMod(s);
        }

        //Then, add the new hullmod, if we have an amount of slots that allows it and don't already have it
        if (emptySlots > 0 && emptySlots < 3 && !alreadyHasHullmod) {
            ship.getVariant().addMod("SRD_hiddenmod_extra_carrier_bays_" + emptySlots);
        }
    }

    //Never applicable: only comes built-in
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Affects OP costs in some manner
    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "super-large mounts";
        if (index == 1) return "Harmonius";
        if (index == 2) return "one additional fighter bay";
        return null;
    }
}

