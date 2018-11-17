//By Nicke535, a hullmod which buffs the ship's autofire accuracy and turret turn rate depending on how many other allies have this hullmod, while also including the functionality of IPDAI
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;

import java.util.HashMap;
import java.util.Map;

public class SRD_DefenseGrid extends BaseHullMod {

    public static final Map<HullSize, Float> BONUS_PERCENT_PER_SHIP = new HashMap<HullSize, Float>();
    static {
        BONUS_PERCENT_PER_SHIP.put(HullSize.FIGHTER, 0.1f);
        BONUS_PERCENT_PER_SHIP.put(HullSize.FRIGATE, 0.1f);
        BONUS_PERCENT_PER_SHIP.put(HullSize.DESTROYER, 0.1f);
        BONUS_PERCENT_PER_SHIP.put(HullSize.CRUISER, 0.2f);
        BONUS_PERCENT_PER_SHIP.put(HullSize.CAPITAL_SHIP, 0.2f);
    }
    public static final float MAX_ACCURACY_BONUS = 0.5f;
    public static final float MAX_TURNRATE_BONUS = 0.35f;
    public static final String ID_FOR_BONUS = "SRD_DefenseGridID";

    //Handles our permanent, non-changing bonuses
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSize() == WeaponSize.SMALL && weapon.getType() != WeaponType.MISSILE) {
                weapon.setPD(true);
            }
        }
    }

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
            if (testShip.getVariant().getHullMods().contains("SRD_defense_grid") && !testShip.isHulk() && testShip.getOwner() == ship.getOwner()) {
                alliedBonus += BONUS_PERCENT_PER_SHIP.get(testShip.getHullSize());

                if (alliedBonus >= 1f) {
                    alliedBonus = 1f;
                    break;
                }
            }
        }

        //Applies our bonuses
        ship.getMutableStats().getWeaponTurnRateBonus().modifyMult(ID_FOR_BONUS, 1f + (alliedBonus * MAX_TURNRATE_BONUS));
        ship.getMutableStats().getAutofireAimAccuracy().modifyMult(ID_FOR_BONUS, 1f + (alliedBonus * MAX_ACCURACY_BONUS));

        //If we are the player ship, display a tooltip showing our current bonus
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip(ID_FOR_BONUS + "_TOOLTIP", "graphics/sylphon/icons/hullsys/ai_network.png", "Defense Grid", "Current bonus: " + (alliedBonus*100f) + "%", false);
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
        if (index == 0) return "" + (int)(MAX_TURNRATE_BONUS * 100f) + "%";
        if (index == 1) return "" + (int)(MAX_ACCURACY_BONUS * 100f) + "%";
        if (index == 2) return "" + (int)(BONUS_PERCENT_PER_SHIP.get(HullSize.FRIGATE) * 100f) + "%";
        if (index == 3) return "" + (int)(BONUS_PERCENT_PER_SHIP.get(HullSize.DESTROYER) * 100f) + "%";
        if (index == 4) return "" + (int)(BONUS_PERCENT_PER_SHIP.get(HullSize.CRUISER) * 100f) + "%";
        if (index == 5) return "" + (int)(BONUS_PERCENT_PER_SHIP.get(HullSize.CAPITAL_SHIP) * 100f) + "%";
        if (index == 6) return "small non-missile weapons";
        if (index == 7) return "PD";
        return null;
    }
}