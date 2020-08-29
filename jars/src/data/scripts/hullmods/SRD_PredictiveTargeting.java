package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class SRD_PredictiveTargeting extends BaseHullMod {

    private static final float ENERGY_RANGE_INCREASE = 100f;

    //Applies the effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponRangeBonus().modifyFlat(id,ENERGY_RANGE_INCREASE);
    }

    //Never applicable: only comes built-in
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)ENERGY_RANGE_INCREASE +"su";
        return null;
    }
}

