//By Nicke535, a hullmod which buffs certain ship aspects but makes the ship unavailable for direct orders and control
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.mission.FleetSide;

import java.util.HashMap;
import java.util.Map;

public class SRD_CommandGridIsolation extends BaseHullMod {

    public static final float ECM_MULT = 0f;
    public static final String ID_FOR_BONUS = "SRD_CommandGridIsolationID";

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Has no effect with an officer or as the flagship
        if ((ship.getCaptain() == null || ship.getCaptain().isDefault()) && ship != Global.getCombatEngine().getPlayerShip()) {
            //If this ship is on the player's side, it should be set as an ally
            if (Global.getCombatEngine().getFleetManager(ship.getOwner()).equals(Global.getCombatEngine().getFleetManager(FleetSide.PLAYER))) {
                ship.setAlly(true);
            }

            //Applies our bonuses
            ship.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).modifyMult(ID_FOR_BONUS, 0f);
            ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyMult(ID_FOR_BONUS, ECM_MULT);
        } else {
            //Removes our bonuses; this should theoretically never be necessary, but better be on the safe side
            ship.getMutableStats().getDynamic().getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT).unmodify(ID_FOR_BONUS);
            ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify(ID_FOR_BONUS);
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
        if (index == 0) return "" + (int)((1f - ECM_MULT) * 100f) + "%";
        if (index == 1) return "cannot be controlled";
        if (index == 2) return "Has no effect if the ship is the flagship or has an officer";
        return null;
    }
}