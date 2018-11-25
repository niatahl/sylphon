//By Nicke535, if you're reading this you are most likely spoiling a lot of surprises (or you're Nia. That's also possible)
package data.scripts.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRD_EccentricCoreCieve extends BaseHullMod {

    //Normal Eccentric stats
    public static final float SUPPLY_USE_MULT = 1.50f;
    public static final float CREW_CAPACITY_MULT = 0.8f;

    //Special stats for Cieve
    public static final float BALLISTIC_FLUX_USE_MULT = 0.9f;
    public static final float BONUS_PD_DAMAGE_PERCENT = 40f;

    //-----------------------------------------------------Betrayal stats...--------------------------------------------
    //Bonus multiplier for "percieved intelligence", for the purpose of Sylph Core betrayal. It's an Eccentric, so
    //it should probably be quite high
    public static final float INTELLIGENCE_MULT = 2f;

    //Applies the before-combat effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);

        stats.getMaxCrewMod().modifyMult(id, CREW_CAPACITY_MULT);
        stats.getMinCrewMod().modifyMult(id, 0f);

        stats.getDamageToMissiles().modifyPercent(id, BONUS_PD_DAMAGE_PERCENT);
        stats.getDamageToFighters().modifyPercent(id, BONUS_PD_DAMAGE_PERCENT);
        stats.getAutofireAimAccuracy().modifyFlat(id, 1f);
        stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);

        stats.getDynamic().getMod("SRD_SylphCoreTChanceMult").modifyMult(id, INTELLIGENCE_MULT);
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
        if (index == 4) return "-Cieve";
        if (index == 5) return "" + (int)Math.ceil((BALLISTIC_FLUX_USE_MULT-1f)*-100f) + "%";
        if (index == 6) return "" + (int)Math.round(BONUS_PD_DAMAGE_PERCENT) + "%";
        if (index == 7) return "perfect target leading";
        return null;
    }
}

