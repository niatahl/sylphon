//By Nicke535, a hullmod which buffs the ship's ECM resistance depending on how many other allies have this hullmod
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.SRD_ModPlugin;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.HashMap;
import java.util.Map;

public class SRD_TargetingLinkup extends BaseHullMod {

    public static final Map<HullSize, Float> ECM_IGNORE_PER_SHIP = new HashMap<HullSize, Float>();
    static {
        ECM_IGNORE_PER_SHIP.put(HullSize.FIGHTER, 0.04f);
        ECM_IGNORE_PER_SHIP.put(HullSize.FRIGATE, 0.04f);
        ECM_IGNORE_PER_SHIP.put(HullSize.DESTROYER, 0.04f);
        ECM_IGNORE_PER_SHIP.put(HullSize.CRUISER, 0.1f);
        ECM_IGNORE_PER_SHIP.put(HullSize.CAPITAL_SHIP, 0.1f);
    }
    public static final float MAX_BONUS = 0.6f;
    public static final String ID_FOR_BONUS = "SRD_TargetingLinkupID";

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Finds how many allies with the same hullmod are on the battlefield (only count up to our max, after that they don't matter)
        float alliedBonus = 0;
        for (ShipAPI testShip : Global.getCombatEngine().getShips()) {
            if (testShip.getVariant().getHullMods().contains("SRD_targeting_linkup") && !testShip.isHulk() && testShip.getOwner() == ship.getOwner()) {
                alliedBonus += ECM_IGNORE_PER_SHIP.get(testShip.getHullSize());

                if (alliedBonus >= MAX_BONUS) {
                    alliedBonus = MAX_BONUS;
                    break;
                }
            }
        }

        //Applies our bonus
        ship.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifyMult(ID_FOR_BONUS, 1f - alliedBonus);

        //If we are the player ship, display a tooltip showing our current bonus
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip(ID_FOR_BONUS + "_TOOLTIP", "graphics/sylphon/icons/hullsys/ai_network.png", "Targeting Linkup", "Current bonus: " + (alliedBonus*100f/MAX_BONUS) + "%", false);
        }
    }


    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //If we don't have a Sylph Core, we can't equip the hullmod
        return ship.getVariant().getHullMods().contains("SRD_sylph_core");
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("SRD_sylph_core")) {
            return "Requires an installed Sylph Core";
        } else {
            return "BUG: report to mod author that the file 'SRD_TargetingLinkup' has encountered an error";
        }
    }

    //Adds the description strings
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)(ECM_IGNORE_PER_SHIP.get(HullSize.FRIGATE) * 100f) + "%";
        if (index == 1) return "" + (int)(ECM_IGNORE_PER_SHIP.get(HullSize.DESTROYER) * 100f) + "%";
        if (index == 2) return "" + (int)(ECM_IGNORE_PER_SHIP.get(HullSize.CRUISER) * 100f) + "%";
        if (index == 3) return "" + (int)(ECM_IGNORE_PER_SHIP.get(HullSize.CAPITAL_SHIP) * 100f) + "%";
        if (index == 4) return "" + (int)(MAX_BONUS * 100f) + "%";
        return null;
    }
}