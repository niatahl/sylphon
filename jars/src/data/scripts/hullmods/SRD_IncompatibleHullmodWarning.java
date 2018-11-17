package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseHullMod;

public class SRD_IncompatibleHullmodWarning extends BaseHullMod {

    private static int runnerCount = 0;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (runnerCount != 2) {
            runnerCount++;
        } else {
            runnerCount = 0;
            ship.getVariant().removeMod("SRD_IncompatibleHullmodWarning");
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "WARNING";
        
        return null;
    }
}
