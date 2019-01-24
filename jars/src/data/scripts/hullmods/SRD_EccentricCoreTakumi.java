package data.scripts.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.SRD_ModPlugin;

import java.util.ArrayList;

public class SRD_EccentricCoreTakumi extends BaseHullMod {
    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    private final float CHECK=1f;
    private float timer=0, previous=0;
    private String ID="Booster Overdrive";

    //Normal Eccentric stats
    public static final float SUPPLY_USE_MULT = 1.50f;

    //Takumi specific stats
    public static float ENGINE_DAMAGE_MULT = 0.1f;



    //-----------------------------------------------------Betrayal stats...--------------------------------------------
    //Bonus multiplier for "percieved intelligence", for the purpose of Sylph Core betrayal. It's an Eccentric, so
    //it should probably be quite high
    public static final float INTELLIGENCE_MULT = 2f;

    //Applies the effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);

        stats.getMinCrewMod().modifyMult(id, 0f);

        stats.getEngineDamageTakenMult().modifyMult(id, ENGINE_DAMAGE_MULT);

        stats.getDynamic().getMod("SRD_SylphCoreTChanceMult").modifyMult(id, INTELLIGENCE_MULT);
    }


    //Applies any in-combat mutable effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Handles the betrayal part of the code by accessing the Sylph Core's code; no need to duplicate
        SRD_SylphCore.advanceInCombatStatic(ship,amount);

        timer=Global.getCombatEngine().getTotalElapsedTime(false);

        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        if(previous>=timer || timer>previous+CHECK || ship==playerShip){

            if(ship!=playerShip){
                previous=timer;
            }

            int tick = (int)ship.getFullTimeDeployed()/30;
            float effectLevel = Math.min(1, 0.2f*tick);

            ship.getMutableStats().getMaxSpeed().modifyMult(ID, 1+(50*effectLevel/100));
            ship.getMutableStats().getAcceleration().modifyMult(ID, 1+(50*effectLevel/100));
            ship.getMutableStats().getDeceleration().modifyMult(ID, 1+(50*effectLevel/100));

            if(ship==playerShip){
                Global.getCombatEngine().maintainStatusForPlayerShip(
                        "TakumiOverdrive",
                        "graphics/icons/hullsys/high_energy_focus.png",
                        "Booster Overdrive",
                        "Engines running at " + (100+Math.round(effectLevel*50)) + "% power",
                        effectLevel < 0f);
            }
        }
    }

    //Never applicable: only comes built-in
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)((SUPPLY_USE_MULT-1f)*100f) + "%";
        if (index == 1) return "Sylph Core";
        if (Global.getSector() != null && Global.getCurrentState().equals(GameState.CAMPAIGN)) {
            if (index == 2 && Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().contains("$SRDHasSeenAIRebellion")) return " Has a chance to rebel under certain circumstances.";
            else if (index == 2) return "";
        } else if (index == 2) {
            return "";
        }
        if (index == 3) return "-Takumi";
        if (index == 4) return "90%";
        if (index == 5) return "20%";
        if (index == 6) return "30 seconds";
        if (index == 7) return "50%";
        return null;
    }
}

