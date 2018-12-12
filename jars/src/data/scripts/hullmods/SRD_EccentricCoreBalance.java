package data.scripts.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class SRD_EccentricCoreBalance extends BaseHullMod {

    //Normal Eccentric stats
    public static final float SUPPLY_USE_MULT = 1.50f;
    public static final float CREW_CAPACITY_MULT = 0.8f;

    //Balance specific stats
    public static float SHIELD_DEPLOY_BOOST = 2f;
    public static float CP_RECOVERY_BOOST = 2f;

    //-----------------------------------------------------Betrayal stats...--------------------------------------------
    //Bonus multiplier for "percieved intelligence", for the purpose of Sylph Core betrayal. It's an Eccentric, so
    //it should probably be quite high
    public static final float INTELLIGENCE_MULT = 2f;

    //Applies the effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);

        stats.getMaxCrewMod().modifyMult(id, CREW_CAPACITY_MULT);
        stats.getMinCrewMod().modifyMult(id, 0f);

        stats.getShieldUnfoldRateMult().modifyMult(id, SHIELD_DEPLOY_BOOST);

    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getMutableStats().getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT).modifyFlat(id, CP_RECOVERY_BOOST);
    }

    //Applies any in-combat mutable effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Handles the betrayal part of the code by accessing the Sylph Core's code; no need to duplicate
        SRD_SylphCore.advanceInCombatStatic(ship,amount);
    }

    //Never applicable: only comes built-in
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)((SUPPLY_USE_MULT-1f)*100f) + "%";
        if (index == 1) return "" + (int)Math.ceil((CREW_CAPACITY_MULT-1f)*-100f) + "%";
        if (index == 2) return "Sylph Core";
        if (Global.getSector() != null && Global.getCurrentState().equals(GameState.CAMPAIGN)) {
            if (index == 3 && Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().contains("$SRDHasSeenAIRebellion")) return " Has a chance to rebel under certain circumstances.";
            else if (index == 3) return "";
        } else if (index == 3) {
            return "";
        }
        if (index == 4) return "-Balance Unto All";
        if (index == 5) return "" + (int)SHIELD_DEPLOY_BOOST + " times";
        if (index == 6) return "100%";
        return null;
    }
}

