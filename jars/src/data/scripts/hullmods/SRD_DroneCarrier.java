//By Nicke535, hullmod which automatically removes any non-drone fighters from the ship
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.ArrayList;

public class SRD_DroneCarrier extends BaseHullMod {

    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    public static float FIGHTER_REFIT_TIME_MULT = 0.9f;
    public static float REPLACEMENT_RATE_DECREASE_MULT = 0.75f;
    public static float REPLACEMENT_RATE_INCREASE_MULT = 1.5f;

    //Applies the stat-based effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFighterRefitTimeMult().modifyMult(id, FIGHTER_REFIT_TIME_MULT);
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, REPLACEMENT_RATE_DECREASE_MULT);
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyMult(id, REPLACEMENT_RATE_INCREASE_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //-----------------------------------------REMOVING FIGHTER WINGS-----------------------------------------------
        //If we happen to have automated our fighters via Seeker magic, they are all unaffected
        if (!ship.getVariant().getHullMods().contains("SKR_remote")) {
            //Attempt to go through all fighter wings on the ship
            int i = 0;
            while (i < ship.getMutableStats().getNumFighterBays().getModifiedValue() && i < 20) {
                //Ignore empty slots
                if (ship.getVariant().getWing(i) == null) {
                    i++;
                    continue;
                }

                //This wing has crew requirement; remove it, and add a LPC to the player's inventory
                if (ship.getVariant().getWing(i).getVariant().getHullSpec().getMinCrew() > 0) {
                    if (Global.getSector() != null) {
                        if (Global.getSector().getPlayerFleet() != null) {
                            Global.getSector().getPlayerFleet().getCargo().addFighters(ship.getVariant().getWingId(i), 1);
                        }
                    }
                    ship.getVariant().setWingId(i ,null);
                }

                //Finally, increase our iterator
                i++;
            }
        }

        //-------------------------------------------REMOVING HULLMODS--------------------------------------------------
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is forbidden. If it is, remove it
        for (String s : ship.getVariant().getNonBuiltInHullmods()) {
            if (s.contains("expanded_deck_crew")) {
                deletionList.add(s);
            }
        }

        //Finally, deletes the hullmods we aren't allowed to have
        if (deletionList.size() > 0) {
            ship.getVariant().addMod("SRD_IncompatibleHullmodWarning");
            Global.getSoundPlayer().playUISound(BAD_HULLMOD_NOTIFICATION_SOUND, 1f, 1f);
        }
        for (String s : deletionList) {
            ship.getVariant().removeMod(s);
        }
    }

    //Never applicable: only comes built-in
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "cannot mount crewed fighters";
        if (index == 1) return "" + (int)((1f-FIGHTER_REFIT_TIME_MULT)*100f) + "%";
        if (index == 2) return "" + (int)((1f-REPLACEMENT_RATE_DECREASE_MULT)*100f) + "%";
        if (index == 3) return "" + (int)((REPLACEMENT_RATE_INCREASE_MULT-1f)*100f) + "%";
        if (index == 4) return "Expanded Deck Crew";
        return null;
    }
}

