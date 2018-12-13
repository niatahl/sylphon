package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.SRD_ModPlugin;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRD_MasterworkOptics extends BaseHullMod {

    public static float BEAM_RANGE_INCREASE = 400f;
    public static float SUPPLY_USE_MULT = 2.25f;
    public static float REPAIR_COST_MULT = 1.5f;
    public static float BOMBARD_BONUS = 500f;

    //Applies the effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBeamWeaponRangeBonus().modifyFlat(id, BEAM_RANGE_INCREASE);
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);
        stats.getSuppliesToRecover().modifyMult(id, REPAIR_COST_MULT);
        stats.getDynamic().getMod(Stats.FLEET_BOMBARD_COST_REDUCTION).modifyFlat("SRD_MasterworkOpticsID", BOMBARD_BONUS);
    }

    //Never applicable: only comes built-in
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)BEAM_RANGE_INCREASE;
        if (index == 1) return "" + (int)BOMBARD_BONUS + " units of fuel";
        if (index == 2) return "" + (int)((SUPPLY_USE_MULT-1f)*100f) + "%";
        if (index == 3) return "" + (int)((REPAIR_COST_MULT-1f)*100f) + "%";
        return null;
    }
}

