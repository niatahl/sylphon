package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class SRD_SylphonCrews extends BaseHullMod {

    private static final float UPKEEP_MULT = 0.9f;
    private static final float UNFOLD_PERCENT = 15f;

    //Applies the effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getShieldUpkeepMult().modifyMult(id,UPKEEP_MULT);
        stats.getShieldUnfoldRateMult().modifyPercent(id,UNFOLD_PERCENT);
        stats.getShieldTurnRateMult().modifyPercent(id,UNFOLD_PERCENT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("CHM_commission")) {
            ship.getVariant().removeMod("CHM_commission");
        }
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)((1f-UPKEEP_MULT)*100f) +"%";
        if (index == 1) return "" + (int)UNFOLD_PERCENT + "%";
        return null;
    }
}

