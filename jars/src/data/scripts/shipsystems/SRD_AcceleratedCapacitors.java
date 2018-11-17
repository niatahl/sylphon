//By Nicke535, disables all non-ballistic weapons and
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.HashMap;
import java.util.Map;

public class SRD_AcceleratedCapacitors extends BaseShipSystemScript {

    public static float ROF_MULT = 3f;

    private Map<WeaponAPI, Integer> storedAmmo = new HashMap<WeaponAPI, Integer>();

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Improves ballistic RoF drastically
        stats.getBallisticRoFMult().modifyMult(id, ROF_MULT);

        for (WeaponAPI weap : ship.getAllWeapons()) {
            //Ignore ballistics (we should only have non-ballistic weapons equipped except our built-ins)
            if (weap.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                continue;
            }

            //If our "stored ammo" is -1, we have been disabled by this script already, and should be ignored
            if (storedAmmo.get(weap) != null && storedAmmo.get(weap) == -1) {continue;}

            //Disables all weapons with 4 different methods to *ensure* we disable them properly, unless some extremely weird script is on the weapons in question
            //The 2 other methods are done after this loop (flux cost and fire-rate)
            if (weap.getMaxAmmo() >= 1) {
                if (storedAmmo.get(weap) != null) {
                    storedAmmo.put(weap, storedAmmo.get(weap) + weap.getAmmo());
                } else {
                    storedAmmo.put(weap, weap.getAmmo());
                }
                weap.setAmmo(0);
            } else {
                weap.setMaxAmmo(1);
                weap.setAmmo(0);
                storedAmmo.put(weap, -1);
            }

            if (weap.getCooldownRemaining() < 0.2f) {
                weap.setRemainingCooldownTo(0.2f);
            }
        }

        //Modifies RoF and flux cost for weapons so they cannot fire even if the above code doesn't work
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 999999999999999999f);
        stats.getEnergyRoFMult().modifyMult(id, 0.000000000000000001f);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Resets all the stats for the weapons
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        for (WeaponAPI weap : ship.getAllWeapons()) {
            if (storedAmmo.get(weap) == null) {
                continue;
            }

            if (storedAmmo.get(weap) == -1) {
                weap.setAmmo(1);
                weap.setMaxAmmo(0);
            } else {
                weap.setAmmo(storedAmmo.get(weap));
            }
        }
        storedAmmo.clear();
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("all power to main weapons", false);
        }
        return null;
    }
}