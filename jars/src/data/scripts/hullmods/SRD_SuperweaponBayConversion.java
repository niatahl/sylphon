//By Nicke535
//NEW EFFECT: Adds one fighter bay for each un-filled Large mount on the ship, and gives a 5 OP discount for large Energy weapons
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
    //How much cheaper are energy weapons to mount (we want to slightly incentivize them, as they aren't as good as Ballistic or Missile at many things)
    public final static float OP_COST_BONUS = -10f;

    //Affects OP costs, those effects must be in applyBeforeShipCreation
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, OP_COST_BONUS);
    }

    //Most of our bonuses and effects needs to be set in applyEffectsAfterShipCreation, due to depending on weapon loadout
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Used for tracking how many additional bays we want; starts at the maximum number of slots we have
        int emptySlots = 2;

        //Checks how many of our Large slots are filled
        for (String slotID : ship.getVariant().getFittedWeaponSlots()) {
            if (ship.getVariant().getSlot(slotID).getSlotSize().equals(WeaponAPI.WeaponSize.LARGE)) {
                emptySlots--;
            }
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
        if (index == 0) return "one additional fighter bay";
        if (index == 1) return "" + Math.round(-OP_COST_BONUS) + " less OP";
        return null;
    }
}

