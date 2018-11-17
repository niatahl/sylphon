//By Nicke535, a script that eats all ship flux and spits it back at you as hard flux once the system is turned off
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.HashMap;
import java.util.Map;

public class SRD_NullspaceShuntOLD extends BaseShipSystemScript {

    public static Map<ShipAPI, Float> fluxStorageMap = new HashMap<ShipAPI, Float>();

    private float fluxCurrentlyInNullspace = 0f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Store all flux the ship has
        fluxCurrentlyInNullspace += ship.getFluxTracker().getCurrFlux();
        ship.getFluxTracker().decreaseFlux(ship.getFluxTracker().getCurrFlux());

        fluxStorageMap.put(ship, fluxCurrentlyInNullspace);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Sends back all nullspace:d flux once the system is turned off
        if (fluxCurrentlyInNullspace > 0f) {
            ship.getFluxTracker().increaseFlux(fluxCurrentlyInNullspace, true);
            fluxCurrentlyInNullspace = 0f;
            fluxStorageMap.put(ship, fluxCurrentlyInNullspace);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("" + (int)fluxCurrentlyInNullspace + " flux stored in nullspace", true);
        }
        return null;
    }
}