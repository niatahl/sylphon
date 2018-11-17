package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.SRD_ModPlugin;
import java.util.ArrayList;

public class SRD_BurstShieldGenerator extends BaseHullMod {

    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is a shield-related hullmod
        for (String s : ship.getVariant().getHullMods()) {
            if (SRD_ModPlugin.SHIELD_HULLMODS.contains(s) && s != "SRD_burst_shield_generator") {
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

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "unable to mount any shield-related hullmods";
        return null;
    }
}

